package br.com.fiap.saveus.common.erro;

import org.springframework.http.HttpStatus;

/**
 * Erro de regra de negócio que vira ProblemDetail com o status, o title e o detail informados.
 * As mensagens são em português e prontas para o app exibir.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String titulo;

    public ApiException(HttpStatus status, String titulo, String detalhe) {
        super(detalhe);
        this.status = status;
        this.titulo = titulo;
    }

    public static ApiException naoEncontrado(String titulo, String detalhe) {
        return new ApiException(HttpStatus.NOT_FOUND, titulo, detalhe);
    }

    public static ApiException conflito(String titulo, String detalhe) {
        return new ApiException(HttpStatus.CONFLICT, titulo, detalhe);
    }

    public static ApiException naoAutenticado(String titulo, String detalhe) {
        return new ApiException(HttpStatus.UNAUTHORIZED, titulo, detalhe);
    }

    public static ApiException servicoIndisponivel(String detalhe) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço indisponível", detalhe);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitulo() {
        return titulo;
    }
}
