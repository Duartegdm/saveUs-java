package br.com.fiap.saveus.common.erro;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Escreve um ProblemDetail direto na resposta. Usado onde o @RestControllerAdvice não alcança:
 * filtros e handlers do Spring Security (401, 403) e o limite de requisições (429).
 */
@Component
public class ProblemaWriter {

    private final ObjectMapper objectMapper;

    public ProblemaWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void escrever(HttpServletRequest request, HttpServletResponse response, ProblemDetail problema) throws IOException {
        if (problema.getInstance() == null) {
            problema.setInstance(URI.create(request.getRequestURI()));
        }
        response.setStatus(problema.getStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
