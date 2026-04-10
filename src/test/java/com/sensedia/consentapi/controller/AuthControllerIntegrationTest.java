package com.sensedia.consentapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensedia.consentapi.domain.User;
import com.sensedia.consentapi.domain.UserRole;
import com.sensedia.consentapi.dto.LoginRequest;
import com.sensedia.consentapi.dto.RegisterRequest;
import com.sensedia.consentapi.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Deve registrar um novo usuário e retornar 201 com tokens")
        void shouldRegisterSuccessfully() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("luis")
                    .password("senha123")
                    .role(UserRole.ROLE_USER)
                    .build();

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(1800000));
        }

        @Test
        @DisplayName("Deve retornar 400 quando username já existir")
        void shouldReturn400WhenUsernameExists() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("duplicado")
                    .password("senha123")
                    .build();

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("já está em uso")));
        }

        @Test
        @DisplayName("Deve retornar 400 quando request inválida (username vazio)")
        void shouldReturn400WhenUsernameIsBlank() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("")
                    .password("senha123")
                    .build();

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class LoginTests {

        @Test
        @DisplayName("Deve fazer login com sucesso e retornar 200 com tokens")
        void shouldLoginSuccessfully() throws Exception {
            registerUser("luis", "senha123", UserRole.ROLE_USER);

            LoginRequest loginRequest = LoginRequest.builder()
                    .username("luis")
                    .password("senha123")
                    .build();

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("Deve retornar 401 com senha incorreta")
        void shouldReturn401WithWrongPassword() throws Exception {
            registerUser("luis", "senha123", UserRole.ROLE_USER);

            LoginRequest loginRequest = LoginRequest.builder()
                    .username("luis")
                    .password("senha-errada")
                    .build();

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Endpoints Protegidos - /consents")
    class ProtectedEndpointTests {

        @Test
        @DisplayName("Deve retornar 401 ao acessar /consents sem token")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get("/consents"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve retornar 200 ao acessar /consents com token válido")
        void shouldReturn200WithValidToken() throws Exception {
            String accessToken = registerAndGetToken("luis", "senha123", UserRole.ROLE_USER);

            mockMvc.perform(get("/consents")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve retornar 401 com token inválido (adulterado)")
        void shouldReturn401WithInvalidToken() throws Exception {
            mockMvc.perform(get("/consents")
                            .header("Authorization", "Bearer token.invalido.aqui"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Autorização por Role - PUT /consents/{id}")
    class RoleAuthorizationTests {

        @Test
        @DisplayName("USER deve receber 403 Forbidden ao tentar PUT /consents/{id}")
        void userShouldBeForbiddenFromPut() throws Exception {
            String userToken = registerAndGetToken("user_normal", "senha123", UserRole.ROLE_USER);
            UUID fakeId = UUID.randomUUID();

            mockMvc.perform(put("/consents/" + fakeId)
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"additionalInfo\":\"test\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN deve conseguir acessar PUT /consents/{id}")
        void adminShouldBeAllowedToPut() throws Exception {
            String adminToken = registerAndGetToken("admin_user", "senha123", UserRole.ROLE_ADMIN);
            UUID fakeId = UUID.randomUUID();

            mockMvc.perform(put("/consents/" + fakeId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"additionalInfo\":\"test\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("USER deve conseguir acessar DELETE /consents/{id}")
        void userShouldBeAllowedToDelete() throws Exception {
            String userToken = registerAndGetToken("user_delete", "senha123", UserRole.ROLE_USER);
            UUID fakeId = UUID.randomUUID();

            mockMvc.perform(delete("/consents/" + fakeId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Deve renovar access token com refresh token válido")
        void shouldRefreshToken() throws Exception {
            RegisterRequest register = RegisterRequest.builder()
                    .username("luis_refresh")
                    .password("senha123")
                    .role(UserRole.ROLE_USER)
                    .build();

            MvcResult registerResult = mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(register)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String refreshToken = objectMapper.readTree(
                    registerResult.getResponse().getContentAsString()
            ).get("refreshToken").asText();

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").value(refreshToken));
        }
    }

    private void registerUser(String username, String password, UserRole role) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .password(passwordEncoder.encode(password))
                .roles(Set.of(role))
                .build();
        userRepository.save(user);
    }

    private String registerAndGetToken(String username, String password, UserRole role) throws Exception {
        registerUser(username, password, role);

        LoginRequest loginRequest = LoginRequest.builder()
                .username(username)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).get("accessToken").asText();
    }
}
