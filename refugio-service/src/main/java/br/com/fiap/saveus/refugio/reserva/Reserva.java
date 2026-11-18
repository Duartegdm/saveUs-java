package br.com.fiap.saveus.refugio.reserva;

import br.com.fiap.saveus.refugio.refugio.Refugio;
import br.com.fiap.saveus.refugio.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * T_RFG_RESERVA: uma reserva pertence a um Usuario (o idoso) e a um Refugio.
 * O Java só lê (vagas, bloqueio de exclusão e IA); criar e cancelar é do .NET, por isso @Immutable.
 */
@Entity
@Immutable
@Table(name = "T_RFG_RESERVA")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDOSO_ID", nullable = false)
    private Usuario idoso;

    /** A FK tem ON DELETE CASCADE no banco: excluir o refúgio leva as reservas passadas junto. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "REFUGIO_ID", nullable = false)
    private Refugio refugio;

    @Column(name = "DATA_RESERVA", nullable = false)
    private LocalDate dataReserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private StatusReserva status;

    @Column(name = "CRIADO_EM", nullable = false)
    private OffsetDateTime criadoEm;

    /** Não usado pelo Java; existe para o mapeamento ficar completo. */
    @Column(name = "CHAVE_IDEMPOTENCIA", nullable = false, length = 100)
    private String chaveIdempotencia;

    protected Reserva() {
        // JPA
    }

    /** Usado só em testes: no sistema real quem cria reservas é o .NET. */
    public Reserva(Usuario idoso, Refugio refugio, LocalDate dataReserva, StatusReserva status,
                   OffsetDateTime criadoEm, String chaveIdempotencia) {
        this.idoso = idoso;
        this.refugio = refugio;
        this.dataReserva = dataReserva;
        this.status = status;
        this.criadoEm = criadoEm;
        this.chaveIdempotencia = chaveIdempotencia;
    }

    public Long getId() { return id; }
    public Usuario getIdoso() { return idoso; }
    public Refugio getRefugio() { return refugio; }
    public LocalDate getDataReserva() { return dataReserva; }
    public StatusReserva getStatus() { return status; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
}
