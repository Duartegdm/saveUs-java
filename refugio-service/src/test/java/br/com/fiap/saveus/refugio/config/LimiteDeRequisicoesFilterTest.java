package br.com.fiap.saveus.refugio.config;

import br.com.fiap.saveus.common.erro.ProblemaWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteDeRequisicoesFilterTest {

    private final Clock relogio = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);
    private final LimiteDeRequisicoesFilter filtro =
            new LimiteDeRequisicoesFilter(new ProblemaWriter(new ObjectMapper()), relogio, 2, 60);

    @Test
    void depoisDoLimiteResponde429ComRetryAfter() throws Exception {
        assertThat(login("10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(login("10.0.0.1").getStatus()).isEqualTo(200);

        MockHttpServletResponse bloqueada = login("10.0.0.1");
        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getHeader("Retry-After")).isEqualTo("60");
        assertThat(bloqueada.getContentType()).startsWith("application/problem+json");
        assertThat(bloqueada.getContentAsString()).contains("Muitas requisições");

        // Outro IP tem a própria janela.
        assertThat(login("10.0.0.2").getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse login(String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
