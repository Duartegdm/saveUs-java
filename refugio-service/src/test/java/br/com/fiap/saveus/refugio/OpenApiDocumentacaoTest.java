package br.com.fiap.saveus.refugio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Confere o documento OpenAPI e grava uma cópia em build/openapi/ (copiada para docs/ no repositório). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentacaoTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void documentaTodosOsEndpointsComSegurancaEErros() throws Exception {
        String corpo = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode doc = json.readTree(corpo);
        Path destino = Path.of("build/openapi/refugio-service.json");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, json.writerWithDefaultPrettyPrinter().writeValueAsString(doc));

        assertThat(doc.at("/paths").fieldNames()).toIterable().containsExactlyInAnyOrder(
                "/auth/login", "/auth/registrar", "/api/v1/refugios", "/api/v1/refugios/{id}",
                "/api/v1/cep/{cep}", "/api/v1/ia/guia-do-calor");
        assertThat(doc.at("/components/securitySchemes/Bearer/scheme").asText()).isEqualTo("bearer");
        assertThat(doc.at("/components/schemas/ProblemDetails").isMissingNode()).isFalse();

        // Rota pública: sem cadeado e sem 401/403 de token.
        JsonNode login = doc.at("/paths/~1auth~1login/post");
        assertThat(login.get("security")).hasSize(0);
        assertThat(login.at("/responses/403").isMissingNode()).isTrue();
        assertThat(login.at("/responses/401/content/application~1problem+json/example/title").asText()).isEqualTo("Credenciais inválidas");
        assertThat(login.at("/responses/401/content/application~1problem+json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/ProblemDetails");

        // Rota protegida: 401 e 403 em ProblemDetails, e os erros específicos com exemplo.
        JsonNode excluir = doc.at("/paths/~1api~1v1~1refugios~1{id}/delete/responses");
        assertThat(excluir.fieldNames()).toIterable().contains("204", "401", "403", "404", "409", "500");
        assertThat(excluir.at("/403/content/application~1problem+json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/ProblemDetails");
        assertThat(excluir.at("/409/content/application~1problem+json/example/title").asText()).isEqualTo("Refúgio com reservas");

    }

    @Test
    void swaggerUiEhPublica() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
