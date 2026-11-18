package br.com.fiap.saveus.refugio.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginação do contrato com o app (o Page do Spring usa content/number/totalElements e o app não entende).
 * pagina começa em 1.
 */
@Schema(description = "Envelope de paginação do contrato com o app")
public record PaginaResponse<T>(
        @Schema(description = "Itens da página") List<T> itens,
        @Schema(description = "Página atual, a partir de 1", example = "1") int pagina,
        @Schema(description = "Itens por página (1 a 50)", example = "10") int tamanho,
        @Schema(description = "Total de itens com os filtros aplicados", example = "23") long totalItens,
        @Schema(description = "Total de páginas", example = "3") int totalPaginas) {

    public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new PaginaResponse<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber() + 1,
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
