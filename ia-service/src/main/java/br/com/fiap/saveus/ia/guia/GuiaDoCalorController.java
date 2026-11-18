package br.com.fiap.saveus.ia.guia;

import br.com.fiap.saveus.ia.guia.dto.ContextoGuia;
import br.com.fiap.saveus.ia.guia.dto.GuiaGerado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint interno, chamado só pelo refugio-service via OpenFeign (o app não chama este serviço). */
@RestController
@RequestMapping("/interno")
@Tag(name = "Interno: Guia do Calor", description = "Não é chamado pelo app. O refugio-service chama via OpenFeign "
        + "(@FeignClient(name = \"ia-service\"), resolvido pelo Eureka) e repassa o JWT do idoso no header Authorization.")
public class GuiaDoCalorController {

    private final GuiaDoCalorService service;

    public GuiaDoCalorController(GuiaDoCalorService service) {
        this.service = service;
    }

    @PostMapping("/guia-do-calor")
    @Operation(summary = "Gera resumo e dicas do Guia do Calor com o LLM (IDOSO)",
            description = "Usa `chatClient.prompt().system(...).user(contexto).call().entity(GuiaGerado.class)`: o Spring AI "
                    + "envia o JSON Schema ao modelo e converte a resposta. A saída é conferida (resumo e 3 a 5 dicas). "
                    + "O prompt de sistema proíbe inventar endereços, horários, temperaturas ou telefones, diagnóstico e remédios.")
    @ApiResponse(responseCode = "200", description = "Resumo e dicas gerados")
    @ApiResponse(responseCode = "503", description = "LLM indisponível ou resposta que não converte (o refugio-service usa o guia padrão)",
            content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = """
                    {"type": "https://tools.ietf.org/html/rfc9110#section-15.6.4", "title": "Serviço indisponível",
                     "status": 503, "detail": "Não foi possível gerar o guia agora. Tente de novo em instantes.",
                     "instance": "/interno/guia-do-calor"}""")))
    public GuiaGerado gerar(@Valid @RequestBody ContextoGuia contexto) {
        return service.gerar(contexto);
    }
}
