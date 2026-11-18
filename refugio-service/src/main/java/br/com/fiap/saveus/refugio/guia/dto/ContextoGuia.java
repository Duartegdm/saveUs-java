package br.com.fiap.saveus.refugio.guia.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Corpo de POST /interno/guia-do-calor no ia-service. É record: serve também de chave do cache,
 * então qualquer mudança na reserva gera um guia novo.
 */
public record ContextoGuia(String nomeIdoso, LocalDate data, String bairro, RefugioContexto refugio) {

    public record RefugioContexto(
            String nome,
            String endereco,
            @JsonFormat(pattern = "HH:mm") LocalTime abre,
            @JsonFormat(pattern = "HH:mm") LocalTime fecha,
            Boolean acessivel) {
    }
}
