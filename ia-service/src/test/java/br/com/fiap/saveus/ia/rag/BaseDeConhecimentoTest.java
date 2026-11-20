package br.com.fiap.saveus.ia.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Avaliação da recuperação: perguntas reais de idosos, cada uma com o trecho que deveria aparecer.
 * O critério é hit@3 (o trecho certo entre os 3 primeiros), que é o que chega ao prompt.
 */
class BaseDeConhecimentoTest {

    private final BaseDeConhecimento base = new BaseDeConhecimento();

    @Test
    void carregaTodosOsTrechosDoArquivo() {
        assertThat(base.todos()).hasSize(12)
                .allSatisfy(t -> assertThat(t.texto()).isNotBlank().doesNotStartWith("palavras:"));
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource(delimiter = ';', value = {
            "Não tenho reserva, onde posso ficar?; sem-refugio",
            "Uso cadeira de rodas, como chego lá?; acessibilidade",
            "Estou com tontura e dor de cabeça; sinais-de-alerta",
            "Tomo remédio para pressão, posso tomar no calor?; remedios",
            "Moro sozinho e tenho medo de passar mal; companhia",
            "Minha casa está muito quente; casa-fresca",
            "Que roupa devo vestir?; roupas",
            "Que horas devo ir ao refúgio?; ir-ao-refugio",
            "Quanta água devo beber?; hidratacao",
            "Posso sair para caminhar ao meio-dia?; horario-do-sol",
            "O que comer nos dias quentes?; alimentacao",
            "Como dormir com a noite quente?; noite-quente",
    })
    void encontraOTrechoCertoEntreOsTresPrimeiros(String pergunta, String esperado) {
        List<String> ids = base.buscar(pergunta, 3).stream().map(Trecho::id).toList();

        assertThat(ids).contains(esperado);
    }

    @Test
    void perguntaForaDoAssuntoNaoTrazNada() {
        assertThat(base.buscar("resultado do jogo de futebol", 3)).isEmpty();
    }

    @Test
    void normalizaAcentosEPlural() {
        assertThat(BaseDeConhecimento.termos("Refúgios ACESSÍVEIS de água")).containsExactly("refugio", "acessivei", "agua");
    }
}
