package br.com.fiap.saveus.refugio.guia;

import br.com.fiap.saveus.refugio.guia.dto.GuiaGerado;
import br.com.fiap.saveus.refugio.guia.dto.GuiaGerado.Dica;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuiaIaGatewayTest {

    private static final Dica DICA = new Dica("Leve água", "Beba um copo a cada hora.");

    @Test
    void aceitaDe3a5DicasECortaOExcesso() {
        GuiaGerado guia = GuiaIaGateway.validar(new GuiaGerado(" Resumo. ", Collections.nCopies(7, DICA)));

        assertThat(guia.resumo()).isEqualTo("Resumo.");
        assertThat(guia.dicas()).hasSize(5);
    }

    @Test
    void recusaGuiaIncompleto() {
        assertThatThrownBy(() -> GuiaIaGateway.validar(new GuiaGerado("Resumo", List.of(DICA, DICA))))
                .isInstanceOf(GuiaIaGateway.GuiaInvalidoException.class);
        assertThatThrownBy(() -> GuiaIaGateway.validar(new GuiaGerado(" ", List.of(DICA, DICA, DICA))))
                .isInstanceOf(GuiaIaGateway.GuiaInvalidoException.class);
        assertThatThrownBy(() -> GuiaIaGateway.validar(null))
                .isInstanceOf(GuiaIaGateway.GuiaInvalidoException.class);
    }
}
