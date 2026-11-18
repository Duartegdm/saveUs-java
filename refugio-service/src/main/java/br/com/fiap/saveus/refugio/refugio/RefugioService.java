package br.com.fiap.saveus.refugio.refugio;

import br.com.fiap.saveus.common.erro.ApiException;
import br.com.fiap.saveus.refugio.common.CepFormato;
import br.com.fiap.saveus.refugio.common.FusoHorario;
import br.com.fiap.saveus.refugio.common.PaginaResponse;
import br.com.fiap.saveus.refugio.refugio.dto.RefugioRequest;
import br.com.fiap.saveus.refugio.refugio.dto.RefugioResponse;
import br.com.fiap.saveus.refugio.reserva.ReservaRepository;
import br.com.fiap.saveus.refugio.reserva.StatusReserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RefugioService {

    private static final Logger log = LoggerFactory.getLogger(RefugioService.class);
    private static final Sort ORDENACAO = Sort.by(Sort.Order.asc("nome"), Sort.Order.asc("id"));

    private final RefugioRepository refugios;
    private final ReservaRepository reservas;
    private final Clock relogio;

    public RefugioService(RefugioRepository refugios, ReservaRepository reservas, Clock relogio) {
        this.refugios = refugios;
        this.reservas = reservas;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<RefugioResponse> listar(String bairro, Boolean ativo, LocalDate data, int pagina, int tamanho) {
        LocalDate dia = data != null ? data : FusoHorario.hoje(relogio);
        Specification<Refugio> filtro = Specification.allOf(RefugioRepository.doBairro(bairro), RefugioRepository.comAtivo(ativo));

        // O Spring Data começa a página em 0; o contrato com o app, em 1.
        Page<Refugio> resultado = refugios.findAll(filtro, PageRequest.of(pagina - 1, tamanho, ORDENACAO));

        Map<Long, Long> ocupadas = ocupacao(resultado.getContent(), dia);
        return PaginaResponse.de(resultado, r -> RefugioResponse.de(r, ocupadas.getOrDefault(r.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public RefugioResponse obter(Long id, LocalDate data) {
        Refugio refugio = buscar(id);
        LocalDate dia = data != null ? data : FusoHorario.hoje(relogio);
        return RefugioResponse.de(refugio, reservas.countByRefugioIdAndDataReservaAndStatus(id, dia, StatusReserva.ATIVA));
    }

    @Transactional
    public RefugioResponse criar(RefugioRequest request) {
        Refugio refugio = new Refugio(request.nome(), request.tipo(), cepNormalizado(request), request.endereco(),
                request.bairro(), request.capacidade(), request.abre(), request.fecha(), request.acessivel(), request.ativo());
        refugios.save(refugio);
        log.info("Refúgio {} criado: {}", refugio.getId(), refugio.getNome());
        // Recém-criado não tem reservas: vagas = capacidade se ativo, 0 se inativo.
        return RefugioResponse.de(refugio, 0);
    }

    @Transactional
    public RefugioResponse atualizar(Long id, RefugioRequest request) {
        Refugio refugio = buscar(id);
        refugio.atualizar(request.nome(), request.tipo(), cepNormalizado(request), request.endereco(), request.bairro(),
                request.capacidade(), request.abre(), request.fecha(), request.acessivel(), request.ativo());
        refugios.saveAndFlush(refugio);
        long ocupadas = reservas.countByRefugioIdAndDataReservaAndStatus(id, FusoHorario.hoje(relogio), StatusReserva.ATIVA);
        log.info("Refúgio {} atualizado (ativo={})", id, refugio.getAtivo());
        return RefugioResponse.de(refugio, ocupadas);
    }

    @Transactional
    public void excluir(Long id) {
        Refugio refugio = buscar(id);
        // Excluir apagaria reservas que idosos ainda vão usar: nesse caso o gestor deve desativar o refúgio.
        if (reservas.existsByRefugioIdAndDataReservaGreaterThanEqualAndStatus(id, FusoHorario.hoje(relogio), StatusReserva.ATIVA)) {
            throw ApiException.conflito("Refúgio com reservas",
                    "Este refúgio tem reservas ativas para hoje ou datas futuras. Desative-o em vez de excluir.");
        }
        // As reservas passadas saem junto pelo ON DELETE CASCADE da FK.
        refugios.delete(refugio);
        log.info("Refúgio {} excluído", id);
    }

    private Refugio buscar(Long id) {
        return refugios.findById(id).orElseThrow(() ->
                ApiException.naoEncontrado("Refúgio não encontrado", "Este refúgio não está mais disponível."));
    }

    private Map<Long, Long> ocupacao(List<Refugio> itens, LocalDate dia) {
        if (itens.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = itens.stream().map(Refugio::getId).toList();
        return reservas.contarPorRefugios(ids, dia, StatusReserva.ATIVA).stream()
                .collect(Collectors.toMap(ReservaRepository.OcupacaoPorRefugio::getRefugioId,
                        ReservaRepository.OcupacaoPorRefugio::getTotal));
    }

    /** O @CepValido já garantiu o formato; aqui só normaliza para 00000-000. */
    private static String cepNormalizado(RefugioRequest request) {
        return CepFormato.normalizar(request.cep()).orElseThrow();
    }
}
