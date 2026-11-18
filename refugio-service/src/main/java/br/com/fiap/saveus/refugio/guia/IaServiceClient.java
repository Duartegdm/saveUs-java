package br.com.fiap.saveus.refugio.guia;

import br.com.fiap.saveus.refugio.guia.dto.ContextoGuia;
import br.com.fiap.saveus.refugio.guia.dto.GuiaGerado;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente OpenFeign do ia-service. Usa o nome registrado no Eureka, sem URL fixa:
 * o Spring Cloud LoadBalancer resolve a instância. O token do idoso é repassado pelo FeignConfig.
 */
@FeignClient(name = "ia-service")
public interface IaServiceClient {

    @PostMapping("/interno/guia-do-calor")
    GuiaGerado gerarGuia(@RequestBody ContextoGuia contexto);
}
