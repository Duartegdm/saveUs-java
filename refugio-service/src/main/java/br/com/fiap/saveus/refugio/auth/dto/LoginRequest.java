package br.com.fiap.saveus.refugio.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "gestor@refugio.com") @NotBlank(message = "Informe o e-mail.") String email,
        @Schema(example = "Senha@123") @NotBlank(message = "Informe a senha.") String senha) {
}
