package br.com.fiap.saveus.refugio.refugio.dto;

import br.com.fiap.saveus.refugio.refugio.Refugio;
import br.com.fiap.saveus.refugio.refugio.TipoRefugio;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

/** Objeto Refugio do contrato, campo por campo igual ao .NET. vagasDisponiveis é calculado para a data pedida. */
@Schema(name = "Refugio", description = "Refúgio climático, no mesmo formato do backend .NET")
public record RefugioResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Biblioteca Municipal da Penha") String nome,
        @Schema(example = "BIBLIOTECA") TipoRefugio tipo,
        @Schema(description = "Sempre no formato 00000-000", example = "03601-000") String cep,
        @Schema(example = "Rua Padre Benedito de Camargo, 400") String endereco,
        @Schema(example = "Penha") String bairro,
        @Schema(description = "Total de vagas por dia", example = "40") Integer capacidade,
        @Schema(description = "capacidade − reservas ATIVA da data; 0 se o refúgio estiver inativo", example = "12")
        Integer vagasDisponiveis,
        @Schema(type = "string", pattern = "HH:mm", example = "09:00") @JsonFormat(pattern = "HH:mm") LocalTime abre,
        @Schema(type = "string", pattern = "HH:mm", example = "18:00") @JsonFormat(pattern = "HH:mm") LocalTime fecha,
        @Schema(description = "Acessível para cadeira de rodas", example = "true") Boolean acessivel,
        @Schema(description = "false = pausado pelo gestor (não aceita reservas)", example = "true") Boolean ativo) {

    public static RefugioResponse de(Refugio r, long vagasOcupadas) {
        return new RefugioResponse(r.getId(), r.getNome(), r.getTipo(), r.getCep(), r.getEndereco(), r.getBairro(),
                r.getCapacidade(), r.vagasDisponiveis(vagasOcupadas), r.getAbre(), r.getFecha(),
                r.getAcessivel(), r.getAtivo());
    }
}
