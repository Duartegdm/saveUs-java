package br.com.fiap.saveus.ia.guia.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** Dados reais do idoso e da reserva, montados pelo refugio-service a partir do banco. */
@Schema(description = "Contexto montado pelo refugio-service a partir do banco (Usuario, Reserva e Refugio)")
public record ContextoGuia(
        @Schema(description = "Primeiro nome do idoso", example = "Maria")
        @NotBlank(message = "Informe o nome do idoso.") @Size(max = 150) String nomeIdoso,
        @Schema(description = "Dia do guia (data da reserva ou a data pedida)", example = "2026-11-27")
        @NotNull(message = "Informe a data do guia.") LocalDate data,
        @Schema(example = "Penha")
        @Size(max = 100) String bairro,
        @Schema(description = "Refúgio da próxima reserva; null quando o idoso não tem reserva", nullable = true)
        @Valid RefugioContexto refugio) {

    public record RefugioContexto(
            @Schema(example = "Biblioteca Municipal da Penha")
            @NotBlank(message = "Informe o nome do refúgio.") @Size(max = 150) String nome,
            @Schema(example = "Rua Padre Benedito de Camargo, 400 — Penha")
            @NotBlank(message = "Informe o endereço do refúgio.") @Size(max = 310) String endereco,
            @Schema(type = "string", example = "09:00")
            @NotNull(message = "Informe o horário de abertura.") @JsonFormat(pattern = "HH:mm") LocalTime abre,
            @Schema(type = "string", example = "18:00")
            @NotNull(message = "Informe o horário de fechamento.") @JsonFormat(pattern = "HH:mm") LocalTime fecha,
            @NotNull(message = "Informe se o refúgio é acessível.") Boolean acessivel) {
    }
}
