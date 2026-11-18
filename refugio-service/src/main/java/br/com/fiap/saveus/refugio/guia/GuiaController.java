package br.com.fiap.saveus.refugio.guia;

import br.com.fiap.saveus.refugio.guia.dto.GuiaRequest;
import br.com.fiap.saveus.refugio.guia.dto.GuiaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ia")
@Tag(name = "IA: Guia do Calor", description = "Guia de proteção contra o calor gerado com Spring AI a partir da "
        + "reserva real do idoso. O refugio-service monta o contexto (Usuario + Reserva + Refugio) e chama o ia-service "
        + "via OpenFeign/Eureka, repassando o JWT do idoso.")
public class GuiaController {

    private static final String EXEMPLO_IA = """
            {"resumo": "Amanhã o calor continua forte. Sua vaga na Biblioteca Municipal da Penha está garantida das 9h às 18h.",
             "dicas": [
               {"titulo": "Saia cedo", "descricao": "Vá até o refúgio antes das 11h, quando o sol ainda está mais fraco."},
               {"titulo": "Leve água", "descricao": "Beba um copo a cada hora, mesmo sem sede."},
               {"titulo": "Roupa leve", "descricao": "Use roupas claras e um chapéu no caminho."}],
             "refugio": {"id": 1, "nome": "Biblioteca Municipal da Penha",
                         "endereco": "Rua Padre Benedito de Camargo, 400 — Penha", "data": "2026-11-27",
                         "abre": "09:00", "fecha": "18:00", "acessivel": true},
             "sinaisDeAlerta": ["Tontura ou confusão", "Dor de cabeça forte", "Pele quente e seca", "Náusea ou vômito"],
             "emergencia": "Se sentir algum desses sinais, ligue 192 (SAMU).",
             "geradoPorIa": true}""";

    private static final String EXEMPLO_FALLBACK = """
            {"resumo": "Hoje o calor pede cuidado. Procure um lugar fresco para passar as horas mais quentes.",
             "dicas": [
               {"titulo": "Procure um refúgio", "descricao": "Veja no app os refúgios abertos perto de você e reserve sua vaga."},
               {"titulo": "Leve água", "descricao": "Beba um copo de água a cada hora, mesmo sem sede."},
               {"titulo": "Roupa leve", "descricao": "Use roupas claras e soltas e um chapéu no caminho."},
               {"titulo": "Evite o sol forte", "descricao": "Entre 10h e 16h, fique na sombra ou em lugar fresco."}],
             "refugio": null,
             "sinaisDeAlerta": ["Tontura ou confusão", "Dor de cabeça forte", "Pele quente e seca", "Náusea ou vômito"],
             "emergencia": "Se sentir algum desses sinais, ligue 192 (SAMU).",
             "geradoPorIa": false}""";

    private final GuiaService service;

    public GuiaController(GuiaService service) {
        this.service = service;
    }

    @PostMapping("/guia-do-calor")
    @Operation(summary = "Gera o Guia do Calor do idoso logado (IDOSO)",
            description = """
                    Não é um chat: o idoso não digita nada. O guia usa a próxima reserva ATIVA do idoso (data ≥ `data`, \
                    padrão hoje em São Paulo).

                    - `resumo` e `dicas` (3 a 5) vêm do LLM.
                    - `refugio` vem do banco e é `null` quando não há reserva futura.
                    - `sinaisDeAlerta` e `emergencia` são fixos no código: informação de saúde não depende do modelo.
                    - Se o LLM falhar, passar de 3 minutos ou devolver algo que não converte, a resposta continua 200, \
                    com o guia padrão e `geradoPorIa: false`.
                    - O guia fica em cache por idoso, data e reserva: reabrir a tela não chama o LLM de novo.""")
    @ApiResponse(responseCode = "200", description = "Guia gerado (pela IA ou o padrão)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GuiaResponse.class),
                    examples = {
                            @ExampleObject(name = "comIa", summary = "Gerado pela IA, com reserva", value = EXEMPLO_IA),
                            @ExampleObject(name = "fallback", summary = "LLM fora do ar, sem reserva", value = EXEMPLO_FALLBACK)}))
    public GuiaResponse gerar(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                              @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                                      description = "Opcional. Sem corpo, vale hoje em São Paulo.")
                              @RequestBody(required = false) GuiaRequest request) {
        return service.gerar(Long.valueOf(jwt.getSubject()), request == null ? null : request.data());
    }
}
