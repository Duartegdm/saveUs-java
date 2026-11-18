package br.com.fiap.saveus.common.seguranca;

import br.com.fiap.saveus.common.erro.ProblemaWriter;
import br.com.fiap.saveus.common.erro.Problemas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 403 em ProblemDetail: o papel do token não permite a operação. */
@Component
public class ProblemaAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemaWriter writer;

    public ProblemaAccessDeniedHandler(ProblemaWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        writer.escrever(request, response,
                Problemas.criar(HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para isso."));
    }
}
