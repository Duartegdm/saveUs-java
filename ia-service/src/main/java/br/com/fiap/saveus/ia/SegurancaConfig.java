package br.com.fiap.saveus.ia;

import br.com.fiap.saveus.common.seguranca.SegurancaJwtBase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * O endpoint interno valida o mesmo JWT do idoso, repassado pelo refugio-service.
 * Sem token válido de IDOSO, ninguém consegue usar o LLM por aqui.
 */
@Configuration
@EnableWebSecurity
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http, SegurancaJwtBase base) throws Exception {
        base.aplicar(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SegurancaJwtBase.ROTAS_PUBLICAS).permitAll()
                        .requestMatchers("/interno/**").hasRole("IDOSO")
                        .anyRequest().authenticated());
        return http.build();
    }
}
