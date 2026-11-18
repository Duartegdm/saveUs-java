package br.com.fiap.saveus.refugio.config;

import br.com.fiap.saveus.common.erro.ProblemaWriter;
import br.com.fiap.saveus.common.erro.Problemas;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limite de tentativas em /auth/login e /auth/registrar, por IP, em janela fixa (como o .NET):
 * freia força bruta de senha. Excedeu: 429 em ProblemDetail com o header Retry-After em segundos.
 */
@Component
public class LimiteDeRequisicoesFilter extends OncePerRequestFilter {

    private static final int MAX_CHAVES = 10_000;

    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private final ProblemaWriter writer;
    private final Clock relogio;
    private final int limite;
    private final long janelaMs;

    public LimiteDeRequisicoesFilter(ProblemaWriter writer, Clock relogio,
                                     @Value("${saveus.limite-auth.requisicoes:10}") int limite,
                                     @Value("${saveus.limite-auth.janela-segundos:60}") int janelaSegundos) {
        this.writer = writer;
        this.relogio = relogio;
        this.limite = limite;
        this.janelaMs = janelaSegundos * 1000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !request.getRequestURI().startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long agora = relogio.millis();
        if (janelas.size() > MAX_CHAVES) {
            janelas.values().removeIf(j -> agora - j.inicio >= janelaMs);
        }

        Janela janela = janelas.compute(request.getRemoteAddr(),
                (ip, atual) -> atual == null || agora - atual.inicio >= janelaMs ? new Janela(agora) : atual);

        if (janela.contagem.incrementAndGet() <= limite) {
            chain.doFilter(request, response);
            return;
        }

        long segundos = Math.max(1, (long) Math.ceil((janela.inicio + janelaMs - agora) / 1000.0));
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundos));
        ProblemDetail problema = Problemas.criar(HttpStatus.TOO_MANY_REQUESTS, "Muitas requisições",
                "Você fez muitas tentativas seguidas. Tente de novo em " + segundos + " segundos.");
        writer.escrever(request, response, problema);
    }

    private static final class Janela {
        private final long inicio;
        private final AtomicInteger contagem = new AtomicInteger();

        private Janela(long inicio) {
            this.inicio = inicio;
        }
    }
}
