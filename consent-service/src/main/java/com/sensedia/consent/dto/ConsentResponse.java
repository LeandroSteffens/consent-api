package com.sensedia.consent.dto;

import com.sensedia.consent.domain.ConsentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponse {
    private UUID id;
    private String cpf;
    private ConsentStatus status;
    private LocalDateTime creationDateTime;
    private LocalDateTime expirationDateTime;
    private String additionalInfo;
}
