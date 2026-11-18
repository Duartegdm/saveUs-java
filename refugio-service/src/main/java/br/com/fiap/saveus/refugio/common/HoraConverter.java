package br.com.fiap.saveus.refugio.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** ABRE e FECHA são VARCHAR2(5) "08:00" no banco do .NET. */
@Converter
public class HoraConverter implements AttributeConverter<LocalTime, String> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public String convertToDatabaseColumn(LocalTime hora) {
        return hora == null ? null : hora.format(FORMATO);
    }

    @Override
    public LocalTime convertToEntityAttribute(String texto) {
        return texto == null ? null : LocalTime.parse(texto.trim(), FORMATO);
    }
}
