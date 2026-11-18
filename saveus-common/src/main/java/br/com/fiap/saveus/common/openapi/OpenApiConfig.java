package br.com.fiap.saveus.common.openapi;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Swagger/OpenAPI dos dois serviços: botão Authorize (Bearer JWT), schema ProblemDetails e respostas de erro
 * padronizadas (400, 401, 403, 500...) com exemplos reais em todas as operações.
 * Título e descrição vêm de saveus.openapi.* de cada serviço.
 */
@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_BEARER = "Bearer";
    public static final String SCHEMA_PROBLEMA = "ProblemDetails";
    private static final String PROBLEM_JSON = "application/problem+json";

    @Bean
    public OpenAPI openApi(@Value("${saveus.openapi.titulo:SaveUs API}") String titulo,
                           @Value("${saveus.openapi.descricao:}") String descricao) {
        Components componentes = new Components().addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Token devolvido por POST /auth/login (do Java ou do .NET: os dois emitem o mesmo JWT). "
                        + "Cole só o token, sem o prefixo Bearer."));
        componentes.addSchemas(SCHEMA_PROBLEMA,
                ModelConverters.getInstance().readAllAsResolvedSchema(ProblemaDoc.class).schema);

        return new OpenAPI()
                .info(new Info()
                        .title(titulo)
                        .version("v1")
                        .description(descricao)
                        .contact(new Contact().name("Equipe SaveUs — FIAP Global Solution 2026/2")))
                .components(componentes)
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }

    /**
     * Completa cada operação com as respostas de erro do contrato. Rotas com @SecurityRequirements vazio
     * (públicas, como /auth/login) não documentam 401/403 de token. Toda resposta 4xx/5xx sem corpo
     * documentado recebe o schema ProblemDetails.
     */
    @Bean
    public OperationCustomizer respostasDeErroPadrao() {
        return (operacao, metodo) -> {
            boolean publica = metodo.hasMethodAnnotation(SecurityRequirements.class)
                    || metodo.getBeanType().isAnnotationPresent(SecurityRequirements.class);
            boolean temEntrada = operacao.getRequestBody() != null
                    || (operacao.getParameters() != null && !operacao.getParameters().isEmpty());

            ApiResponses respostas = operacao.getResponses();
            if (temEntrada) {
                respostas.putIfAbsent("400", new ApiResponse().description("Dados inválidos"));
            }
            if (!publica) {
                respostas.putIfAbsent("401", new ApiResponse().description("Token ausente, inválido ou expirado"));
                respostas.putIfAbsent("403", new ApiResponse().description("O papel do usuário não permite esta operação"));
            }
            respostas.putIfAbsent("500", new ApiResponse().description("Erro inesperado (sem stack trace)"));

            respostas.forEach((codigo, resposta) -> {
                if (codigo.startsWith("4") || codigo.startsWith("5")) {
                    completarErro(codigo, resposta);
                }
            });
            return operacao;
        };
    }

    private static void completarErro(String codigo, ApiResponse resposta) {
        if (resposta.getContent() != null && !resposta.getContent().isEmpty()) {
            // Exemplo vindo da anotação do endpoint: só liga o schema ProblemDetails.
            resposta.getContent().values().stream()
                    .filter(tipo -> tipo.getSchema() == null)
                    .forEach(tipo -> tipo.setSchema(refProblema()));
            return;
        }
        MediaType tipo = new MediaType().schema(refProblema());
        Map<String, Example> exemplos = exemplosPadrao(codigo);
        if (!exemplos.isEmpty()) {
            tipo.setExamples(exemplos);
        }
        resposta.setContent(new Content().addMediaType(PROBLEM_JSON, tipo));
    }

    private static Map<String, Example> exemplosPadrao(String codigo) {
        Map<String, Example> exemplos = new LinkedHashMap<>();
        switch (codigo) {
            case "400" -> exemplos.put("validacao", exemplo("Campo inválido", Map.of(
                    "type", "https://tools.ietf.org/html/rfc9110#section-15.5.1",
                    "title", "Dados inválidos",
                    "status", 400,
                    "errors", Map.of(
                            "capacidade", List.of("A capacidade deve ser maior que zero."),
                            "fecha", List.of("O horário de fechamento deve ser depois da abertura.")))));
            case "401" -> {
                exemplos.put("semToken", exemplo("Sem token ou token inválido",
                        problema(401, "rfc9110#section-15.5.2", "Não autenticado", "Faça login para continuar.")));
                exemplos.put("expirado", exemplo("Token expirado (o app volta para o login)",
                        problema(401, "rfc9110#section-15.5.2", "Não autenticado", "Sua sessão expirou. Faça login novamente.")));
            }
            case "403" -> exemplos.put("papel", exemplo("Papel sem permissão",
                    problema(403, "rfc9110#section-15.5.4", "Acesso negado", "Você não tem permissão para isso.")));
            case "429" -> exemplos.put("limite", exemplo("Muitas tentativas (header Retry-After em segundos)",
                    problema(429, "rfc6585#section-4", "Muitas requisições",
                            "Você fez muitas tentativas seguidas. Tente de novo em 47 segundos.")));
            case "500" -> exemplos.put("interno", exemplo("Erro inesperado",
                    problema(500, "rfc9110#section-15.6.1", "Erro interno",
                            "Ocorreu um erro inesperado. Tente novamente em instantes.")));
            default -> {
                // 404, 409, 503: os exemplos específicos ficam nas anotações de cada endpoint.
            }
        }
        return exemplos;
    }

    private static Schema<Object> refProblema() {
        return new Schema<>().$ref("#/components/schemas/" + SCHEMA_PROBLEMA);
    }

    private static Map<String, Object> problema(int status, String secao, String titulo, String detalhe) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("type", "https://tools.ietf.org/html/" + secao);
        corpo.put("title", titulo);
        corpo.put("status", status);
        corpo.put("detail", detalhe);
        return corpo;
    }

    private static Example exemplo(String resumo, Object valor) {
        return new Example().summary(resumo).value(valor);
    }
}
