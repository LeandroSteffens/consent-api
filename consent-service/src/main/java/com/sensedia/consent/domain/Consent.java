package com.sensedia.consent.domain;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "consents")
public class Consent {

    @Id
    private UUID id;

    private String cpf;

    private ConsentStatus status;

    @CreatedDate
    private LocalDateTime creationDateTime;

    private LocalDateTime expirationDateTime;

    private String additionalInfo;

    @Indexed(unique = true)
    @Size(max = 255, message = "A chave de idempotência não pode ultrapassar 255 caracteres")
    private String idempotencyKey;

}
