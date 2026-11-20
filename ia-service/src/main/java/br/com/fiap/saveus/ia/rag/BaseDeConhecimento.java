package br.com.fiap.saveus.ia.rag;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Recuperação do RAG do Guia do Calor: busca, por BM25, os trechos da base de orientações que combinam com o
 * contexto do idoso. A base é pequena e curada, então a busca léxica basta, roda sem chamar outro modelo
 * (sem custo nem latência de embeddings) e dá sempre o mesmo resultado, o que permite testar a recuperação.
 */
@Component
public class BaseDeConhecimento {

    private static final String ARQUIVO = "conhecimento/calor-e-saude.md";
    private static final String PREFIXO_PALAVRAS = "palavras:";
    private static final double K1 = 1.2;
    private static final double B = 0.75;
    private static final Set<String> STOPWORDS = Set.of(
            "a", "o", "as", "os", "de", "da", "do", "das", "dos", "e", "em", "no", "na", "nos", "nas", "um", "uma",
            "para", "por", "com", "que", "se", "ao", "aos", "mais", "muito", "nao", "sua", "seu", "eu", "voce", "esta",
            "estou", "tenho", "pela", "pelo", "como", "ou", "mesmo", "quem", "deve");

    private final List<Trecho> trechos;
    private final List<Map<String, Long>> frequencias;
    private final Map<String, Long> trechosPorTermo;
    private final double tamanhoMedio;

    public BaseDeConhecimento() {
        this(lerArquivo());
    }

    BaseDeConhecimento(String markdown) {
        List<Secao> secoes = separarSecoes(markdown);
        this.trechos = secoes.stream().map(s -> new Trecho(s.id(), s.titulo(), s.texto())).toList();
        this.frequencias = secoes.stream()
                .map(s -> termos(s.titulo() + " " + s.palavras() + " " + s.texto()).stream()
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())))
                .toList();
        this.trechosPorTermo = frequencias.stream()
                .flatMap(f -> f.keySet().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        this.tamanhoMedio = frequencias.stream().mapToLong(BaseDeConhecimento::tamanho).average().orElse(1);
    }

    public List<Trecho> todos() {
        return trechos;
    }

    /** Os {@code quantidade} trechos mais relevantes para a consulta; trechos sem nenhum termo em comum ficam de fora. */
    public List<Trecho> buscar(String consulta, int quantidade) {
        List<String> termosDaConsulta = termos(consulta).stream().distinct().toList();
        double[] notas = IntStream.range(0, trechos.size()).mapToDouble(i -> nota(termosDaConsulta, i)).toArray();
        return IntStream.range(0, trechos.size())
                .filter(i -> notas[i] > 0)
                .boxed()
                .sorted(Comparator.comparingDouble((Integer i) -> notas[i]).reversed().thenComparing(i -> i))
                .limit(quantidade)
                .map(trechos::get)
                .toList();
    }

    private double nota(List<String> termosDaConsulta, int indice) {
        Map<String, Long> frequencia = frequencias.get(indice);
        double tamanhoRelativo = tamanho(frequencia) / tamanhoMedio;
        double total = 0;
        for (String termo : termosDaConsulta) {
            long tf = frequencia.getOrDefault(termo, 0L);
            if (tf == 0) {
                continue;
            }
            long df = trechosPorTermo.get(termo);
            double idf = Math.log(1 + (trechos.size() - df + 0.5) / (df + 0.5));
            total += idf * (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * tamanhoRelativo));
        }
        return total;
    }

    private static long tamanho(Map<String, Long> frequencia) {
        return frequencia.values().stream().mapToLong(Long::longValue).sum();
    }

    /** Minúsculas, sem acentos, sem stopwords e sem o "s" do plural, para "refúgios" casar com "refugio". */
    static List<String> termos(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return Arrays.stream(semAcento.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(t -> t.length() > 1 && !STOPWORDS.contains(t))
                .map(t -> t.length() > 4 && t.endsWith("s") ? t.substring(0, t.length() - 1) : t)
                .toList();
    }

    private record Secao(String id, String titulo, String palavras, String texto) {
    }

    /** Cada seção "## id | título" do arquivo, com a linha opcional "palavras:" logo abaixo. */
    private static List<Secao> separarSecoes(String markdown) {
        List<Secao> secoes = new ArrayList<>();
        for (String bloco : markdown.split("(?m)^## ")) {
            List<String> linhas = bloco.lines().map(String::strip).filter(l -> !l.isEmpty()).toList();
            if (linhas.isEmpty() || !linhas.getFirst().contains("|")) {
                continue;
            }
            String[] cabecalho = linhas.getFirst().split("\\|", 2);
            String palavras = linhas.stream().filter(l -> l.startsWith(PREFIXO_PALAVRAS))
                    .map(l -> l.substring(PREFIXO_PALAVRAS.length())).findFirst().orElse("");
            String texto = linhas.stream().skip(1).filter(l -> !l.startsWith(PREFIXO_PALAVRAS))
                    .collect(Collectors.joining(" "));
            secoes.add(new Secao(cabecalho[0].strip(), cabecalho[1].strip(), palavras, texto));
        }
        return secoes;
    }

    private static String lerArquivo() {
        try (var entrada = new ClassPathResource(ARQUIVO).getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Base de conhecimento não encontrada: " + ARQUIVO, e);
        }
    }
}
