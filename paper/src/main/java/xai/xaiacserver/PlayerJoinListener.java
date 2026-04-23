package xai.xaiacserver;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xai.xaiacserver.XaiACPlugin;
import xai.xaiacserver.config.Config;
import xai.xaiacserver.crypto.CryptoUtil;
import xai.xaiacserver.handshake.HandshakeManager;
import xai.xaiacserver.handshake.Session;
import xai.xaiacserver.handshake.SessionManager;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (Config.isPlayerBypassed(player.getUniqueId())) return;
        Session session = new Session(
            player.getName(),
            CryptoUtil.generateNonce(),
            CryptoUtil.generateSessionKey(),
            Config.getKickTimeoutSeconds() * 20,
            Config.getRecheckIntervalSeconds() * 20
        );
        SessionManager.put(player.getUniqueId(), session);
        // for some reason this delay is needed for consistency (the paper listener is faster than the fabric one)
        Bukkit.getScheduler().runTaskLater(XaiACPlugin.INSTANCE, () -> {
            if (player.isOnline()) HandshakeManager.sendChallenge(player);
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SessionManager.remove(event.getPlayer().getUniqueId());
    }
}
