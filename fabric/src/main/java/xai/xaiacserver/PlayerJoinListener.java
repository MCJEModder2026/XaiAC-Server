package xai.xaiacserver;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import xai.xaiacserver.config.Config;
import xai.xaiacserver.crypto.CryptoUtil;
import xai.xaiacserver.handshake.HandshakeManager;
import xai.xaiacserver.handshake.Session;
import xai.xaiacserver.handshake.SessionManager;


public class PlayerJoinListener {

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (Config.isPlayerBypassed(player.getUUID())) return;
            Session session = new Session(player.getName().getString(), CryptoUtil.generateNonce(), CryptoUtil.generateSessionKey(), Config.getKickTimeoutSeconds() * 20, Config.getRecheckIntervalSeconds() * 20);
            SessionManager.put(player.getUUID(), session);
            HandshakeManager.sendChallenge(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                SessionManager.remove(handler.player.getUUID())
        );
    }
}
