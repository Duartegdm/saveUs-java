package br.com.fiap.saveus.refugio.guia.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/** Corpo opcional de POST /api/v1/ia/guia-do-calor. Sem data, vale hoje em São Paulo. */
@Schema(description = "Corpo opcional do Guia do Calor")
public record GuiaRequest(
        @Schema(description = "A partir de qual dia buscar a reserva (yyyy-MM-dd). Padrão: hoje em São Paulo", example = "2026-11-27")
        LocalDate data) {
}
