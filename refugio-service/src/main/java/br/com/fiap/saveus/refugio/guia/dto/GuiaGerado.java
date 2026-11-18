package br.com.fiap.saveus.refugio.guia.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Resposta do ia-service: a parte do guia que vem do LLM. */
public record GuiaGerado(String resumo, List<Dica> dicas) {

    @Schema(name = "DicaDoGuia", description = "Dica prática gerada pelo LLM")
    public record Dica(
            @Schema(description = "Até 4 palavras", example = "Saia cedo") String titulo,
            @Schema(description = "Até 20 palavras", example = "Vá até o refúgio antes das 11h, quando o sol ainda está mais fraco.")
            String descricao) {
    }
}
