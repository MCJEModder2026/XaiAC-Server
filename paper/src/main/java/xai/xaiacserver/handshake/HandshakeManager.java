package xai.xaiacserver.handshake;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;
import xai.xaiacserver.XaiACPlugin;
import xai.xaiacserver.config.Config;
import xai.xaiacserver.crypto.CryptoUtil;
import xai.xaiacserver.crypto.Encrypt;
import xai.xaiacserver.handshake.payload.HandshakePayload;
import xai.xaiacserver.handshake.payload.ResponsePacket;
import xai.xaiacserver.punishment.Punisher;

import java.util.Base64;
import java.util.List;

public class HandshakeManager {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    /** Encrypts and sends the initial CHALLENGE. Called from PlayerJoinListener on join. */
    public static void sendChallenge(Player player) {
        Session session = SessionManager.get(player.getUniqueId());
        if (session == null) return;
        try {
            String plainJson = ChallengeBuilder.build(player.getUniqueId().toString(), session.nonce, session.sessionKey);
            JsonObject envelope = JsonParser.parseString(
                    Encrypt.encryptForClient(plainJson, Config.getClientEcPublicKeyBytes())).getAsJsonObject();
            envelope.addProperty("type", "CHALLENGE");
            player.sendPluginMessage(XaiACPlugin.INSTANCE, HandshakePayload.CHANNEL, HandshakePayload.encode(GSON.toJson(envelope)));
        } catch (Exception e) {
            XaiACPlugin.LOGGER.error("Failed to encrypt challenge for {}: {}", player.getName(), e.getMessage());
        }
    }

    /**
     * Called by PacketRouter with the already-decrypted pipe-delimited RESPONSE payload.
     * Validates nonce, uuid, and timestamp, then applies punishment or marks as ANSWERED.
     */
    public static void handleResponse(Player player, String decryptedPayload) {
        Session session = SessionManager.get(player.getUniqueId());
        if (session == null || session.state == Session.State.ANSWERED) return;

        ResponsePacket response = ResponsePacket.parse(decryptedPayload);
        if (response == null
                || !session.nonce.equals(response.nonce())
                || !player.getUniqueId().toString().equals(response.uuid())
                || !isTimestampValid(response.timestampMs())) {
            handleCryptoInvalid(player);
            return;
        }

        if ("cheating".equals(response.verdict())) {
            Punisher.punish(player, response.flags());
        }

        session.state = Session.State.ANSWERED;
        session.recheckTicksRemaining = Config.getRecheckIntervalSeconds() * 20;
    }

    /** Called by PacketRouter when RESPONSE decryption fails. */
    public static void handleCryptoInvalid(Player player) {
        Session session = SessionManager.get(player.getUniqueId());
        Punisher.punish(player, List.of("CRYPTO_INVALID"));
        if (session != null) {
            session.state = Session.State.ANSWERED;
            session.recheckTicksRemaining = Config.getRecheckIntervalSeconds() * 20;
        }
    }

    /**
     * Called by RecheckScheduler when recheckTicksRemaining hits 0.
     * Rotates nonce + session key, resets kick timer, and sends an encrypted RECHECK.
     */
    public static void sendRecheck(Player player) {
        Session session = SessionManager.get(player.getUniqueId());
        if (session == null) return;

        String newNonce = CryptoUtil.generateNonce();
        byte[] newKey   = CryptoUtil.generateSessionKey();
        session.nonce              = newNonce;
        session.sessionKey         = newKey;
        session.state              = Session.State.PENDING;
        session.kickTicksRemaining = Config.getKickTimeoutSeconds() * 20;

        try {
            JsonObject recheckInner = new JsonObject();
            recheckInner.addProperty("nonce",       newNonce);
            recheckInner.addProperty("session_key", Base64.getEncoder().encodeToString(newKey));

            JsonObject envelope = JsonParser.parseString(
                    Encrypt.encryptForClient(GSON.toJson(recheckInner), Config.getClientEcPublicKeyBytes())).getAsJsonObject();
            envelope.addProperty("type", "RECHECK");
            player.sendPluginMessage(XaiACPlugin.INSTANCE, HandshakePayload.CHANNEL, HandshakePayload.encode(GSON.toJson(envelope)));
        } catch (Exception e) {
            XaiACPlugin.LOGGER.error("Failed to encrypt recheck for {}: {}", player.getName(), e.getMessage());
        }
    }

    private static boolean isTimestampValid(long timestampMs) {
        long toleranceMs = Config.getTimestampToleranceSeconds() * 1000L;
        return Math.abs(System.currentTimeMillis() - timestampMs) <= toleranceMs;
    }
}
