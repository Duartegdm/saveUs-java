package br.com.fiap.saveus.refugio.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Locale;

/** T_RFG_USUARIO, criada pelo .NET. O Java lê no login e na IA, e só grava no cadastro. */
@Entity
@Table(name = "T_RFG_USUARIO")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 150)
    private String nome;

    @Column(name = "EMAIL", nullable = false, length = 200, unique = true)
    private String email;

    @Column(name = "SENHA_HASH", nullable = false, length = 200)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAPEL", nullable = false, length = 20)
    private Papel papel;

    @Column(name = "BAIRRO", length = 100)
    private String bairro;

    @Column(name = "TELEFONE", length = 20)
    private String telefone;

    @Column(name = "CONTATO_EMERGENCIA_NOME", length = 150)
    private String contatoEmergenciaNome;

    @Column(name = "CONTATO_EMERGENCIA_TELEFONE", length = 20)
    private String contatoEmergenciaTelefone;

    @Column(name = "CRIADO_EM", nullable = false)
    private OffsetDateTime criadoEm;

    protected Usuario() {
        // JPA
    }

    public Usuario(String nome, String email, String senhaHash, Papel papel, OffsetDateTime criadoEm) {
        this.nome = nome.trim();
        this.email = normalizarEmail(email);
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.criadoEm = criadoEm;
    }

    /** Mesmo critério do .NET: e-mail sempre gravado e buscado com trim e em minúsculas. */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** "Maria Souza" → "Maria", para o guia tratar a pessoa pelo primeiro nome. */
    public String primeiroNome() {
        String[] partes = nome.trim().split("\\s+");
        return partes.length == 0 ? nome : partes[0];
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public Papel getPapel() { return papel; }
    public String getBairro() { return bairro; }
    public String getTelefone() { return telefone; }
    public String getContatoEmergenciaNome() { return contatoEmergenciaNome; }
    public String getContatoEmergenciaTelefone() { return contatoEmergenciaTelefone; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }

    public void setBairro(String bairro) { this.bairro = bairro; }
}
