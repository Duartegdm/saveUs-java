package br.com.fiap.saveus.refugio.guia;

import br.com.fiap.saveus.common.erro.ApiException;
import br.com.fiap.saveus.refugio.common.FusoHorario;
import br.com.fiap.saveus.refugio.guia.dto.ContextoGuia;
import br.com.fiap.saveus.refugio.guia.dto.GuiaGerado;
import br.com.fiap.saveus.refugio.guia.dto.GuiaResponse;
import br.com.fiap.saveus.refugio.refugio.Refugio;
import br.com.fiap.saveus.refugio.reserva.Reserva;
import br.com.fiap.saveus.refugio.reserva.ReservaRepository;
import br.com.fiap.saveus.refugio.reserva.StatusReserva;
import br.com.fiap.saveus.refugio.usuario.Usuario;
import br.com.fiap.saveus.refugio.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Guia do Calor personalizado: junta o idoso (Usuario), a próxima reserva ATIVA (Reserva) e o refúgio (Refugio),
 * pede ao ia-service o resumo e as dicas, e completa com os campos fixos de saúde e emergência.
 */
@Service
public class GuiaService {

    private static final Logger log = LoggerFactory.getLogger(GuiaService.class);

    private final UsuarioRepository usuarios;
    private final ReservaRepository reservas;
    private final GuiaIaGateway ia;
    private final Clock relogio;

    public GuiaService(UsuarioRepository usuarios, ReservaRepository reservas, GuiaIaGateway ia, Clock relogio) {
        this.usuarios = usuarios;
        this.reservas = reservas;
        this.ia = ia;
        this.relogio = relogio;
    }

    /**
     * Sem @Transactional de propósito: a chamada ao LLM pode levar até 3 minutos e não deve segurar uma conexão do banco.
     * A reserva já vem com o refúgio carregado (@EntityGraph), então não há lazy loading fora da transação.
     */
    public GuiaResponse gerar(Long idosoId, LocalDate dataPedida) {
        LocalDate hoje = FusoHorario.hoje(relogio);
        LocalDate apartirDe = dataPedida != null ? dataPedida : hoje;

        Usuario idoso = usuarios.findById(idosoId).orElseThrow(() ->
                ApiException.naoEncontrado("Usuário não encontrado", "Sua conta não foi encontrada. Faça login novamente."));

        Optional<Reserva> proxima = reservas
                .findByIdosoIdAndDataReservaGreaterThanEqualAndStatusOrderByDataReserva(idosoId, apartirDe, StatusReserva.ATIVA)
                .stream().findFirst();

        ContextoGuia contexto = montarContexto(idoso, proxima, apartirDe);

        GuiaGerado guia;
        boolean geradoPorIa;
        try {
            guia = ia.gerar(contexto);
            geradoPorIa = true;
        } catch (Exception e) {
            // LLM fora, timeout de 3 minutos, ia-service fora do Eureka ou saída que não converte:
            // para o idoso, um guia genérico é melhor que uma tela de erro.
            log.warn("Guia do Calor sem IA para o idoso {}: {}", idosoId, e.getMessage());
            guia = GuiaPadrao.para(contexto, hoje);
            geradoPorIa = false;
        }

        return new GuiaResponse(guia.resumo(), guia.dicas(), proxima.map(GuiaService::refugioDoGuia).orElse(null),
                GuiaPadrao.SINAIS_DE_ALERTA, GuiaPadrao.EMERGENCIA, geradoPorIa);
    }

    private static ContextoGuia montarContexto(Usuario idoso, Optional<Reserva> reserva, LocalDate apartirDe) {
        ContextoGuia.RefugioContexto refugio = reserva.map(Reserva::getRefugio)
                .map(r -> new ContextoGuia.RefugioContexto(r.getNome(), r.enderecoCompleto(), r.getAbre(), r.getFecha(),
                        r.getAcessivel()))
                .orElse(null);
        LocalDate data = reserva.map(Reserva::getDataReserva).orElse(apartirDe);
        String bairro = idoso.getBairro() != null ? idoso.getBairro()
                : reserva.map(r -> r.getRefugio().getBairro()).orElse(null);
        return new ContextoGuia(idoso.primeiroNome(), data, bairro, refugio);
    }

    private static GuiaResponse.RefugioDoGuia refugioDoGuia(Reserva reserva) {
        Refugio r = reserva.getRefugio();
        return new GuiaResponse.RefugioDoGuia(r.getId(), r.getNome(), r.enderecoCompleto(), reserva.getDataReserva(),
                r.getAbre(), r.getFecha(), r.getAcessivel());
    }
}
