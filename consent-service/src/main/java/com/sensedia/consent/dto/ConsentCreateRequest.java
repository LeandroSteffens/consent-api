package com.sensedia.consent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentCreateRequest {

    @NotBlank(message = "O CPF é obrigatório")
    private String cpf;

    private LocalDateTime expirationDateTime;

    private String additionalInfo;
}
