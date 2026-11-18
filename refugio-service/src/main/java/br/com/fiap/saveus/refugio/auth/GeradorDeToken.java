package br.com.fiap.saveus.refugio.auth;

import br.com.fiap.saveus.common.seguranca.JwtConfig;
import br.com.fiap.saveus.common.seguranca.JwtProperties;
import br.com.fiap.saveus.refugio.common.FusoHorario;
import br.com.fiap.saveus.refugio.usuario.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Emite exatamente o JWT do .NET (GeradorDeTokenJwt): HS256, iss refugio-api, aud refugio-app,
 * 120 minutos, iat = nbf = agora, sub = id, email, name e role. Vale nos dois backends.
 */
@Component
public class GeradorDeToken {

    private final JwtEncoder encoder;
    private final JwtProperties propriedades;
    private final Clock relogio;

    public GeradorDeToken(JwtEncoder encoder, JwtProperties propriedades, Clock relogio) {
        this.encoder = encoder;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    public TokenGerado gerar(Usuario usuario) {
        Instant agora = FusoHorario.agora(relogio);
        Instant expiraEm = agora.plus(Duration.ofMinutes(propriedades.expiracaoMinutos()));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propriedades.emissor())
                .audience(List.of(propriedades.audiencia()))
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(agora)
                .notBefore(agora)
                .expiresAt(expiraEm)
                .claim("email", usuario.getEmail())
                .claim("name", usuario.getNome())
                .claim(JwtConfig.CLAIM_PAPEL, usuario.getPapel().name())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();

        String token = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenGerado(token, expiraEm);
    }

    public record TokenGerado(String token, Instant expiraEm) {
    }
}
