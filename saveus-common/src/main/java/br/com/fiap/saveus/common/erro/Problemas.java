package br.com.fiap.saveus.common.erro;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fábrica de ProblemDetail (RFC 7807) no mesmo formato do backend .NET:
 * status, title, detail e, nos erros de validação, errors com uma lista de mensagens por campo.
 */
public final class Problemas {

    public static final String TITULO_VALIDACAO = "Dados inválidos";

    private Problemas() {
    }

    public static ProblemDetail criar(HttpStatusCode status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatus(status);
        problema.setType(tipo(status.value()));
        problema.setTitle(titulo);
        problema.setDetail(detalhe);
        return problema;
    }

    public static ProblemDetail validacao(String campo, String mensagem) {
        Map<String, List<String>> erros = new LinkedHashMap<>();
        erros.put(campo, List.of(mensagem));
        return validacao(erros);
    }

    public static ProblemDetail validacao(Map<String, List<String>> erros) {
        ProblemDetail problema = criar(HttpStatus.BAD_REQUEST, TITULO_VALIDACAO, null);
        problema.setProperty("errors", erros);
        return problema;
    }

    /** Título padrão em português para erros sem título específico. */
    public static String tituloPadrao(int status) {
        return switch (status) {
            case 400 -> "Requisição inválida";
            case 401 -> "Não autenticado";
            case 403 -> "Acesso negado";
            case 404 -> "Recurso não encontrado";
            case 405 -> "Método não permitido";
            case 406 -> "Formato não aceito";
            case 409 -> "Conflito";
            case 413 -> "Requisição muito grande";
            case 415 -> "Formato não suportado";
            case 429 -> "Muitas requisições";
            case 503 -> "Serviço indisponível";
            default -> status >= 500 ? "Erro interno" : "Requisição inválida";
        };
    }

    /** Mesmos links de "type" que o ASP.NET Core usa no ProblemDetails. */
    private static URI tipo(int status) {
        String secao = switch (status) {
            case 400 -> "rfc9110#section-15.5.1";
            case 401 -> "rfc9110#section-15.5.2";
            case 403 -> "rfc9110#section-15.5.4";
            case 404 -> "rfc9110#section-15.5.5";
            case 405 -> "rfc9110#section-15.5.6";
            case 406 -> "rfc9110#section-15.5.7";
            case 409 -> "rfc9110#section-15.5.10";
            case 415 -> "rfc9110#section-15.5.16";
            case 422 -> "rfc9110#section-15.5.21";
            case 429 -> "rfc6585#section-4";
            case 503 -> "rfc9110#section-15.6.4";
            default -> status >= 500 ? "rfc9110#section-15.6.1" : "rfc9110#section-15.5.1";
        };
        return URI.create("https://tools.ietf.org/html/" + secao);
    }
}
