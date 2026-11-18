package br.com.fiap.saveus.refugio.cep;

import br.com.fiap.saveus.common.erro.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

/** Cliente do ViaCEP (https://viacep.com.br) com RestClient. */
@Component
public class ViaCepClient {

    private static final Logger log = LoggerFactory.getLogger(ViaCepClient.class);

    private final RestClient restClient;

    public ViaCepClient(RestClient.Builder builder,
                        @Value("${saveus.viacep.url:https://viacep.com.br/ws/}") String url,
                        @Value("${saveus.viacep.timeout:5s}") Duration timeout) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(timeout);
        fabrica.setReadTimeout(timeout);
        this.restClient = builder.baseUrl(url).requestFactory(fabrica).build();
    }

    /** @param cepSomenteDigitos 8 dígitos, já validados. Vazio quando o CEP não existe. */
    public Optional<CepResponse> consultar(String cepSomenteDigitos) {
        JsonNode resposta;
        try {
            resposta = restClient.get().uri("{cep}/json/", cepSomenteDigitos).retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            log.warn("Falha ao consultar o ViaCEP para {}: {}", cepSomenteDigitos, e.getMessage());
            throw ApiException.servicoIndisponivel("Não foi possível consultar o CEP agora. Tente de novo em instantes.");
        }

        // CEP inexistente: o ViaCEP responde 200 com { "erro": true } (ou "true" como texto).
        if (resposta == null || !resposta.isObject() || resposta.path("erro").asBoolean(false)) {
            return Optional.empty();
        }
        return Optional.of(new CepResponse(
                texto(resposta, "cep"),
                texto(resposta, "logradouro"),
                texto(resposta, "bairro"),
                texto(resposta, "localidade"),
                texto(resposta, "uf")));
    }

    private static String texto(JsonNode no, String campo) {
        return no.path(campo).asText("");
    }
}
