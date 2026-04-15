package com.sensedia.consent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentHistoryResponse {
    private String action;
    private LocalDateTime timestamp;
    private ConsentResponse consentSnapshot;
}
