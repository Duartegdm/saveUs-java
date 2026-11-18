package br.com.fiap.saveus.refugio.common;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** "Hoje" é sempre o dia em São Paulo, nunca o fuso do servidor (a VM pode estar em UTC). */
public final class FusoHorario {

    public static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private FusoHorario() {
    }

    public static LocalDate hoje(Clock relogio) {
        return LocalDate.now(relogio.withZone(SAO_PAULO));
    }

    /** Agora em segundos inteiros (o JWT trabalha em segundos). */
    public static Instant agora(Clock relogio) {
        return relogio.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    public static OffsetDateTime emSaoPaulo(Instant instante) {
        return instante.atZone(SAO_PAULO).toOffsetDateTime();
    }
}
