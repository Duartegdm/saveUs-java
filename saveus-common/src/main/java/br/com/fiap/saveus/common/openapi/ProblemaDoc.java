package br.com.fiap.saveus.common.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Só para a documentação: o formato do ProblemDetail (RFC 7807) que todos os erros devolvem,
 * em application/problem+json. É o mesmo formato do backend .NET.
 */
@Schema(name = "ProblemDetails",
        description = "Erro no formato RFC 7807, igual ao do .NET. O app mostra `detail` ao usuário e usa `errors` "
                + "para marcar cada campo do formulário.")
public record ProblemaDoc(
        @Schema(description = "Link da RFC que descreve o status",
                example = "https://tools.ietf.org/html/rfc9110#section-15.5.10")
        String type,
        @Schema(description = "Título curto, em português", example = "Refúgio com reservas")
        String title,
        @Schema(description = "Status HTTP", example = "409")
        int status,
        @Schema(description = "Mensagem pronta para exibir ao usuário",
                example = "Este refúgio tem reservas ativas para hoje ou datas futuras. Desative-o em vez de excluir.")
        String detail,
        @Schema(description = "Caminho da requisição", example = "/api/v1/refugios/1")
        String instance,
        @Schema(description = "Só em erros de validação (400): campo em camelCase → mensagens",
                example = "{\"capacidade\": [\"A capacidade deve ser maior que zero.\"]}")
        Map<String, List<String>> errors) {
}
