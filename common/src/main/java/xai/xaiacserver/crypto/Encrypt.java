package xai.xaiacserver.crypto;

import com.google.gson.JsonObject;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;

public class Encrypt {

    private static final int GCM_IV_LEN  = 12;
    private static final int GCM_TAG_LEN = 128; // bits

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * ECIES encrypt: generates an ephemeral P-256 keypair, performs ECDH with the
     * client's static public key, derives aesKey = SHA-256(sharedSecret), and
     * encrypts with AES-256-GCM.
     *
     * Returns a JSON string {"epk":"...","iv":"...","ct":"..."} where:
     *   epk = base64(X||Y) of the ephemeral public key (64 bytes)
     *   iv  = base64 of 12-byte GCM nonce
     *   ct  = base64 of ciphertext + 16-byte GCM auth tag
     *
     * @param plainJson         The plaintext JSON to encrypt.
     * @param clientPubKeyBytes 64 raw bytes: client static public key X[32] || Y[32].
     */
    public static String encryptForClient(String plainJson, byte[] clientPubKeyBytes) throws Exception {
        byte[] xBytes = Arrays.copyOfRange(clientPubKeyBytes, 0,  32);
        byte[] yBytes = Arrays.copyOfRange(clientPubKeyBytes, 32, 64);

        AlgorithmParameters ecParams = AlgorithmParameters.getInstance("EC");
        ecParams.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecSpec = ecParams.getParameterSpec(ECParameterSpec.class);

        KeyFactory kf = KeyFactory.getInstance("EC");
        PublicKey clientPub = kf.generatePublic(new ECPublicKeySpec(
                new ECPoint(new BigInteger(1, xBytes), new BigInteger(1, yBytes)), ecSpec));

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
        KeyPair ephemeral = kpg.generateKeyPair();

        ECPublicKey ephPub = (ECPublicKey) ephemeral.getPublic();
        byte[] epkBytes = new byte[64];
        System.arraycopy(toBytes32(ephPub.getW().getAffineX()), 0, epkBytes, 0,  32);
        System.arraycopy(toBytes32(ephPub.getW().getAffineY()), 0, epkBytes, 32, 32);

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(ephemeral.getPrivate());
        ka.doPhase(clientPub, true);
        byte[] sharedRaw = ka.generateSecret();
        // Pad to 32 bytes in case JCE trims a leading zero from the X-coordinate
        byte[] shared32 = new byte[32];
        System.arraycopy(sharedRaw, 0, shared32, 32 - sharedRaw.length, sharedRaw.length);

        // AES key = SHA-256(shared secret X-coordinate) — matches DLL's BCryptDeriveKey/SHA256
        byte[] aesKey = MessageDigest.getInstance("SHA-256").digest(shared32);

        byte[] iv = new byte[GCM_IV_LEN];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(aesKey, "AES"),
                new GCMParameterSpec(GCM_TAG_LEN, iv));
        byte[] ct = cipher.doFinal(plainJson.getBytes(StandardCharsets.UTF_8));

        Base64.Encoder enc = Base64.getEncoder();
        JsonObject out = new JsonObject();
        out.addProperty("epk", enc.encodeToString(epkBytes));
        out.addProperty("iv",  enc.encodeToString(iv));
        out.addProperty("ct",  enc.encodeToString(ct));
        return out.toString();
    }

    /** Converts a BigInteger to exactly 32 bytes big-endian, handling sign byte and short values. */
    private static byte[] toBytes32(BigInteger n) {
        byte[] raw = n.toByteArray();
        if (raw.length == 32) return raw;
        byte[] out = new byte[32];
        if (raw.length < 32) {
            System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
        } else {
            // BigInteger may prepend a 0x00 sign byte — take the last 32 bytes
            System.arraycopy(raw, raw.length - 32, out, 0, 32);
        }
        return out;
    }
}
