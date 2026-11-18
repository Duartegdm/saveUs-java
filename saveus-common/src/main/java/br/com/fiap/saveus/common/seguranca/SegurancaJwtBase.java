package br.com.fiap.saveus.common.seguranca;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

/**
 * Configuração comum aos dois serviços: API stateless, sem CSRF, autenticação só por Bearer JWT
 * e erros 401/403 no formato ProblemDetail. Cada serviço define as próprias regras de acesso.
 */
@Component
public class SegurancaJwtBase {

    /** Rotas abertas em todos os serviços: documentação e health check. */
    public static final String[] ROTAS_PUBLICAS = {
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health/**", "/error"
    };

    private final ProblemaAuthenticationEntryPoint entryPoint;
    private final ProblemaAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public SegurancaJwtBase(ProblemaAuthenticationEntryPoint entryPoint,
                            ProblemaAccessDeniedHandler accessDeniedHandler,
                            JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.entryPoint = entryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    public HttpSecurity aplicar(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
    }
}
