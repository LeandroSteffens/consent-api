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
public class ConsentUpdateRequest {

    private LocalDateTime expirationDateTime;

    private String additionalInfo;
}
