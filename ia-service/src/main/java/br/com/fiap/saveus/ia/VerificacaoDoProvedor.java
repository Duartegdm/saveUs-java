package br.com.fiap.saveus.ia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Avisa no log, ao subir, qual LLM está em uso e se falta a chave da OpenAI. */
@Component
public class VerificacaoDoProvedor {

    private static final Logger log = LoggerFactory.getLogger(VerificacaoDoProvedor.class);

    private final String provedor;
    private final String chaveOpenAi;
    private final String modeloOpenAi;
    private final String modeloOllama;

    public VerificacaoDoProvedor(@Value("${spring.ai.model.chat:openai}") String provedor,
                                 @Value("${spring.ai.openai.api-key:}") String chaveOpenAi,
                                 @Value("${spring.ai.openai.chat.options.model:}") String modeloOpenAi,
                                 @Value("${spring.ai.ollama.chat.options.model:}") String modeloOllama) {
        this.provedor = provedor;
        this.chaveOpenAi = chaveOpenAi;
        this.modeloOpenAi = modeloOpenAi;
        this.modeloOllama = modeloOllama;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verificar() {
        if ("ollama".equals(provedor)) {
            log.info("Guia do Calor usando Ollama, modelo {}", modeloOllama);
            return;
        }
        if (chaveOpenAi.isBlank() || chaveOpenAi.startsWith("defina-")) {
            log.warn("OPENAI_API_KEY não definida: o Guia do Calor vai sair no modo padrão (geradoPorIa=false).");
        } else {
            log.info("Guia do Calor usando OpenAI, modelo {}", modeloOpenAi);
        }
    }
}
