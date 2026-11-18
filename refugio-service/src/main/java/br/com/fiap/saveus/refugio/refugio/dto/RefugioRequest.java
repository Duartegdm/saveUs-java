package br.com.fiap.saveus.refugio.refugio.dto;

import br.com.fiap.saveus.refugio.refugio.TipoRefugio;
import br.com.fiap.saveus.refugio.refugio.validacao.CepValido;
import br.com.fiap.saveus.refugio.refugio.validacao.FechaDepoisDeAbre;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/** Corpo de POST e PUT /api/v1/refugios. Todos os campos são obrigatórios: o PUT substitui o refúgio inteiro. */
@Schema(name = "RefugioRequest", description = "Corpo de POST e PUT. Todos os campos são obrigatórios; `fecha` deve ser depois de `abre`.")
@FechaDepoisDeAbre
public record RefugioRequest(
        @Schema(example = "Biblioteca Municipal da Penha")
        @NotBlank(message = "Informe o nome do refúgio.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String nome,

        @Schema(example = "BIBLIOTECA")
        @NotNull(message = "Informe o tipo do refúgio.")
        TipoRefugio tipo,

        @Schema(description = "8 dígitos, com ou sem hífen; gravado como 00000-000", example = "03601-000")
        @NotBlank(message = "Informe o CEP.")
        @CepValido
        String cep,

        @Schema(example = "Rua Exemplo, 100")
        @NotBlank(message = "Informe o endereço.")
        @Size(max = 200, message = "O endereço deve ter no máximo 200 caracteres.")
        String endereco,

        @Schema(example = "Penha")
        @NotBlank(message = "Informe o bairro.")
        @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres.")
        String bairro,

        @Schema(description = "De 1 a 10.000 pessoas por dia", example = "20")
        @NotNull(message = "Informe a capacidade.")
        @Positive(message = "A capacidade deve ser maior que zero.")
        @Max(value = 10_000, message = "A capacidade deve ser de no máximo 10.000 pessoas.")
        Integer capacidade,

        @Schema(type = "string", pattern = "HH:mm", example = "08:00")
        @NotNull(message = "Informe o horário de abertura.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime abre,

        @Schema(type = "string", pattern = "HH:mm", example = "20:00")
        @NotNull(message = "Informe o horário de fechamento.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime fecha,

        @Schema(example = "true")
        @NotNull(message = "Informe se o refúgio é acessível.")
        Boolean acessivel,

        @Schema(description = "false pausa o refúgio (0 vagas, sem novas reservas)", example = "true")
        @NotNull(message = "Informe se o refúgio está ativo.")
        Boolean ativo) {
}
