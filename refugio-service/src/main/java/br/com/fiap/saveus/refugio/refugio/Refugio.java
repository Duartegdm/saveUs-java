package br.com.fiap.saveus.refugio.refugio;

import br.com.fiap.saveus.refugio.common.HoraConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.type.NumericBooleanConverter;

import java.time.LocalTime;

/** T_RFG_REFUGIO, a tabela do CRUD do gestor. O schema é do .NET: o Java não cria nem altera tabelas. */
@Entity
@Table(name = "T_RFG_REFUGIO")
public class Refugio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 30)
    private TipoRefugio tipo;

    /** Sempre gravado como 03601-000. */
    @Column(name = "CEP", nullable = false, length = 9)
    private String cep;

    @Column(name = "ENDERECO", nullable = false, length = 200)
    private String endereco;

    @Column(name = "BAIRRO", nullable = false, length = 100)
    private String bairro;

    @Column(name = "CAPACIDADE", nullable = false)
    private Integer capacidade;

    @Convert(converter = HoraConverter.class)
    @Column(name = "ABRE", nullable = false, length = 5)
    private LocalTime abre;

    @Convert(converter = HoraConverter.class)
    @Column(name = "FECHA", nullable = false, length = 5)
    private LocalTime fecha;

    /** NUMBER(1): 1 = true, 0 = false. */
    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "ACESSIVEL", nullable = false)
    private Boolean acessivel;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "ATIVO", nullable = false)
    private Boolean ativo;

    protected Refugio() {
        // JPA
    }

    public Refugio(String nome, TipoRefugio tipo, String cep, String endereco, String bairro, int capacidade,
                   LocalTime abre, LocalTime fecha, boolean acessivel, boolean ativo) {
        atualizar(nome, tipo, cep, endereco, bairro, capacidade, abre, fecha, acessivel, ativo);
    }

    /** O PUT substitui o refúgio inteiro; pausar/reativar é só trocar o ativo. */
    public void atualizar(String nome, TipoRefugio tipo, String cep, String endereco, String bairro, int capacidade,
                          LocalTime abre, LocalTime fecha, boolean acessivel, boolean ativo) {
        this.nome = nome.trim();
        this.tipo = tipo;
        this.cep = cep;
        this.endereco = endereco.trim();
        this.bairro = bairro.trim();
        this.capacidade = capacidade;
        this.abre = abre;
        this.fecha = fecha;
        this.acessivel = acessivel;
        this.ativo = ativo;
    }

    /** Refúgio inativo não recebe ninguém, então tem 0 vagas. */
    public int vagasDisponiveis(long vagasOcupadas) {
        return Boolean.TRUE.equals(ativo) ? (int) Math.max(0, capacidade - vagasOcupadas) : 0;
    }

    /** "Rua Exemplo, 100 — Penha", como no .NET. */
    public String enderecoCompleto() {
        return endereco + " — " + bairro;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public TipoRefugio getTipo() { return tipo; }
    public String getCep() { return cep; }
    public String getEndereco() { return endereco; }
    public String getBairro() { return bairro; }
    public Integer getCapacidade() { return capacidade; }
    public LocalTime getAbre() { return abre; }
    public LocalTime getFecha() { return fecha; }
    public Boolean getAcessivel() { return acessivel; }
    public Boolean getAtivo() { return ativo; }
}
