package kr.study.ddobak.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 해시: PBKDF2-HMAC-SHA256 (자바 기본 기능, 라이브러리 추가 없음).
 * 저장 형식 "pbkdf2$반복횟수$소금$해시" — 반복 횟수를 함께 저장해서 나중에 횟수를 올려도 옛 해시를 확인할 수 있다.
 * 반복 횟수는 OWASP 권장(60만)을 기본으로, 테스트에서는 빠르게 하려고 줄인다.
 */
@Component
public class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final SecureRandom random = new SecureRandom();
    private final int iterations;

    public PasswordHasher(@Value("${ddobak.auth.pbkdf2-iterations:600000}") int iterations) {
        this.iterations = iterations;
    }

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] key = derive(password, salt, iterations);
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return "pbkdf2$" + iterations + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(key);
    }

    public boolean matches(String password, String stored) {
        if (password == null || stored == null) return false;
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !parts[0].equals("pbkdf2")) return false;
        try {
            int rounds = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, derive(password, salt, rounds)); // 걸린 시간으로 추측하지 못하게 비교
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int rounds) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("비밀번호 해시를 만들 수 없습니다.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
