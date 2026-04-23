package xai.xaiacserver.handshake;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xai.xaiacserver.punishment.Punisher;

public class RecheckScheduler {

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Session session = SessionManager.get(player.getUUID());
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
