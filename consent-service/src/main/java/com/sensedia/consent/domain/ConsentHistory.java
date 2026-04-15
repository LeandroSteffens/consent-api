package com.sensedia.consent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "consent_history")
public class ConsentHistory {

    @Id
    private UUID id;

    private UUID consentId;

    private String action;

    private Consent consentSnapshot;

    private LocalDateTime timestamp;
}
