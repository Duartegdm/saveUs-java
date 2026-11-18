package br.com.fiap.saveus.ia;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sobe o ia-service sem LLM e sem Eureka: confere a segurança do endpoint interno e o documento OpenAPI. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentacaoTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void endpointInternoExigeToken() throws Exception {
        mvc.perform(post("/interno/guia-do-calor").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Faça login para continuar."));
    }

    @Test
    void documentaOEndpointInterno() throws Exception {
        String corpo = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode doc = json.readTree(corpo);

        JsonNode respostas = doc.at("/paths/~1interno~1guia-do-calor/post/responses");
        assertThat(respostas.fieldNames()).toIterable().contains("200", "400", "401", "403", "503", "500");
        assertThat(doc.at("/components/schemas/ProblemDetails").isMissingNode()).isFalse();

        Path destino = Path.of("build/openapi/ia-service.json");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, json.writerWithDefaultPrettyPrinter().writeValueAsString(doc));
    }
}
