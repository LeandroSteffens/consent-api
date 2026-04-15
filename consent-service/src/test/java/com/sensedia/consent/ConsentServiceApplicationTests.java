package com.sensedia.consent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensedia.consent.domain.Consent;
import com.sensedia.consent.domain.ConsentStatus;
import com.sensedia.consent.dto.ConsentCreateRequest;
import com.sensedia.consent.repository.ConsentHistoryRepository;
import com.sensedia.consent.repository.ConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class ConsentServiceApplicationTests {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConsentRepository repository;

    @Autowired
    private ConsentHistoryRepository historyRepository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        historyRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Fluxo Completo: Criar consentimento e testar idempotência com MongoDB Real")
    void shouldCreateConsentAndTestIdempotencyInIntegration() throws Exception {
        String jsonPayload = buildValidConsentJsonPayload();
        String idempotencyKey = "chave-integracao-123";

        mockMvc.perform(post("/consents")
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.cpf").value("123.456.789-00"));

        assertEquals(1, repository.count());

        mockMvc.perform(post("/consents")
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        assertEquals(1, repository.count());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Fluxo Completo: Revogar consentimento e consultar histórico (Auditoria)")
    void shouldRevokeAndConsultHistory() throws Exception {
        Consent consent = createAndSaveActiveConsent();

        mockMvc.perform(delete("/consents/" + consent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));

        assertEquals(1, historyRepository.count());

        mockMvc.perform(get("/consents/" + consent.getId() + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("REVOKE"))
                .andExpect(jsonPath("$[0].consentSnapshot.status").value("REVOKED"));
    }

    private String buildValidConsentJsonPayload() throws Exception {
        ConsentCreateRequest request = new ConsentCreateRequest();
        request.setCpf("123.456.789-00");
        request.setExpirationDateTime(LocalDateTime.now().plusYears(1));
        request.setAdditionalInfo("Teste de Integração Sensedia");

        return objectMapper.writeValueAsString(request);
    }

    private Consent createAndSaveActiveConsent() {
        Consent consent = new Consent();
        consent.setId(UUID.randomUUID());
        consent.setCpf("111.222.333-44");
        consent.setStatus(ConsentStatus.ACTIVE);
        consent.setCreationDateTime(LocalDateTime.now());

        return repository.save(consent);
    }
}
