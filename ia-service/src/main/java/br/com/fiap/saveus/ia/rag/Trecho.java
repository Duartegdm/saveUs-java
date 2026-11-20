package br.com.fiap.saveus.ia.rag;

/** Um trecho da base de conhecimento, recuperado para compor o prompt. */
public record Trecho(String id, String titulo, String texto) {
}
