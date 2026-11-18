package br.com.fiap.saveus.refugio.refugio;

import br.com.fiap.saveus.refugio.common.PaginaResponse;
import br.com.fiap.saveus.refugio.refugio.dto.RefugioRequest;
import br.com.fiap.saveus.refugio.refugio.dto.RefugioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;

/** Leitura para qualquer usuário logado; escrita só para GESTOR (regras em SegurancaConfig). */
@RestController
@RequestMapping("/api/v1/refugios")
@Tag(name = "Refúgios", description = "Refúgios climáticos. Leitura para qualquer usuário logado (IDOSO ou GESTOR); "
        + "criar, editar, pausar e excluir só para GESTOR. As vagas são calculadas a partir das reservas ATIVA do dia.")
public class RefugioController {

    static final String PROBLEM_JSON = "application/problem+json";

    static final String EXEMPLO_404 = """
            {"type": "https://tools.ietf.org/html/rfc9110#section-15.5.5", "title": "Refúgio não encontrado",
             "status": 404, "detail": "Este refúgio não está mais disponível.", "instance": "/api/v1/refugios/99"}""";

    static final String EXEMPLO_409 = """
            {"type": "https://tools.ietf.org/html/rfc9110#section-15.5.10", "title": "Refúgio com reservas",
             "status": 409, "detail": "Este refúgio tem reservas ativas para hoje ou datas futuras. Desative-o em vez de excluir.",
             "instance": "/api/v1/refugios/1"}""";

    static final String EXEMPLO_400_LISTA = """
            {"type": "https://tools.ietf.org/html/rfc9110#section-15.5.1", "title": "Dados inválidos", "status": 400,
             "errors": {"data": ["Data em formato inválido. Use yyyy-MM-dd, como 2026-11-15."],
                        "tamanho": ["O tamanho da página deve ficar entre 1 e 50."]}}""";

    static final String EXEMPLO_400_FORM = """
            {"type": "https://tools.ietf.org/html/rfc9110#section-15.5.1", "title": "Dados inválidos", "status": 400,
             "errors": {"cep": ["O CEP deve ter 8 dígitos, como 03601-000."],
                        "capacidade": ["A capacidade deve ser maior que zero."],
                        "fecha": ["O horário de fechamento deve ser depois da abertura."]}}""";

    private final RefugioService service;

    public RefugioController(RefugioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista refúgios, paginada, com filtro por bairro e data",
            description = "Devolve o envelope `{itens, pagina, tamanho, totalItens, totalPaginas}` (a página começa em 1). "
                    + "Ordenação: nome crescente, depois id. Sem `data`, as vagas são as de hoje no fuso de São Paulo. "
                    + "Refúgio inativo aparece com 0 vagas.")
    @ApiResponse(responseCode = "200", description = "Página de refúgios")
    @ApiResponse(responseCode = "400", description = "Parâmetro em formato errado",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = EXEMPLO_400_LISTA)))
    public PaginaResponse<RefugioResponse> listar(
            @Parameter(description = "Bairro exato, sem diferenciar maiúsculas (até 100 caracteres)", example = "Penha")
            @RequestParam(name = "bairro", required = false)
            @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres.") String bairro,
            @Parameter(description = "Data para calcular as vagas, `yyyy-MM-dd`. Padrão: hoje em São Paulo",
                    example = "2026-11-27")
            @RequestParam(name = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @Parameter(description = "`true`: só ativos; `false`: só pausados; ausente: todos (a lista do gestor usa sem filtro)")
            @RequestParam(name = "ativo", required = false) Boolean ativo,
            @Parameter(description = "Número da página, a partir de 1", example = "1")
            @RequestParam(name = "pagina", defaultValue = "1") @Min(value = 1, message = "A página começa em 1.") int pagina,
            @Parameter(description = "Itens por página, de 1 a 50", example = "10")
            @RequestParam(name = "tamanho", defaultValue = "10")
            @Min(value = 1, message = "O tamanho da página deve ficar entre 1 e 50.")
            @Max(value = 50, message = "O tamanho da página deve ficar entre 1 e 50.") int tamanho) {
        return service.listar(bairro, ativo, data, pagina, tamanho);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um refúgio, com as vagas da data informada",
            description = "Usado pela tela de edição do gestor.")
    @ApiResponse(responseCode = "200", description = "Refúgio encontrado")
    @ApiResponse(responseCode = "404", description = "Refúgio não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = EXEMPLO_404)))
    public RefugioResponse obter(
            @Parameter(description = "Id do refúgio", example = "1") @PathVariable("id") Long id,
            @Parameter(description = "Data para calcular as vagas, `yyyy-MM-dd`. Padrão: hoje em São Paulo",
                    example = "2026-11-27")
            @RequestParam(name = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.obter(id, data);
    }

    @PostMapping
    @Operation(summary = "Cadastra um refúgio (GESTOR)",
            description = "Todos os campos são obrigatórios. Nome, endereço e bairro são gravados sem espaços nas pontas "
                    + "e o CEP é normalizado para `00000-000`. O novo refúgio começa com vagas = capacidade "
                    + "(ou 0, se inativo).")
    @ApiResponse(responseCode = "201", description = "Refúgio criado",
            headers = @Header(name = "Location", description = "/api/v1/refugios/{id}"))
    @ApiResponse(responseCode = "400", description = "Campos inválidos (errors por campo)",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = EXEMPLO_400_FORM)))
    public ResponseEntity<RefugioResponse> criar(@Valid @RequestBody RefugioRequest request) {
        RefugioResponse criado = service.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/refugios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um refúgio inteiro: editar, pausar ou reativar (GESTOR)",
            description = "O corpo é o mesmo do POST e substitui o refúgio inteiro. Pausar ou reativar é um PUT com "
                    + "`ativo` trocado; não existe rota separada. Responde com as vagas de hoje.")
    @ApiResponse(responseCode = "200", description = "Refúgio atualizado")
    @ApiResponse(responseCode = "400", description = "Campos inválidos (errors por campo)",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = EXEMPLO_400_FORM)))
    @ApiResponse(responseCode = "404", description = "Refúgio não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = EXEMPLO_404)))
    public RefugioResponse atualizar(@Parameter(description = "Id do refúgio", example = "1") @PathVariable("id") Long id,
                                     @Valid @RequestBody RefugioRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um refúgio (GESTOR)",
            description = "Só exclui se não houver reserva ATIVA para hoje ou depois (São Paulo); nesse caso, desative "
                    + "o refúgio com um PUT. As reservas passadas saem junto pelo ON DELETE CASCADE do banco.")
    @ApiResponse(responseCode = "204", description = "Excluído, sem corpo")
    @ApiResponse(responseCode = "404", description = "Refúgio não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = EXEMPLO_404)))
    @ApiResponse(responseCode = "409", description = "Refúgio com reservas ativas: desative em vez de excluir",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = EXEMPLO_409)))
    public ResponseEntity<Void> excluir(@Parameter(description = "Id do refúgio", example = "1") @PathVariable("id") Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
