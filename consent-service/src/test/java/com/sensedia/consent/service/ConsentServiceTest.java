package com.sensedia.consent.service;

import com.sensedia.consent.domain.Consent;
import com.sensedia.consent.domain.ConsentHistory;
import com.sensedia.consent.domain.ConsentStatus;
import com.sensedia.consent.dto.ConsentCreateRequest;
import com.sensedia.consent.dto.ConsentResponse;
import com.sensedia.consent.dto.ConsentUpdateRequest;
import com.sensedia.consent.exception.ResourceNotFoundException;
import com.sensedia.consent.mapper.ConsentMapper;
import com.sensedia.consent.repository.ConsentHistoryRepository;
import com.sensedia.consent.repository.ConsentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock
    private ConsentRepository repository;

    @Mock
    private ConsentMapper mapper;

    @Mock
    private ConsentHistoryRepository historyRepository;

    @InjectMocks
    private ConsentService service;

    @Test
    @DisplayName("Deve retornar isCreated=false quando a chave de idempotência já existir (Regra 200 OK)")
    void shouldReturnExistingWhenIdempotencyKeyExists() {
        String key = "chave-repetida-123";
        Consent existingConsent = buildConsent(UUID.randomUUID(), ConsentStatus.ACTIVE);

        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingConsent));
        when(mapper.toResponse(any())).thenReturn(buildConsentResponse(ConsentStatus.ACTIVE));

        ConsentService.CreationResult result = service.createConsent(key, buildCreateRequest());

        assertFalse(result.isCreated());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar isCreated=true e salvar no banco quando a chave for nova (Regra 201 Created)")
    void shouldCreateNewWhenIdempotencyKeyDoesNotExist() {
        String key = "chave-nova-456";
        Consent consent = buildConsent(null, ConsentStatus.ACTIVE);

        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(mapper.toEntity(any())).thenReturn(consent);
        when(repository.save(any())).thenReturn(consent);

        ConsentService.CreationResult result = service.createConsent(key, buildCreateRequest());

        assertTrue(result.isCreated());
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar por um ID que não existe")
    void shouldThrowExceptionWhenIdDoesNotExist() {
        UUID idFake = UUID.randomUUID();
        when(repository.findById(idFake)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(idFake));
    }

    @Test
    @DisplayName("Deve alterar o status para REVOKED ao chamar o método revoke e retornar a resposta")
    void shouldRevokeConsentSuccessfully() {
        UUID id = UUID.randomUUID();
        Consent consent = buildConsent(id, ConsentStatus.ACTIVE);
        ConsentResponse responseMock = buildConsentResponse(ConsentStatus.REVOKED);

        when(repository.findById(id)).thenReturn(Optional.of(consent));
        when(repository.save(any(Consent.class))).thenReturn(consent);
        when(mapper.toResponse(any(Consent.class))).thenReturn(responseMock);

        ConsentResponse result = service.revoke(id);

        assertEquals(ConsentStatus.REVOKED, consent.getStatus());
        assertEquals(ConsentStatus.REVOKED, result.getStatus());
        verify(repository, times(1)).save(consent);
    }

    @Test
    @DisplayName("Deve retornar a lista de histórico de um consentimento existente")
    void shouldReturnHistorySuccessfully() {
        UUID id = UUID.randomUUID();
        Consent consent = buildConsent(id, ConsentStatus.ACTIVE);
        ConsentHistory history = buildConsentHistory(consent, "CREATE");

        when(repository.findById(id)).thenReturn(Optional.of(consent));
        when(historyRepository.findByConsentIdOrderByTimestampDesc(id)).thenReturn(List.of(history));
        when(mapper.toResponse(any())).thenReturn(buildConsentResponse(ConsentStatus.ACTIVE));

        var result = service.getHistory(id);

        assertFalse(result.isEmpty());
        assertEquals("CREATE", result.get(0).getAction());
        verify(historyRepository, times(1)).findByConsentIdOrderByTimestampDesc(id);
    }

    @Test
    @DisplayName("Deve lançar exceção quando a chave de idempotência for nula ou vazia")
    void shouldThrowExceptionWhenIdempotencyKeyIsEmpty() {
        ConsentCreateRequest request = buildCreateRequest();

        assertThrows(IllegalArgumentException.class, () -> service.createConsent(null, request));
        assertThrows(IllegalArgumentException.class, () -> service.createConsent("", request));
        assertThrows(IllegalArgumentException.class, () -> service.createConsent("   ", request));

        verify(repository, never()).findByIdempotencyKey(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar atualizar consentimento REVOKED")
    void shouldThrowWhenUpdatingRevokedConsent() {
        UUID id = UUID.randomUUID();
        Consent consent = buildConsent(id, ConsentStatus.REVOKED);
        when(repository.findById(id)).thenReturn(Optional.of(consent));

        assertThrows(IllegalArgumentException.class, 
            () -> service.update(id, new ConsentUpdateRequest()));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar atualizar consentimento EXPIRED")
    void shouldThrowWhenUpdatingExpiredConsent() {
        UUID id = UUID.randomUUID();
        Consent consent = buildConsent(id, ConsentStatus.EXPIRED);
        when(repository.findById(id)).thenReturn(Optional.of(consent));

        assertThrows(IllegalArgumentException.class,
            () -> service.update(id, new ConsentUpdateRequest()));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar revogar consentimento já REVOKED")
    void shouldThrowWhenRevokingAlreadyRevokedConsent() {
        UUID id = UUID.randomUUID();
        Consent consent = buildConsent(id, ConsentStatus.REVOKED);
        when(repository.findById(id)).thenReturn(Optional.of(consent));

        assertThrows(IllegalArgumentException.class,
            () -> service.revoke(id));
        verify(repository, never()).save(any());
    }


    private Consent buildConsent(UUID id, ConsentStatus status) {
        return Consent.builder()
                .id(id)
                .cpf("123.456.789-00")
                .status(status)
                .creationDateTime(LocalDateTime.now())
                .expirationDateTime(LocalDateTime.now().plusYears(1))
                .build();
    }

    private ConsentCreateRequest buildCreateRequest() {
        ConsentCreateRequest request = new ConsentCreateRequest();
        request.setCpf("123.456.789-00");
        request.setExpirationDateTime(LocalDateTime.now().plusYears(1));
        return request;
    }

    private ConsentResponse buildConsentResponse(ConsentStatus status) {
        return ConsentResponse.builder()
                .cpf("123.456.789-00")
                .status(status)
                .build();
    }

    private ConsentHistory buildConsentHistory(Consent snapshot, String action) {
        return ConsentHistory.builder()
                .id(UUID.randomUUID())
                .consentId(snapshot.getId())
                .action(action)
                .consentSnapshot(snapshot)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
