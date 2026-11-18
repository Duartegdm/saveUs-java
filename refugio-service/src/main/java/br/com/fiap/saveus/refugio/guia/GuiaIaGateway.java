package br.com.fiap.saveus.refugio.guia;

import br.com.fiap.saveus.refugio.guia.dto.ContextoGuia;
import br.com.fiap.saveus.refugio.guia.dto.GuiaGerado;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chama o ia-service e confere a saída. Guarda o guia em cache por contexto (idoso, data e reserva),
 * para não chamar o LLM toda vez que a tela abre. Exceções não são guardadas: a próxima abertura tenta de novo.
 */
@Component
public class GuiaIaGateway {

    public static final String CACHE = "guias-do-calor";

    private final IaServiceClient iaService;

    public GuiaIaGateway(IaServiceClient iaService) {
        this.iaService = iaService;
    }

    @Cacheable(cacheNames = CACHE, key = "#contexto")
    public GuiaGerado gerar(ContextoGuia contexto) {
        return validar(iaService.gerarGuia(contexto));
    }

    /** resumo preenchido e de 3 a 5 dicas completas; o que vier além de 5 é descartado. */
    static GuiaGerado validar(GuiaGerado guia) {
        if (guia == null || guia.resumo() == null || guia.resumo().isBlank() || guia.dicas() == null) {
            throw new GuiaInvalidoException("Guia sem resumo ou sem dicas");
        }
        List<GuiaGerado.Dica> dicas = guia.dicas().stream()
                .filter(d -> d != null && d.titulo() != null && !d.titulo().isBlank()
                        && d.descricao() != null && !d.descricao().isBlank())
                .map(d -> new GuiaGerado.Dica(d.titulo().trim(), d.descricao().trim()))
                .limit(5)
                .toList();
        if (dicas.size() < 3) {
            throw new GuiaInvalidoException("O guia veio com " + dicas.size() + " dicas válidas");
        }
        return new GuiaGerado(guia.resumo().trim(), dicas);
    }

    static class GuiaInvalidoException extends RuntimeException {
        GuiaInvalidoException(String mensagem) {
            super(mensagem);
        }
    }
}
