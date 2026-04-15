package com.sensedia.scheduler.domain;

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
    private String idempotencyKey;
}
