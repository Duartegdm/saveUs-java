package br.com.fiap.saveus.refugio.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AspNetIdentityPasswordEncoderTest {

    /** Hash real de gestor@refugio.com no script do banco do .NET. */
    private static final String HASH_DO_NET =
            "AQAAAAIAAYagAAAAEJWoSE7CxHY9E9IT4BaCOr481x9z1ONs1Kp3nY9rCsl0tQmrpA9ujFZ7+Xoa8BSMyw==";

    private final AspNetIdentityPasswordEncoder encoder = new AspNetIdentityPasswordEncoder();

    @Test
    void aceitaASenhaCorretaContraOHashGeradoPeloNet() {
        assertThat(encoder.matches("Senha@123", HASH_DO_NET)).isTrue();
    }

    @Test
    void recusaSenhaErrada() {
        assertThat(encoder.matches("senha@123", HASH_DO_NET)).isFalse();
        assertThat(encoder.matches("", HASH_DO_NET)).isFalse();
    }

    @Test
    void recusaHashCorrompidoSemLancarExcecao() {
        assertThat(encoder.matches("Senha@123", "isto-nao-e-base64!")).isFalse();
        assertThat(encoder.matches("Senha@123", "AAAA")).isFalse();
        assertThat(encoder.matches("Senha@123", null)).isFalse();
    }

    @Test
    void hashGeradoTemOFormatoV3EConfere() {
        String hash = encoder.encode("Outra@Senha1");

        assertThat(hash).startsWith("AQAAAAIAAYagAAAAE"); // 0x01 | PRF 2 | 100000 | sal 16
        assertThat(encoder.matches("Outra@Senha1", hash)).isTrue();
        assertThat(encoder.encode("Outra@Senha1")).isNotEqualTo(hash); // sal aleatório
    }
}
