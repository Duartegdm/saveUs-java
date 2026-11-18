package br.com.fiap.saveus.refugio.cep;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Endereço do CEP, para preencher o formulário do refúgio")
public record CepResponse(
        @Schema(example = "03601-000") String cep,
        @Schema(description = "logradouro do ViaCEP", example = "Rua Padre Benedito de Camargo") String endereco,
        @Schema(example = "Penha") String bairro,
        @Schema(description = "localidade do ViaCEP", example = "São Paulo") String cidade,
        @Schema(example = "SP") String uf) {
}
