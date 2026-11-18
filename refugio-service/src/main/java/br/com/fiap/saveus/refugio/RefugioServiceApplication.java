package br.com.fiap.saveus.refugio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Serviço de refúgios: login, CRUD de refúgios do gestor, CEP e o Guia do Calor.
 * É a única aplicação Java que o app chama; o ia-service é chamado por aqui via OpenFeign.
 */
@SpringBootApplication(scanBasePackages = {"br.com.fiap.saveus.refugio", "br.com.fiap.saveus.common"})
@EnableFeignClients
@EnableCaching
public class RefugioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RefugioServiceApplication.class, args);
    }
}
