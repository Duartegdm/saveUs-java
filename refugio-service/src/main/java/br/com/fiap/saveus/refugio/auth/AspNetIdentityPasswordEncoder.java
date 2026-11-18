package br.com.fiap.saveus.refugio.auth;

import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de senha no formato V3 do PasswordHasher do ASP.NET Core Identity, o mesmo gravado pelo .NET:
 * Base64( 0x01 | PRF (4 bytes, 2 = HMAC-SHA512) | iterações (4 bytes, 100000) | tamanho do sal (4 bytes, 16)
 * | sal | subchave PBKDF2 de 32 bytes ), inteiros em big-endian.
 * Assim os usuários cadastrados pelo .NET logam pelo Java, e vice-versa.
 */
public final class AspNetIdentityPasswordEncoder implements PasswordEncoder {

    private static final byte FORMATO_V3 = 0x01;
    private static final int PRF_HMAC_SHA512 = 2;
    private static final int ITERACOES = 100_000;
    private static final int TAMANHO_SAL = 16;
    private static final int TAMANHO_SUBCHAVE = 32;
    private static final int TAMANHO_CABECALHO = 13;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String encode(CharSequence senha) {
        byte[] sal = new byte[TAMANHO_SAL];
        random.nextBytes(sal);
        byte[] subchave = pbkdf2(senha, sal, ITERACOES, TAMANHO_SUBCHAVE);
        ByteBuffer hash = ByteBuffer.allocate(TAMANHO_CABECALHO + TAMANHO_SAL + TAMANHO_SUBCHAVE);
        hash.put(FORMATO_V3).putInt(PRF_HMAC_SHA512).putInt(ITERACOES).putInt(TAMANHO_SAL).put(sal).put(subchave);
        return Base64.getEncoder().encodeToString(hash.array());
    }

    @Override
    public boolean matches(CharSequence senha, String senhaHash) {
        if (senha == null || senhaHash == null) {
            return false;
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(senhaHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (bytes.length < TAMANHO_CABECALHO || bytes[0] != FORMATO_V3) {
            return false;
        }
        ByteBuffer hash = ByteBuffer.wrap(bytes, 1, bytes.length - 1);
        int prf = hash.getInt();
        int iteracoes = hash.getInt();
        int tamanhoSal = hash.getInt();
        if (prf != PRF_HMAC_SHA512 || iteracoes < 1 || tamanhoSal < 1 || tamanhoSal > hash.remaining()) {
            return false;
        }
        byte[] sal = new byte[tamanhoSal];
        hash.get(sal);
        byte[] esperado = new byte[hash.remaining()];
        hash.get(esperado);
        if (esperado.length == 0) {
            return false;
        }
        return MessageDigest.isEqual(esperado, pbkdf2(senha, sal, iteracoes, esperado.length));
    }

    private static byte[] pbkdf2(CharSequence senha, byte[] sal, int iteracoes, int bytes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(senha.toString().toCharArray(), sal, iteracoes, bytes * 8);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
