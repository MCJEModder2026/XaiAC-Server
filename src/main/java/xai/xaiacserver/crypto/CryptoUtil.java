package xai.xaiacserver.crypto;

import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateNonce() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] generateSessionKey() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        return key;
    }
}
