package com.sensedia.consent.controller;

import com.sensedia.consent.dto.ConsentCreateRequest;
import com.sensedia.consent.dto.ConsentHistoryResponse;
import com.sensedia.consent.dto.ConsentResponse;
import com.sensedia.consent.dto.ConsentUpdateRequest;
import com.sensedia.consent.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/consents")
@RequiredArgsConstructor
@Tag(name = "Consentimentos", description = "API para gestão de consentimentos de usuários (Open Insurance)")
public class ConsentController {

    private final ConsentService service;

    @Operation(summary = "Criar um novo consentimento", description = "Endpoint com suporte a idempotência")
    @PostMapping
    public ResponseEntity<ConsentResponse> createConsent(
            @Parameter(description = "Chave de Idempotência", required = true)
            @NotBlank(message = "A chave de idempotência não pode estar em branco")
            @Size(max = 255, message = "A chave de idempotência não pode ultrapassar 255 caracteres")
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ConsentCreateRequest request) {

        ConsentService.CreationResult result = service.createConsent(idempotencyKey, request);

        if (result.isCreated()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result.response());
        } else {
            return ResponseEntity.ok(result.response());
        }
    }

    @Operation(summary = "Listar os consentimentos", description = "Retorna uma lista paginada")
    @GetMapping
    public ResponseEntity<Page<ConsentResponse>> getAllConsents(
            @PageableDefault(size = 10) Pageable pageable) {

        Page<ConsentResponse> responses = service.findAll(pageable);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Buscar consentimento por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ConsentResponse> getConsentById(@PathVariable UUID id) {
        ConsentResponse response = service.findById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Atualizar informações do consentimento",
            description = "Apenas usuários com role ADMIN podem atualizar consentimentos")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ConsentResponse> updateConsent(
            @PathVariable UUID id,
            @Valid @RequestBody ConsentUpdateRequest request) {

        ConsentResponse response = service.update(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Revogar um consentimento", description = "Altera o status do consentimento para REVOKED e retorna o objeto atualizado")
    @DeleteMapping("/{id}")
    public ResponseEntity<ConsentResponse> revokeConsent(@PathVariable UUID id) {
        ConsentResponse response = service.revoke(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obter histórico de alterações", description = "Retorna a lista de snapshots (auditoria) do consentimento ordenados pelo mais recente")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<ConsentHistoryResponse>> getConsentHistory(@PathVariable UUID id) {
        List<ConsentHistoryResponse> history = service.getHistory(id);
        return ResponseEntity.ok(history);
    }
}
