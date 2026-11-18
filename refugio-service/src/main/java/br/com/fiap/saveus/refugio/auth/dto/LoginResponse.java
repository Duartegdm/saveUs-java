package br.com.fiap.saveus.refugio.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Resposta do login")
public record LoginResponse(
        @Schema(description = "JWT HS256 para o header Authorization: Bearer <token>; vale no Java e no .NET",
                example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZSI6IkdFU1RPUiJ9.assinatura")
        String token,
        @Schema(description = "Expiração do token (120 min), com o offset de São Paulo", example = "2026-11-17T17:00:00-03:00")
        OffsetDateTime expiraEm,
        UsuarioResumo usuario) {
}
