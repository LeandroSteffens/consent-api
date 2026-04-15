package com.sensedia.consent.service;

import com.sensedia.consent.domain.Consent;
import com.sensedia.consent.domain.ConsentHistory;
import com.sensedia.consent.domain.ConsentStatus;
import com.sensedia.consent.dto.ConsentCreateRequest;
import com.sensedia.consent.dto.ConsentHistoryResponse;
import com.sensedia.consent.dto.ConsentResponse;
import com.sensedia.consent.dto.ConsentUpdateRequest;
import com.sensedia.consent.exception.ResourceNotFoundException;
import com.sensedia.consent.mapper.ConsentMapper;
import com.sensedia.consent.repository.ConsentHistoryRepository;
import com.sensedia.consent.repository.ConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentRepository repository;
    private final ConsentMapper mapper;
    private final ConsentHistoryRepository historyRepository;

    public record CreationResult(ConsentResponse response, boolean isCreated) {}

    public CreationResult createConsent(String idempotencyKey, ConsentCreateRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("A chave de idempotência não pode estar vazia.");
        }

        Optional<Consent> existingConsent = repository.findByIdempotencyKey(idempotencyKey);
        if (existingConsent.isPresent()) {
            return new CreationResult(mapper.toResponse(existingConsent.get()), false);
        }

        Consent consent = mapper.toEntity(request);
        consent.setStatus(ConsentStatus.ACTIVE);
        if (consent.getId() == null) consent.setId(UUID.randomUUID());
        consent.setIdempotencyKey(idempotencyKey);

        consent.setCreationDateTime(LocalDateTime.now());

        Consent savedConsent = repository.save(consent);
        registerHistory(savedConsent, "CREATE");
        return new CreationResult(mapper.toResponse(savedConsent), true);
    }

    public Page<ConsentResponse> findAll(Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1) {
            throw new IllegalArgumentException("Os parâmetros de paginação (page e size) devem ser positivos.");
        }
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    public ConsentResponse findById(UUID id) {
        return mapper.toResponse(getConsentByIdOrThrow(id));
    }

    public ConsentResponse update(UUID id, ConsentUpdateRequest request) {
        Consent consent = getConsentByIdOrThrow(id);

        if (!ConsentStatus.ACTIVE.equals(consent.getStatus())) {
            throw new IllegalArgumentException(
                "Só é possível alterar consentimentos com status ACTIVE. Status atual: " + consent.getStatus());
        }

        mapper.updateEntityFromRequest(request, consent);

        Consent updatedConsent = repository.save(consent);
        registerHistory(updatedConsent, "UPDATE");

        return mapper.toResponse(updatedConsent);
    }

    public ConsentResponse revoke(UUID id) {
        Consent consent = getConsentByIdOrThrow(id);

        if (!ConsentStatus.ACTIVE.equals(consent.getStatus())) {
            throw new IllegalArgumentException(
                "Só é possível revogar consentimentos com status ACTIVE. Status atual: " + consent.getStatus());
        }
        
        consent.setStatus(ConsentStatus.REVOKED);

        Consent savedConsent = repository.save(consent);
        registerHistory(savedConsent, "REVOKE");

        return mapper.toResponse(savedConsent);
    }

    public java.util.List<ConsentHistoryResponse> getHistory(UUID consentId) {
        getConsentByIdOrThrow(consentId);

        return historyRepository.findByConsentIdOrderByTimestampDesc(consentId)
                .stream()
                .map(history -> ConsentHistoryResponse.builder()
                        .action(history.getAction())
                        .timestamp(history.getTimestamp())
                        .consentSnapshot(mapper.toResponse(history.getConsentSnapshot()))
                        .build())
                .toList();
    }

    private Consent getConsentByIdOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consentimento não encontrado com o ID: " + id));
    }

    private void registerHistory(Consent consent, String action) {
        ConsentHistory history = ConsentHistory.builder()
                .id(UUID.randomUUID())
                .consentId(consent.getId())
                .action(action)
                .consentSnapshot(consent)
                .timestamp(LocalDateTime.now())
                .build();

        historyRepository.save(history);
    }
}
