package xai.xaiacserver.crypto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Decrypt {

    private static final int GCM_TAG_LEN = 128; // bits

    /**
     * Decrypts a client RESPONSE envelope {"iv":"...","ct":"..."} using the per-session AES-256-GCM key.
     * Throws on auth tag mismatch or malformed input — callers treat any exception as CRYPTO_INVALID.
     */
    public static String decryptResponse(String encryptedJson, byte[] sessionKey) throws Exception {
        JsonObject json = JsonParser.parseString(encryptedJson).getAsJsonObject();
        byte[] iv            = Base64.getDecoder().decode(json.get("iv").getAsString());
        byte[] cipherWithTag = Base64.getDecoder().decode(json.get("ct").getAsString());

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(sessionKey, "AES"),
                new GCMParameterSpec(GCM_TAG_LEN, iv));

        return new String(cipher.doFinal(cipherWithTag), StandardCharsets.UTF_8);
    }
}
