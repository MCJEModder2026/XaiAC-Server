package xai.xaiacserver.punishment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;
import xai.xaiacserver.XaiACServer;
import xai.xaiacserver.config.Config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Punisher {

    private static final List<String> SEVERITY_ORDER = List.of("log", "kick", "ban");

    /**
     * Applies the configured punishment for the given flags (log/kick/ban).
     * The session is NOT removed here — it persists until disconnect so rechecks can continue.
     */
    public static void punish(ServerPlayer player, List<String> flags) {
        FlagLogger.logFlags(player.getUUID(), player.getName().getString(), flags);

        String mode   = resolveMode(flags);
        String reason = String.join(", ", flags);

        switch (mode) {
            case "ban" -> {
                if (XaiACServer.SERVER != null) {
                    XaiACServer.SERVER.getPlayerList().getBans().add(new UserBanListEntry(new NameAndId(player.getGameProfile()), null, "[XaiAC]", null, reason));
                }
                player.connection.disconnect(Component.literal("[XaiAC] " + reason));
                FlagLogger.logBan(player.getName().getString(), flags);
            }
            case "kick" -> player.connection.disconnect(Component.literal("[XaiAC] " + reason));
            case "log"  -> XaiACServer.LOGGER.warn("Player {} flagged: {}", player.getName().getString(), reason);
        }
    }

    /** Kicks a player whose verification window expired without a valid RESPONSE. */
    public static void kickTimeout(ServerPlayer player) {
        FlagLogger.logTimeout(player.getUUID(), player.getName().getString());
        player.connection.disconnect(Component.literal("[XaiAC] Anticheat timeout"));
    }

    // comes directly from PacketRouter.java
    public static void handleReport(ServerPlayer player, JsonObject json) {
        List<String> flags = new ArrayList<>();
        JsonArray arr = json.getAsJsonArray("flags");
        if (arr != null) {
            arr.forEach(el -> {
                String f = el.getAsString().strip().replaceAll("[\\r\\n|]", "_");
                if (!f.isBlank()) flags.add(f);
            });
        }
        punish(player, flags);
    }

    private static String resolveMode(List<String> flags) {
        return flags.stream()
                .map(f -> Config.getPunishmentMode(stripFlagDetail(f)))
                .max(Comparator.comparingInt(m -> {
                    int idx = SEVERITY_ORDER.indexOf(m);
                    return idx == -1 ? 1 : idx;
                }))
                .orElse("kick");
    }

    private static String stripFlagDetail(String flag) {
        // Flags may carry a detail suffix e.g. "MOD_UNLISTED:wurst.jar", returns only the MOD_UNLISTED
        return flag.contains(":") ? flag.substring(0, flag.indexOf(':')) : flag;
    }
}
