package br.com.fiap.saveus.ia.guia;

import br.com.fiap.saveus.common.erro.ApiException;
import br.com.fiap.saveus.ia.guia.dto.ContextoGuia;
import br.com.fiap.saveus.ia.guia.dto.GuiaGerado;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testa o fluxo do Spring AI (prompt + saída estruturada) com um ChatModel falso, sem chamar um LLM de verdade. */
class GuiaDoCalorServiceTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-26T15:00:00Z"), ZoneOffset.UTC);
    private static final ContextoGuia CONTEXTO = new ContextoGuia("Maria", LocalDate.of(2026, 11, 27), "Penha",
            new ContextoGuia.RefugioContexto("Biblioteca Municipal da Penha", "Rua Padre Benedito de Camargo, 400 — Penha",
                    LocalTime.of(9, 0), LocalTime.of(18, 0), true));

    @Test
    void converteOJsonDoModeloEMandaOContextoReal() {
        AtomicReference<Prompt> enviado = new AtomicReference<>();
        GuiaDoCalorService service = servico(enviado, """
                {"resumo": "Amanhã o calor continua forte.",
                 "dicas": [
                   {"titulo": "Saia cedo", "descricao": "Vá ao refúgio às 9h."},
                   {"titulo": "Leve água", "descricao": "Beba um copo a cada hora."},
                   {"titulo": "Roupa leve", "descricao": "Use roupas claras."}
                 ]}
                """);

        GuiaGerado guia = service.gerar(CONTEXTO);

        assertThat(guia.resumo()).isEqualTo("Amanhã o calor continua forte.");
        assertThat(guia.dicas()).extracting(GuiaGerado.Dica::titulo).containsExactly("Saia cedo", "Leve água", "Roupa leve");

        String prompt = enviado.get().getContents();
        assertThat(prompt).contains("Use somente os dados do contexto");
        assertThat(prompt).contains("Biblioteca Municipal da Penha", "das 09:00 às 18:00", "27/11/2026 (amanhã)");
    }

    @Test
    void semReservaAvisaOModelo() {
        AtomicReference<Prompt> enviado = new AtomicReference<>();
        GuiaDoCalorService service = servico(enviado, "{}");

        String contexto = service.montarContexto(new ContextoGuia("José", LocalDate.of(2026, 11, 26), null, null));

        assertThat(contexto).contains("Refúgio reservado: nenhum", "Bairro: não informado", "26/11/2026 (hoje)");
    }

    @Test
    void respostaQueNaoConverteOuIncompletaVira503() {
        assertThatThrownBy(() -> servico(new AtomicReference<>(), "isto não é JSON").gerar(CONTEXTO))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> servico(new AtomicReference<>(),
                "{\"resumo\": \"Oi\", \"dicas\": [{\"titulo\": \"A\", \"descricao\": \"B\"}]}").gerar(CONTEXTO))
                .isInstanceOf(ApiException.class)
                .hasMessage("A IA devolveu menos de 3 dicas.");
    }

    private static GuiaDoCalorService servico(AtomicReference<Prompt> enviado, String respostaDoModelo) {
        ChatModel modelo = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                enviado.set(prompt);
                return new ChatResponse(List.of(new Generation(new AssistantMessage(respostaDoModelo))));
            }
        };
        return new GuiaDoCalorService(ChatClient.builder(modelo), RELOGIO);
    }
}
