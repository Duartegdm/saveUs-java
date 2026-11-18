package br.com.fiap.saveus.ia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Serviço de IA: gera o Guia do Calor com Spring AI. Não é chamado pelo app, só pelo refugio-service (OpenFeign). */
@SpringBootApplication(scanBasePackages = {"br.com.fiap.saveus.ia", "br.com.fiap.saveus.common"})
public class IaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IaServiceApplication.class, args);
    }
}
