package br.com.fiap.saveus.refugio;

import br.com.fiap.saveus.refugio.guia.IaServiceClient;
import br.com.fiap.saveus.refugio.guia.dto.GuiaGerado;
import br.com.fiap.saveus.refugio.refugio.Refugio;
import br.com.fiap.saveus.refugio.refugio.RefugioRepository;
import br.com.fiap.saveus.refugio.refugio.TipoRefugio;
import br.com.fiap.saveus.refugio.reserva.Reserva;
import br.com.fiap.saveus.refugio.reserva.ReservaRepository;
import br.com.fiap.saveus.refugio.reserva.StatusReserva;
import br.com.fiap.saveus.refugio.usuario.Papel;
import br.com.fiap.saveus.refugio.usuario.Usuario;
import br.com.fiap.saveus.refugio.usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato com o app de ponta a ponta (H2 no lugar do Oracle; ia-service simulado). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefugioApiIntegrationTest {

    private static final String HASH_SENHA_123 =
            "AQAAAAIAAYagAAAAEJWoSE7CxHY9E9IT4BaCOr481x9z1ONs1Kp3nY9rCsl0tQmrpA9ujFZ7+Xoa8BSMyw==";
    private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired RefugioRepository refugios;
    @Autowired ReservaRepository reservas;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired JwtEncoder jwtEncoder;

    @MockitoBean IaServiceClient iaService;

    Usuario maria;
    Refugio biblioteca;
    Refugio ceu;

    @BeforeEach
    void popularBanco() {
        reservas.deleteAll();
        refugios.deleteAll();
        usuarios.deleteAll();

        usuarios.save(new Usuario("Gestora Refúgio", "gestor@refugio.com", HASH_SENHA_123, Papel.GESTOR, agoraUtc()));
        maria = new Usuario("Maria Souza", "maria@email.com", HASH_SENHA_123, Papel.IDOSO, agoraUtc());
        maria.setBairro("Penha");
        usuarios.save(maria);

        biblioteca = refugios.save(new Refugio("Biblioteca Municipal da Penha", TipoRefugio.BIBLIOTECA, "03601-000",
                "Rua Exemplo, 100", "Penha", 20, LocalTime.of(8, 0), LocalTime.of(20, 0), true, true));
        ceu = refugios.save(new Refugio("CEU Penha", TipoRefugio.CEU, "03602-000",
                "Avenida Modelo, 2500", "Penha", 40, LocalTime.of(7, 0), LocalTime.of(22, 0), true, true));
        refugios.save(new Refugio("UBS Vila Matilde", TipoRefugio.UBS, "03510-000",
                "Rua Fictícia, 45", "Vila Matilde", 10, LocalTime.of(7, 0), LocalTime.of(19, 0), true, false));
    }

    // ---------------------------------------------------------------- Autenticação

    @Nested
    class Login {

        @Test
        void devolveOMesmoJwtDoNet() throws Exception {
            JsonNode corpo = corpo(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"  GESTOR@refugio.com \",\"senha\":\"Senha@123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usuario.papel").value("GESTOR"))
                    .andExpect(jsonPath("$.usuario.email").value("gestor@refugio.com"))
                    .andExpect(jsonPath("$.expiraEm", endsWith("-03:00"))));

            Jwt jwt = jwtDecoder.decode(corpo.get("token").asText());
            assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
            assertThat(jwt.getClaimAsString("iss")).isEqualTo("refugio-api");
            assertThat(jwt.getAudience()).containsExactly("refugio-app");
            assertThat(jwt.getSubject()).isEqualTo(String.valueOf(corpo.at("/usuario/id").asLong()));
            assertThat(jwt.getClaimAsString("role")).isEqualTo("GESTOR");
            assertThat(jwt.getClaimAsString("name")).isEqualTo("Gestora Refúgio");
            assertThat(jwt.getClaimAsString("email")).isEqualTo("gestor@refugio.com");
            assertThat(jwt.getNotBefore()).isEqualTo(jwt.getIssuedAt());
            assertThat(ChronoUnit.MINUTES.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(120);
        }

        @Test
        void mesmaMensagemParaSenhaErradaEEmailInexistente() throws Exception {
            for (String corpo : List.of("{\"email\":\"gestor@refugio.com\",\"senha\":\"errada\"}",
                    "{\"email\":\"ninguem@refugio.com\",\"senha\":\"Senha@123\"}")) {
                mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                        .andExpect(status().isUnauthorized())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.title").value("Credenciais inválidas"))
                        .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
            }
        }

        @Test
        void camposVaziosDao400PorCampo() throws Exception {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\" \"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Dados inválidos"))
                    .andExpect(jsonPath("$.errors.email[0]").value("Informe o e-mail."))
                    .andExpect(jsonPath("$.errors.senha[0]").value("Informe a senha."));
        }
    }

    @Nested
    class Registrar {

        @Test
        void criaSempreIdosoE409NoEmailRepetido() throws Exception {
            String corpo = "{\"nome\":\"Ana Lima\",\"email\":\"Ana.Lima@email.com\",\"senha\":\"Senha@123\"}";
            mvc.perform(post("/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/usuarios/me"))
                    .andExpect(jsonPath("$.papel").value("IDOSO"))
                    .andExpect(jsonPath("$.email").value("ana.lima@email.com"))
                    .andExpect(jsonPath("$.token").doesNotExist());

            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"ana.lima@email.com\",\"senha\":\"Senha@123\"}"))
                    .andExpect(status().isOk());

            mvc.perform(post("/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("E-mail já cadastrado"))
                    .andExpect(jsonPath("$.detail").value("Já existe uma conta com este e-mail. Faça login."));
        }

        @Test
        void senhaFracaTemUmaMensagemClara() throws Exception {
            mvc.perform(post("/auth/registrar").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Ana\",\"email\":\"ana@email.com\",\"senha\":\"senhafraca1\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.senha", hasSize(1)))
                    .andExpect(jsonPath("$.errors.senha[0]").value("A senha deve ter uma letra maiúscula."));
        }
    }

    @Nested
    class Seguranca {

        @Test
        void semToken401() throws Exception {
            mvc.perform(get("/api/v1/refugios"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Não autenticado"))
                    .andExpect(jsonPath("$.detail").value("Faça login para continuar."));
        }

        @Test
        void tokenExpiradoTemMensagemPropria() throws Exception {
            Instant passado = Instant.now().minus(3, ChronoUnit.HOURS);
            String expirado = token(passado, passado.plus(120, ChronoUnit.MINUTES), "GESTOR");

            mvc.perform(get("/api/v1/refugios").header("Authorization", "Bearer " + expirado))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Sua sessão expirou. Faça login novamente."));
        }

        @Test
        void tokenComOutraChave401() throws Exception {
            String adulterado = loginToken("gestor@refugio.com");
            adulterado = adulterado.substring(0, adulterado.length() - 4) + "AAAA";
            mvc.perform(get("/api/v1/refugios").header("Authorization", "Bearer " + adulterado))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Faça login para continuar."));
        }

        @Test
        void idosoLeMasNaoEscreveRefugio() throws Exception {
            String idoso = loginToken("maria@email.com");
            mvc.perform(autenticado(get("/api/v1/refugios"), idoso)).andExpect(status().isOk());
            mvc.perform(autenticado(post("/api/v1/refugios"), idoso).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(refugioValido())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Acesso negado"));
            mvc.perform(autenticado(get("/api/v1/cep/03601000"), idoso)).andExpect(status().isForbidden());
        }

        @Test
        void gestorNaoUsaOGuiaDoIdoso() throws Exception {
            mvc.perform(autenticado(post("/api/v1/ia/guia-do-calor"), loginToken("gestor@refugio.com")))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------- Refúgios

    @Nested
    class CrudDeRefugios {

        @Test
        void listaPaginadaNoEnvelopeDoContrato() throws Exception {
            reservas.save(reserva(maria, biblioteca, HOJE, StatusReserva.ATIVA));
            reservas.save(reserva(maria, biblioteca, HOJE, StatusReserva.CANCELADA));

            mvc.perform(autenticado(get("/api/v1/refugios").param("tamanho", "2"), gestor()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pagina").value(1))
                    .andExpect(jsonPath("$.tamanho").value(2))
                    .andExpect(jsonPath("$.totalItens").value(3))
                    .andExpect(jsonPath("$.totalPaginas").value(2))
                    .andExpect(jsonPath("$.itens", hasSize(2)))
                    .andExpect(jsonPath("$.itens[0].nome").value("Biblioteca Municipal da Penha"))
                    .andExpect(jsonPath("$.itens[0].vagasDisponiveis").value(19))
                    .andExpect(jsonPath("$.itens[0].abre").value("08:00"))
                    .andExpect(jsonPath("$.itens[0].tipo").value("BIBLIOTECA"))
                    .andExpect(jsonPath("$.itens[1].nome").value("CEU Penha"))
                    .andExpect(jsonPath("$.content").doesNotExist());
        }

        @Test
        void filtraPorBairroSemDiferenciarMaiusculasEPorAtivo() throws Exception {
            mvc.perform(autenticado(get("/api/v1/refugios").param("bairro", "vila matilde"), gestor()))
                    .andExpect(jsonPath("$.totalItens").value(1))
                    .andExpect(jsonPath("$.itens[0].vagasDisponiveis").value(0)); // inativo
            mvc.perform(autenticado(get("/api/v1/refugios").param("ativo", "true"), gestor()))
                    .andExpect(jsonPath("$.totalItens").value(2));
        }

        @Test
        void parametrosInvalidosSaemEmErrors() throws Exception {
            mvc.perform(autenticado(get("/api/v1/refugios").param("data", "15-11-2026"), gestor()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.data[0]").value("Data em formato inválido. Use yyyy-MM-dd, como 2026-11-15."));
            mvc.perform(autenticado(get("/api/v1/refugios").param("tamanho", "51").param("pagina", "0"), gestor()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.tamanho[0]").value("O tamanho da página deve ficar entre 1 e 50."))
                    .andExpect(jsonPath("$.errors.pagina[0]").value("A página começa em 1."));
        }

        @Test
        void criaComCepNormalizadoELocation() throws Exception {
            Map<String, Object> corpo = refugioValido();
            corpo.put("cep", "08210000");
            corpo.put("nome", "  Centro Comunitário Itaquera  ");

            mvc.perform(autenticado(post("/api/v1/refugios"), gestor()).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/refugios/\\d+")))
                    .andExpect(jsonPath("$.cep").value("08210-000"))
                    .andExpect(jsonPath("$.nome").value("Centro Comunitário Itaquera"))
                    .andExpect(jsonPath("$.tipo").value("CENTRO_COMUNITARIO"))
                    .andExpect(jsonPath("$.vagasDisponiveis").value(25))
                    .andExpect(jsonPath("$.fecha").value("18:00"));
        }

        @Test
        void validacaoDoFormularioPorCampo() throws Exception {
            Map<String, Object> corpo = refugioValido();
            corpo.put("fecha", "07:00");
            corpo.put("capacidade", 0);
            corpo.put("cep", "123");
            corpo.remove("acessivel");

            mvc.perform(autenticado(post("/api/v1/refugios"), gestor()).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Dados inválidos"))
                    .andExpect(jsonPath("$.errors.fecha[0]").value("O horário de fechamento deve ser depois da abertura."))
                    .andExpect(jsonPath("$.errors.capacidade[0]").value("A capacidade deve ser maior que zero."))
                    .andExpect(jsonPath("$.errors.cep[0]").value("O CEP deve ter 8 dígitos, como 03601-000."))
                    .andExpect(jsonPath("$.errors.acessivel[0]").value("Informe se o refúgio é acessível."));
        }

        @Test
        void numeroEmStringEEnumDesconhecidoSaoRecusados() throws Exception {
            Map<String, Object> corpo = refugioValido();
            corpo.put("capacidade", "20");
            mvc.perform(autenticado(post("/api/v1/refugios"), gestor()).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.capacidade[0]").value("Valor em formato inválido."));

            corpo = refugioValido();
            corpo.put("tipo", "HOSPITAL");
            mvc.perform(autenticado(post("/api/v1/refugios"), gestor()).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.tipo[0]").value("Valor em formato inválido."));

            mvc.perform(autenticado(post("/api/v1/refugios"), gestor()).contentType(MediaType.APPLICATION_JSON)
                            .content("{ isto não é json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.corpo[0]").value("O corpo da requisição não é um JSON válido."));
        }

        @Test
        void putPausaORefugio() throws Exception {
            Map<String, Object> corpo = refugioValido();
            corpo.put("ativo", false);

            mvc.perform(autenticado(put("/api/v1/refugios/" + ceu.getId()), gestor()).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ceu.getId()))
                    .andExpect(jsonPath("$.ativo").value(false))
                    .andExpect(jsonPath("$.vagasDisponiveis").value(0));

            mvc.perform(autenticado(put("/api/v1/refugios/999999"), gestor()).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(corpo)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Refúgio não encontrado"));
        }

        @Test
        void naoExcluiRefugioComReservaFuturaMasExcluiComReservaPassada() throws Exception {
            reservas.save(reserva(maria, biblioteca, HOJE.plusDays(2), StatusReserva.ATIVA));
            reservas.save(reserva(maria, ceu, HOJE.minusDays(5), StatusReserva.ATIVA));

            mvc.perform(autenticado(delete("/api/v1/refugios/" + biblioteca.getId()), gestor()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Refúgio com reservas"))
                    .andExpect(jsonPath("$.detail")
                            .value("Este refúgio tem reservas ativas para hoje ou datas futuras. Desative-o em vez de excluir."));

            // No Oracle a FK tem ON DELETE CASCADE; o H2 do teste não, então a reserva passada sai aqui.
            reservas.deleteAll(reservas.findAll().stream().filter(r -> r.getRefugio().getId().equals(ceu.getId())).toList());
            mvc.perform(autenticado(delete("/api/v1/refugios/" + ceu.getId()), gestor()))
                    .andExpect(status().isNoContent());
            mvc.perform(autenticado(get("/api/v1/refugios/" + ceu.getId()), gestor()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Este refúgio não está mais disponível."));
        }

        @Test
        void cepInvalidoNaoChamaOViaCep() throws Exception {
            mvc.perform(autenticado(get("/api/v1/cep/0360"), gestor()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.cep[0]").value("O CEP deve ter 8 dígitos, como 03601-000."));
        }
    }

    // ---------------------------------------------------------------- Guia do Calor

    @Nested
    class GuiaDoCalor {

        @Test
        void usaAReservaDoIdosoEOTextoDaIa() throws Exception {
            reservas.save(reserva(maria, biblioteca, HOJE.plusDays(1), StatusReserva.ATIVA));
            when(iaService.gerarGuia(any())).thenReturn(new GuiaGerado("Amanhã o calor continua forte.", List.of(
                    new GuiaGerado.Dica("Saia cedo", "Vá antes das 11h."),
                    new GuiaGerado.Dica("Leve água", "Beba um copo a cada hora."),
                    new GuiaGerado.Dica("Roupa leve", "Use roupas claras."))));

            mvc.perform(autenticado(post("/api/v1/ia/guia-do-calor"), loginToken("maria@email.com")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.geradoPorIa").value(true))
                    .andExpect(jsonPath("$.resumo").value("Amanhã o calor continua forte."))
                    .andExpect(jsonPath("$.dicas", hasSize(3)))
                    .andExpect(jsonPath("$.refugio.id").value(biblioteca.getId()))
                    .andExpect(jsonPath("$.refugio.endereco").value("Rua Exemplo, 100 — Penha"))
                    .andExpect(jsonPath("$.refugio.data").value(HOJE.plusDays(1).toString()))
                    .andExpect(jsonPath("$.refugio.abre").value("08:00"))
                    .andExpect(jsonPath("$.sinaisDeAlerta", hasSize(4)))
                    .andExpect(jsonPath("$.emergencia").value("Se sentir algum desses sinais, ligue 192 (SAMU)."));
        }

        @Test
        void llmForaDevolveGuiaPadraoSemErro() throws Exception {
            when(iaService.gerarGuia(any())).thenThrow(new RuntimeException("ia-service fora"));

            mvc.perform(autenticado(post("/api/v1/ia/guia-do-calor"), loginToken("maria@email.com"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"data\":\"" + HOJE + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.geradoPorIa").value(false))
                    .andExpect(jsonPath("$.refugio").value(nullValue()))
                    .andExpect(jsonPath("$.dicas[0].titulo").value("Procure um refúgio"))
                    .andExpect(jsonPath("$.emergencia").value("Se sentir algum desses sinais, ligue 192 (SAMU)."));
        }
    }

    // ---------------------------------------------------------------- Apoio

    private String gestor() throws Exception {
        return loginToken("gestor@refugio.com");
    }

    private String loginToken(String email) throws Exception {
        JsonNode corpo = corpo(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"Senha@123\"}")).andExpect(status().isOk()));
        return corpo.get("token").asText();
    }

    private String token(Instant emitidoEm, Instant expiraEm, String papel) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("refugio-api").audience(List.of("refugio-app"))
                .subject("1").issuedAt(emitidoEm).notBefore(emitidoEm).expiresAt(expiraEm).claim("role", papel).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private static MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder req, String token) {
        return req.header("Authorization", "Bearer " + token);
    }

    private JsonNode corpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private static Map<String, Object> refugioValido() {
        Map<String, Object> corpo = new java.util.LinkedHashMap<>();
        corpo.put("nome", "Centro Comunitário Itaquera");
        corpo.put("tipo", "CENTRO_COMUNITARIO");
        corpo.put("cep", "08210-000");
        corpo.put("endereco", "Rua do Bairro, 300");
        corpo.put("bairro", "Itaquera");
        corpo.put("capacidade", 25);
        corpo.put("abre", "09:00");
        corpo.put("fecha", "18:00");
        corpo.put("acessivel", false);
        corpo.put("ativo", true);
        return corpo;
    }

    private static Reserva reserva(Usuario idoso, Refugio refugio, LocalDate data, StatusReserva status) {
        return new Reserva(idoso, refugio, data, status, agoraUtc(), java.util.UUID.randomUUID().toString());
    }

    private static OffsetDateTime agoraUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
