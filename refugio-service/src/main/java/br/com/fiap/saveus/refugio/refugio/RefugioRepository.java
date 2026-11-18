package br.com.fiap.saveus.refugio.refugio;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Locale;

public interface RefugioRepository extends JpaRepository<Refugio, Long>, JpaSpecificationExecutor<Refugio> {

    /** lower(bairro) = lower(:bairro): comparação exata, sem diferenciar maiúsculas. */
    static Specification<Refugio> doBairro(String bairro) {
        return (root, query, cb) -> bairro == null || bairro.isBlank()
                ? null
                : cb.equal(cb.lower(root.get("bairro")), bairro.trim().toLowerCase(Locale.ROOT));
    }

    static Specification<Refugio> comAtivo(Boolean ativo) {
        return (root, query, cb) -> ativo == null ? null : cb.equal(root.get("ativo"), ativo);
    }
}
