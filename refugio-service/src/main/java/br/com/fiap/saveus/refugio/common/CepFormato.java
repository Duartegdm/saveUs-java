package br.com.fiap.saveus.refugio.common;

import java.util.Optional;

/** Aceita "03601000" ou "03601-000" e normaliza sempre para "03601-000", como o .NET grava. */
public final class CepFormato {

    public static final String MENSAGEM = "O CEP deve ter 8 dígitos, como 03601-000.";

    private CepFormato() {
    }

    public static Optional<String> normalizar(String entrada) {
        String digitos = somenteDigitos(entrada);
        if (digitos.length() != 8) {
            return Optional.empty();
        }
        return Optional.of(digitos.substring(0, 5) + "-" + digitos.substring(5));
    }

    public static boolean ehValido(String entrada) {
        return normalizar(entrada).isPresent();
    }

    /** Só aceita dígitos e um hífen na 6ª posição; "0360-1000" ou letras não são CEP. */
    public static String somenteDigitos(String entrada) {
        String texto = entrada == null ? "" : entrada.trim();
        if (texto.length() == 9 && texto.charAt(5) == '-') {
            texto = texto.substring(0, 5) + texto.substring(6);
        }
        return texto.chars().allMatch(c -> c >= '0' && c <= '9') ? texto : "";
    }
}
