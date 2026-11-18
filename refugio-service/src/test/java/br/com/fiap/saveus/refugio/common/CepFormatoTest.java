package br.com.fiap.saveus.refugio.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CepFormatoTest {

    @ParameterizedTest
    @ValueSource(strings = {"03601000", "03601-000", " 03601-000 "})
    void normalizaParaOFormatoDoBanco(String entrada) {
        assertThat(CepFormato.normalizar(entrada)).contains("03601-000");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0360100", "036010000", "0360-1000", "03601_000", "abcde-fgh", "03601-00a"})
    void recusaOQueNaoTem8Digitos(String entrada) {
        assertThat(CepFormato.normalizar(entrada)).isEmpty();
    }
}
