package xai.xaiacserver.handshake;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xai.xaiacserver.punishment.Punisher;

public class RecheckScheduler {

    public static void register(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, RecheckScheduler::tick, 0L, 1L);
    }

    private static void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Session session = SessionManager.get(player.getUniqueId());
            if (session == null) continue;

            if (session.state == Session.State.PENDING) {
                session.kickTicksRemaining--;
                if (session.kickTicksRemaining <= 0) {
                    Punisher.kickTimeout(player);
                }
            } else if (session.state == Session.State.ANSWERED) {
                session.recheckTicksRemaining--;
                if (session.recheckTicksRemaining <= 0) {
                    HandshakeManager.sendRecheck(player);
                }
            }
        }
    }
}
