package xai.xaiacserver.networking;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;
import xai.xaiacserver.crypto.Decrypt;
import xai.xaiacserver.handshake.HandshakeManager;
import xai.xaiacserver.handshake.Session;
import xai.xaiacserver.handshake.SessionManager;
import xai.xaiacserver.handshake.payload.HandshakePayload;
import xai.xaiacserver.punishment.Punisher;

public class PacketRouter {

    public static void route(ServerPlayer player, HandshakePayload payload) {
        String content = payload.content();
        try {
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            if ("REPORT".equals(json.get("type").getAsString())) {
                Punisher.handleReport(player, json);
                return;
            }
        } catch (Exception ignored) {}

        // If its not a report its a response (encrypted)
        routeResponse(player, content);
    }

    private static void routeResponse(ServerPlayer player, String encryptedContent) {
        Session session = SessionManager.get(player.getUUID());
        if (session == null) {
            return;
        }
        try {
            String decrypted = Decrypt.decryptResponse(encryptedContent, session.sessionKey);
            HandshakeManager.handleResponse(player, decrypted);
        } catch (Exception e) {
            HandshakeManager.handleCryptoInvalid(player);
        }
    }
}
