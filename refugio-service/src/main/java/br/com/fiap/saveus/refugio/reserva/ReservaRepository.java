package br.com.fiap.saveus.refugio.reserva;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Vagas ocupadas de um refúgio numa data. */
    long countByRefugioIdAndDataReservaAndStatus(Long refugioId, LocalDate data, StatusReserva status);

    /** Bloqueia a exclusão de refúgio com reservas ativas para hoje ou depois. */
    boolean existsByRefugioIdAndDataReservaGreaterThanEqualAndStatus(Long refugioId, LocalDate hoje, StatusReserva status);

    /** Próximas reservas do idoso, já com o refúgio carregado (para o Guia do Calor). */
    @EntityGraph(attributePaths = "refugio")
    List<Reserva> findByIdosoIdAndDataReservaGreaterThanEqualAndStatusOrderByDataReserva(
            Long idosoId, LocalDate hoje, StatusReserva status);

    /** Vagas ocupadas de vários refúgios de uma vez: evita uma consulta por item na listagem paginada. */
    @Query("""
            select r.refugio.id as refugioId, count(r) as total
            from Reserva r
            where r.refugio.id in :ids and r.dataReserva = :data and r.status = :status
            group by r.refugio.id
            """)
    List<OcupacaoPorRefugio> contarPorRefugios(@Param("ids") Collection<Long> ids,
                                               @Param("data") LocalDate data,
                                               @Param("status") StatusReserva status);

    interface OcupacaoPorRefugio {
        Long getRefugioId();

        Long getTotal();
    }
}
