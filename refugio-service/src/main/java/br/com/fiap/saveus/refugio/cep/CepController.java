package br.com.fiap.saveus.refugio.cep;

import br.com.fiap.saveus.common.erro.ApiException;
import br.com.fiap.saveus.common.erro.DadosInvalidosException;
import br.com.fiap.saveus.refugio.common.CepFormato;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cep")
@Tag(name = "CEP", description = "Consulta de endereço no ViaCEP (via RestClient), para o formulário do gestor se preencher sozinho")
public class CepController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final ViaCepClient viaCep;

    public CepController(ViaCepClient viaCep) {
        this.viaCep = viaCep;
    }

    @GetMapping("/{cep}")
    @Operation(summary = "Endereço de um CEP (GESTOR)",
            description = "Aceita `03601000` ou `03601-000`. Mapeamento do ViaCEP: `logradouro` → `endereco`, "
                    + "`localidade` → `cidade`.")
    @ApiResponse(responseCode = "200", description = "Endereço encontrado")
    @ApiResponse(responseCode = "400", description = "CEP sem 8 dígitos",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = """
                    {"type": "https://tools.ietf.org/html/rfc9110#section-15.5.1", "title": "Dados inválidos", "status": 400,
                     "errors": {"cep": ["O CEP deve ter 8 dígitos, como 03601-000."]}}""")))
    @ApiResponse(responseCode = "404", description = "CEP não existe",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = """
                    {"type": "https://tools.ietf.org/html/rfc9110#section-15.5.5", "title": "CEP não encontrado",
                     "status": 404, "detail": "Não encontramos o CEP 99999-999.", "instance": "/api/v1/cep/99999999"}""")))
    @ApiResponse(responseCode = "503", description = "ViaCEP fora do ar ou sem resposta",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = """
                    {"type": "https://tools.ietf.org/html/rfc9110#section-15.6.4", "title": "Serviço indisponível",
                     "status": 503, "detail": "Não foi possível consultar o CEP agora. Tente de novo em instantes.",
                     "instance": "/api/v1/cep/03601000"}""")))
    public CepResponse consultar(@Parameter(description = "CEP com 8 dígitos, com ou sem hífen", example = "01001-000")
                                 @PathVariable("cep") String cep) {
        String formatado = CepFormato.normalizar(cep)
                .orElseThrow(() -> new DadosInvalidosException("cep", CepFormato.MENSAGEM));
        return viaCep.consultar(CepFormato.somenteDigitos(formatado))
                .orElseThrow(() -> ApiException.naoEncontrado("CEP não encontrado", "Não encontramos o CEP " + formatado + "."));
    }
}
