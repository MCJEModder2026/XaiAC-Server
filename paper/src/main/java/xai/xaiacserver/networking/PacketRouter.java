package xai.xaiacserver.networking;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;
import xai.xaiacserver.crypto.Decrypt;
import xai.xaiacserver.handshake.HandshakeManager;
import xai.xaiacserver.handshake.Session;
import xai.xaiacserver.handshake.SessionManager;
import xai.xaiacserver.punishment.Punisher;

public class PacketRouter {

    public static void route(Player player, String content) {
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

    private static void routeResponse(Player player, String encryptedContent) {
        Session session = SessionManager.get(player.getUniqueId());
        if (session == null) return;
        try {
            String decrypted = Decrypt.decryptResponse(encryptedContent, session.sessionKey);
            HandshakeManager.handleResponse(player, decrypted);
        } catch (Exception e) {
            HandshakeManager.handleCryptoInvalid(player);
        }
    }
}
