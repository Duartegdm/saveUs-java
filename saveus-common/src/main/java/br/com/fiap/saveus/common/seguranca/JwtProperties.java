package br.com.fiap.saveus.common.seguranca;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Mesmo JWT do backend .NET (seção Jwt do appsettings): HS256, mesma chave, emissor e audiência.
 * A chave vem da variável de ambiente JWT_CHAVE, nunca do repositório.
 */
@ConfigurationProperties(prefix = "saveus.jwt")
public record JwtProperties(
        String chave,
        @DefaultValue("refugio-api") String emissor,
        @DefaultValue("refugio-app") String audiencia,
        @DefaultValue("120") int expiracaoMinutos,
        @DefaultValue("30") int toleranciaSegundos) {
}
