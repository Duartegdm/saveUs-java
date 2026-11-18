package br.com.fiap.saveus.ia.guia.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Saída estruturada do LLM. O Spring AI gera o JSON Schema desta classe, pede o JSON ao modelo
 * e converte a resposta sozinho (chatClient...call().entity(GuiaGerado.class)).
 */
@Schema(description = "Saída estruturada do LLM, convertida pelo Spring AI")
public record GuiaGerado(
        @JsonPropertyDescription("Resumo de até 2 frases curtas sobre o dia e o refúgio")
        @Schema(example = "Amanhã o calor continua forte. Sua vaga na Biblioteca Municipal da Penha está garantida das 9h às 18h.")
        String resumo,
        @JsonPropertyDescription("De 3 a 5 dicas práticas para o dia")
        List<Dica> dicas) {

    @Schema(name = "Dica")
    public record Dica(
            @JsonPropertyDescription("Título de até 4 palavras") String titulo,
            @JsonPropertyDescription("Descrição de até 20 palavras") String descricao) {
    }
}
