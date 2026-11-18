package br.com.fiap.saveus.refugio.auth;

import br.com.fiap.saveus.common.erro.ApiException;
import br.com.fiap.saveus.refugio.auth.dto.LoginRequest;
import br.com.fiap.saveus.refugio.auth.dto.LoginResponse;
import br.com.fiap.saveus.refugio.auth.dto.RegistrarRequest;
import br.com.fiap.saveus.refugio.auth.dto.UsuarioResumo;
import br.com.fiap.saveus.refugio.common.FusoHorario;
import br.com.fiap.saveus.refugio.usuario.Papel;
import br.com.fiap.saveus.refugio.usuario.Usuario;
import br.com.fiap.saveus.refugio.usuario.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneOffset;

@Service
public class AuthService {

    /**
     * Hash fictício (formato V3): sem usuário, a senha é conferida contra ele, para o tempo de resposta
     * ser o mesmo e não revelar quais e-mails existem.
     */
    private static final String HASH_FICTICIO =
            "AQAAAAIAAYagAAAAEJWoSE7CxHY9E9IT4BaCOr481x9z1ONs1Kp3nY9rCsl0tQmrpA9ujFZ7+Xoa8BSMyw==";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final GeradorDeToken gerador;
    private final Clock relogio;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, GeradorDeToken gerador, Clock relogio) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.gerador = gerador;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarios.findByEmail(Usuario.normalizarEmail(request.email())).orElse(null);

        boolean senhaConfere = encoder.matches(request.senha(), usuario != null ? usuario.getSenhaHash() : HASH_FICTICIO);
        if (usuario == null || !senhaConfere) {
            // Mesma mensagem para e-mail inexistente e senha errada.
            throw ApiException.naoAutenticado("Credenciais inválidas", "E-mail ou senha incorretos.");
        }

        GeradorDeToken.TokenGerado token = gerador.gerar(usuario);
        return new LoginResponse(token.token(), FusoHorario.emSaoPaulo(token.expiraEm()), UsuarioResumo.de(usuario));
    }

    @Transactional
    public UsuarioResumo registrar(RegistrarRequest request) {
        String email = Usuario.normalizarEmail(request.email());
        if (usuarios.existsByEmail(email)) {
            throw emailEmUso();
        }
        Usuario usuario = new Usuario(request.nome(), email, encoder.encode(request.senha()), Papel.IDOSO,
                FusoHorario.agora(relogio).atOffset(ZoneOffset.UTC));
        try {
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Dois cadastros simultâneos com o mesmo e-mail: o índice único UX_T_RFG_USUARIO_EMAIL segura o segundo.
            throw emailEmUso();
        }
        return UsuarioResumo.de(usuario);
    }

    private static ApiException emailEmUso() {
        return ApiException.conflito("E-mail já cadastrado", "Já existe uma conta com este e-mail. Faça login.");
    }
}
