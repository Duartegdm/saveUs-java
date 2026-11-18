package br.com.fiap.saveus.refugio.guia.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Resposta para a tela Guia do Calor. resumo e dicas vêm do LLM; refugio vem do banco (null sem reserva futura);
 * sinaisDeAlerta e emergencia são fixos no código: informação de saúde não depende do modelo.
 */
@Schema(name = "GuiaDoCalor", description = "Guia de proteção contra o calor, desenhado pela tela Guia do Calor do app")
public record GuiaResponse(
        @Schema(description = "Até 2 frases, gerado pelo LLM",
                example = "Amanhã o calor continua forte. Sua vaga na Biblioteca Municipal da Penha está garantida das 9h às 18h.")
        String resumo,
        @Schema(description = "De 3 a 5 dicas práticas, geradas pelo LLM") List<GuiaGerado.Dica> dicas,
        @Schema(description = "Refúgio da próxima reserva ATIVA, vindo do banco; null sem reserva futura", nullable = true)
        @JsonInclude(JsonInclude.Include.ALWAYS) RefugioDoGuia refugio,
        @Schema(description = "Fixos no código, nunca gerados pelo modelo",
                example = "[\"Tontura ou confusão\", \"Dor de cabeça forte\", \"Pele quente e seca\", \"Náusea ou vômito\"]")
        List<String> sinaisDeAlerta,
        @Schema(description = "Fixo no código", example = "Se sentir algum desses sinais, ligue 192 (SAMU).") String emergencia,
        @Schema(description = "false quando o LLM falhou e o guia padrão foi usado") boolean geradoPorIa) {

    @Schema(description = "Refúgio reservado pelo idoso")
    public record RefugioDoGuia(
            @Schema(example = "1") Long id,
            @Schema(example = "Biblioteca Municipal da Penha") String nome,
            @Schema(example = "Rua Padre Benedito de Camargo, 400 — Penha") String endereco,
            @Schema(description = "Data da reserva", example = "2026-11-27") LocalDate data,
            @Schema(type = "string", example = "09:00") @JsonFormat(pattern = "HH:mm") LocalTime abre,
            @Schema(type = "string", example = "18:00") @JsonFormat(pattern = "HH:mm") LocalTime fecha,
            Boolean acessivel) {
    }
}
