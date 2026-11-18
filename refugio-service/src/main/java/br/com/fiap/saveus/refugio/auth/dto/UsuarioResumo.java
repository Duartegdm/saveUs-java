package br.com.fiap.saveus.refugio.auth.dto;

import br.com.fiap.saveus.refugio.usuario.Papel;
import io.swagger.v3.oas.annotations.media.Schema;
import br.com.fiap.saveus.refugio.usuario.Usuario;

@Schema(description = "Dados básicos do usuário")
public record UsuarioResumo(
        @Schema(example = "1") Long id,
        @Schema(example = "Gestora Refúgio") String nome,
        @Schema(example = "gestor@refugio.com") String email,
        @Schema(description = "IDOSO ou GESTOR", example = "GESTOR") Papel papel) {

    public static UsuarioResumo de(Usuario u) {
        return new UsuarioResumo(u.getId(), u.getNome(), u.getEmail(), u.getPapel());
    }
}
