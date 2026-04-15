# API de Consentimentos - Open Insurance Brasil

Este projeto é uma API REST para gestão de consentimentos de usuários no ecossistema de Open Insurance. A aplicação segue uma arquitetura de microsserviços com Spring Cloud.

## Tecnologias Utilizadas

- Java 21
- Spring Boot 3.3.4
- Spring Cloud 2023.0.4 (Gateway, Eureka)
- Spring Security + JWT (JJWT 0.12.6)
- MongoDB
- Redis
- Lombok
- MapStruct
- OpenAPI / Swagger
- JUnit 5 & Mockito
- Testcontainers
- Docker & Docker Compose
- SLF4J & Logback
- Spring Scheduling

## Arquitetura

O projeto é composto por 5 módulos independentes orquestrados via Docker Compose:

- **discovery-server** (porta 8761) — Service Discovery (Eureka)
- **api-gateway** (porta 8080) — Ponto de entrada e roteamento
- **auth-service** (porta 8081) — Autenticação e autorização (JWT)
- **consent-service** (porta 8082) — CRUD de consentimentos
- **consent-scheduler** (porta 8083) — Job de expiração automática

Cada serviço possui seu próprio banco de dados (`auth_db`, `consent_db`). A validação de tokens entre serviços é feita via secret JWT compartilhado, e a blacklist de tokens é gerenciada pelo Redis.

## Funcionalidades e Regras de Negócio

- Idempotência: O endpoint de criação (POST /consents) exige o cabeçalho X-Idempotency-Key. Requisições subsequentes com a mesma chave não geram duplicidade no banco de dados e retornam status 200 OK com o recurso originalmente criado.
- Exclusão Lógica: A revogação de um consentimento (DELETE /consents/{id}) altera o status do registro para REVOKED, mantendo o histórico na base de dados e retornando o objeto atualizado.
- Paginação: A listagem de consentimentos (GET /consents) implementa paginação nativa através da interface Pageable do Spring Data.
- Tratamento Global de Exceções: Utilização de @RestControllerAdvice para interceptar erros de validação (Bean Validation) e recursos não encontrados, padronizando o formato da resposta de erro.
- Proteção de Dados Sensíveis: O endpoint de atualização (PUT /consents/{id}) utiliza DTOs e MapStruct configurados para ignorar campos imutáveis, como ID, CPF e Data de Criação.
- Trilha de Auditoria: Implementação de um fluxo de histórico que salva automaticamente uma "foto" (snapshot) do consentimento em uma coleção separada (`consent_history`) sempre que ocorre uma mutação (CREATE, UPDATE ou REVOKE). Esse histórico de vida do dado pode ser consultado via endpoint específico (GET /consents/{id}/history).
- Rotina de Expiração Automática: O sistema possui um job rodando em background (via `@Scheduled` com Cron) que identifica e altera o status de consentimentos vencidos para EXPIRED, registrando a ação na trilha de auditoria.
- Autenticação JWT: Sistema completo de registro, login, logout e refresh de tokens. O logout invalida tokens via blacklist no Redis. Endpoints protegidos por roles (USER/ADMIN).
- Conformidade de Fuso Horário: A aplicação é forçada a inicializar em UTC (Zero offset) via `@PostConstruct`, garantindo que todas as datas e horas trafegadas sigam o padrão ISO 8601.
- Sistema de Logs: Implementação de logs separados por níveis de severidade (INFO, WARN, ERROR) no `GlobalExceptionHandler` e no `ConsentExpirationScheduler` para monitoramento de rotinas, tratamento de exceções e auditoria.

## Pré-requisitos

Para executar este projeto, é necessário ter instalado:
- Java 21
- Docker e Docker Compose

## Como Executar a Aplicação

**Passo 1:** Na raiz do projeto, compile todos os módulos:
> ./mvnw clean package -DskipTests

**Passo 2:** Suba os contêineres:
> docker-compose up --build -d

**Passo 3:** Acesse a documentação interativa da API (Swagger UI):
- Auth Service: http://localhost:8081/swagger-ui.html
- Consent Service: http://localhost:8082/swagger-ui.html
- Eureka Dashboard: http://localhost:8761

As requisições devem ser feitas através do API Gateway na porta 8080 (ex: `http://localhost:8080/auth/login`, `http://localhost:8080/consents`).

## Como Executar os Testes

O projeto possui uma suíte de testes unitários e de integração. Os testes de integração utilizam a biblioteca Testcontainers para provisionar contêineres efêmeros do MongoDB, garantindo isolamento completo da base de dados.

Para rodar a suíte completa de testes, execute na raiz do projeto:
> ./mvnw clean test

## Solução de Problemas Comuns

1. Erro: "Port 8080 was already in use" ou portas conflitantes
   Causa: Outro serviço na máquina já está ocupando a porta.
   Solução: Pare outros containers ou altere as portas nos arquivos `application.yml` de cada serviço.

2. Token rejeitado no consent-service
   Causa: O JWT_SECRET está diferente entre auth-service e consent-service.
   Solução: Verifique se a variável de ambiente `JWT_SECRET` é a mesma para ambos os serviços no `docker-compose.yml`.

3. Erro de compilação: "cannot find symbol" (Getters/Setters não encontrados)
   Causa: A IDE ou o compilador não processou as anotações do Lombok corretamente.
   Solução: Execute `./mvnw clean install`. No IntelliJ IDEA, ative "Enable Annotation Processing".

4. Eureka não encontra os serviços
   Causa: O discovery-server precisa estar rodando antes dos demais.
   Solução: O `docker-compose.yml` já trata a ordem de inicialização com `depends_on` e healthcheck.