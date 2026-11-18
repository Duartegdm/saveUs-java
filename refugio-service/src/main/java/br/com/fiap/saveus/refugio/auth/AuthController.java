package br.com.fiap.saveus.refugio.auth;

import br.com.fiap.saveus.refugio.auth.dto.LoginRequest;
import br.com.fiap.saveus.refugio.auth.dto.LoginResponse;
import br.com.fiap.saveus.refugio.auth.dto.RegistrarRequest;
import br.com.fiap.saveus.refugio.auth.dto.UsuarioResumo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Rotas públicas em /auth (sem /api/v1), como no .NET. */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login e cadastro, públicos. O JWT emitido aqui vale também no backend .NET "
        + "(mesma chave HS256, iss `refugio-api`, aud `refugio-app`, claims `sub`, `email`, `name` e `role`). "
        + "Limite de 10 tentativas por minuto por IP.")
@SecurityRequirements
public class AuthController {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String EXEMPLO_429 = """
            {"type": "https://tools.ietf.org/html/rfc6585#section-4", "title": "Muitas requisições", "status": 429,
             "detail": "Você fez muitas tentativas seguidas. Tente de novo em 47 segundos.", "instance": "/auth/login"}""";

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e devolve o JWT e os dados do usuário",
            description = "O e-mail é buscado com trim e em minúsculas. O token vale 120 minutos e não há refresh token: "
                    + "expirado, o app volta para o login. Usuários de teste: `gestor@refugio.com`, `maria@email.com`, "
                    + "`jose@email.com`, todos com a senha `Senha@123`.")
    @ApiResponse(responseCode = "200", description = "Autenticado")
    @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos (mesma mensagem nos dois casos)",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = """
                    {"type": "https://tools.ietf.org/html/rfc9110#section-15.5.2", "title": "Credenciais inválidas",
                     "status": 401, "detail": "E-mail ou senha incorretos.", "instance": "/auth/login"}""")))
    @ApiResponse(responseCode = "429", description = "Muitas tentativas",
            headers = @Header(name = "Retry-After", description = "Segundos até poder tentar de novo"),
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = EXEMPLO_429)))
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/registrar")
    @Operation(summary = "Cria a conta de um idoso",
            description = "O cadastro cria sempre o papel IDOSO; contas de gestor vêm do script do banco. "
                    + "Não devolve token: faça login em seguida. Senha de 8 a 100 caracteres, com maiúscula, minúscula e número.")
    @ApiResponse(responseCode = "201", description = "Conta criada",
            headers = @Header(name = "Location", description = "/api/v1/usuarios/me (perfil, no backend .NET)"))
    @ApiResponse(responseCode = "400", description = "Campos inválidos",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = """
                    {"type": "https://tools.ietf.org/html/rfc9110#section-15.5.1", "title": "Dados inválidos", "status": 400,
                     "errors": {"email": ["Informe um e-mail válido."], "senha": ["A senha deve ter uma letra maiúscula."]}}""")))
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = """
                    {"type": "https://tools.ietf.org/html/rfc9110#section-15.5.10", "title": "E-mail já cadastrado",
                     "status": 409, "detail": "Já existe uma conta com este e-mail. Faça login.", "instance": "/auth/registrar"}""")))
    @ApiResponse(responseCode = "429", description = "Muitas tentativas",
            headers = @Header(name = "Retry-After", description = "Segundos até poder tentar de novo"),
            content = @Content(mediaType = PROBLEM_JSON, examples = @ExampleObject(value = EXEMPLO_429)))
    public ResponseEntity<UsuarioResumo> registrar(@Valid @RequestBody RegistrarRequest request) {
        return ResponseEntity.created(URI.create("/api/v1/usuarios/me")).body(service.registrar(request));
    }
}
