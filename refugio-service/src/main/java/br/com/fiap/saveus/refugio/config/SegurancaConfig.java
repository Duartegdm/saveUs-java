package br.com.fiap.saveus.refugio.config;

import br.com.fiap.saveus.common.seguranca.SegurancaJwtBase;
import br.com.fiap.saveus.refugio.auth.AspNetIdentityPasswordEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SegurancaConfig {

    private static final String GESTOR = "GESTOR";
    private static final String IDOSO = "IDOSO";

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http, SegurancaJwtBase base) throws Exception {
        base.aplicar(http)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(SegurancaJwtBase.ROTAS_PUBLICAS).permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/refugios", "/api/v1/refugios/**").authenticated()
                        .requestMatchers("/api/v1/refugios", "/api/v1/refugios/**").hasRole(GESTOR)
                        .requestMatchers(HttpMethod.GET, "/api/v1/cep/**").hasRole(GESTOR)
                        .requestMatchers(HttpMethod.POST, "/api/v1/ia/guia-do-calor").hasRole(IDOSO)
                        .anyRequest().authenticated());
        return http.build();
    }

    /** Verifica e gera senhas no formato do ASP.NET Core Identity, o mesmo do banco. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new AspNetIdentityPasswordEncoder();
    }

    /** Assina o token do login com a mesma chave HS256 do .NET. */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    /** O app nativo não precisa de CORS; isto libera a versão web do Expo (http://localhost:8081). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${saveus.cors.origens}") List<String> origens) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origens);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        config.setExposedHeaders(List.of("Location", "Retry-After"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
