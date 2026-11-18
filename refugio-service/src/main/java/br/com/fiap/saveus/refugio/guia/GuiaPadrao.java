package br.com.fiap.saveus.refugio.guia;

import br.com.fiap.saveus.refugio.guia.dto.ContextoGuia;
import br.com.fiap.saveus.refugio.guia.dto.GuiaGerado;
import br.com.fiap.saveus.refugio.guia.dto.GuiaGerado.Dica;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Conteúdo fixo do guia. sinaisDeAlerta e emergencia nunca vêm do LLM; o guia padrão é usado
 * quando o LLM falha, demora ou devolve algo que não converte (geradoPorIa = false).
 */
public final class GuiaPadrao {

    public static final List<String> SINAIS_DE_ALERTA = List.of(
            "Tontura ou confusão",
            "Dor de cabeça forte",
            "Pele quente e seca",
            "Náusea ou vômito");

    public static final String EMERGENCIA = "Se sentir algum desses sinais, ligue 192 (SAMU).";

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");

    private GuiaPadrao() {
    }

    public static GuiaGerado para(ContextoGuia contexto, LocalDate hoje) {
        ContextoGuia.RefugioContexto refugio = contexto.refugio();
        String quando = quando(contexto.data(), hoje);
        List<Dica> dicas = new ArrayList<>();
        String resumo;

        if (refugio != null) {
            resumo = "%s o calor pede cuidado. Sua vaga em %s está garantida das %s às %s."
                    .formatted(quando, refugio.nome(), refugio.abre().format(HORA), refugio.fecha().format(HORA));
            dicas.add(new Dica("Saia cedo",
                    "Vá até o refúgio logo que ele abrir, às %s, antes do sol mais forte.".formatted(refugio.abre().format(HORA))));
        } else {
            resumo = "%s o calor pede cuidado. Procure um lugar fresco para passar as horas mais quentes.".formatted(quando);
            dicas.add(new Dica("Procure um refúgio",
                    "Veja no app os refúgios abertos perto de você e reserve sua vaga."));
        }
        dicas.add(new Dica("Leve água", "Beba um copo de água a cada hora, mesmo sem sede."));
        dicas.add(new Dica("Roupa leve", "Use roupas claras e soltas e um chapéu no caminho."));
        dicas.add(new Dica("Evite o sol forte", "Entre 10h e 16h, fique na sombra ou em lugar fresco."));
        return new GuiaGerado(resumo, dicas);
    }

    private static String quando(LocalDate data, LocalDate hoje) {
        if (data == null || data.equals(hoje)) return "Hoje";
        if (data.equals(hoje.plusDays(1))) return "Amanhã";
        return "No dia " + data.format(DIA);
    }
}
