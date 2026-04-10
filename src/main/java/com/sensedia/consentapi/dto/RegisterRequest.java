package com.sensedia.consentapi.dto;

import com.sensedia.consentapi.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "O username é obrigatório")
    @Size(min = 3, max = 50, message = "O username deve ter entre 3 e 50 caracteres")
    private String username;

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres")
    private String password;

    private UserRole role;
}
