package br.com.fiap.saveus.refugio.auth.dto;

import br.com.fiap.saveus.refugio.auth.SenhaForte;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O cadastro cria sempre um IDOSO; gestores vêm do script do banco. */
public record RegistrarRequest(
        @Schema(example = "Ana Lima")
        @NotBlank(message = "Informe o nome.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String nome,

        @Schema(example = "ana.lima@email.com")
        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 200, message = "O e-mail deve ter no máximo 200 caracteres.")
        String email,

        @Schema(description = "8 a 100 caracteres, com letra maiúscula, minúscula e número", example = "Senha@123")
        @NotBlank(message = "Informe a senha.")
        @SenhaForte
        String senha) {
}
