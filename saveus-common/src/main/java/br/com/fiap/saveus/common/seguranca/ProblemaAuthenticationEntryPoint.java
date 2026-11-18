package br.com.fiap.saveus.common.seguranca;

import br.com.fiap.saveus.common.erro.ProblemaWriter;
import br.com.fiap.saveus.common.erro.Problemas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 401 em ProblemDetail: sem token, token inválido ou expirado (o Spring Security não passa pelo advice). */
@Component
public class ProblemaAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String DETALHE_EXPIRADO = "Sua sessão expirou. Faça login novamente.";
    static final String DETALHE_PADRAO = "Faça login para continuar.";

    private final ProblemaWriter writer;

    public ProblemaAuthenticationEntryPoint(ProblemaWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        String detalhe = tokenExpirado(ex) ? DETALHE_EXPIRADO : DETALHE_PADRAO;
        writer.escrever(request, response, Problemas.criar(HttpStatus.UNAUTHORIZED, "Não autenticado", detalhe));
    }

    /** O JwtTimestampValidator descreve o erro como "Jwt expired at ...". */
    static boolean tokenExpirado(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains("Jwt expired")) {
                return true;
            }
            if (t.getCause() == t) break;
        }
        return false;
    }
}
