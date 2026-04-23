package xai.xaiacserver.punishment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import xai.xaiacserver.XaiACPlugin;
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
    public static void punish(Player player, List<String> flags) {
        FlagLogger.logFlags(player.getUniqueId(), player.getName(), flags);

        String mode   = resolveMode(flags);
        String reason = String.join(", ", flags);

        switch (mode) {
            case "ban" -> {
                // CraftProfileBanList.addBan() is broken in Paper 1.21.1 (calls the deprecated getId()
                // which intentionally throws). Use the vanilla /ban command instead — it goes through
                // NMS UserBanList directly, and also kicks the online player automatically.
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "ban " + player.getName() + " [XaiAC] " + reason);
                FlagLogger.logBan(player.getName(), flags);
            }
            case "kick" -> player.kick(Component.text("[XaiAC] " + reason));
            case "log"  -> XaiACPlugin.LOGGER.warn("Player {} flagged: {}", player.getName(), reason);
        }
    }

    /** Kicks a player whose verification window expired without a valid RESPONSE. */
    public static void kickTimeout(Player player) {
        FlagLogger.logTimeout(player.getUniqueId(), player.getName());
        player.kick(Component.text("[XaiAC] Anticheat timeout"));
    }

    // comes directly from PacketRouter.java
    public static void handleReport(Player player, JsonObject json) {
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

    @SuppressWarnings("unchecked")
    public static BanList<PlayerProfile> profileBanList() {
        return (BanList<PlayerProfile>) Bukkit.getBanList(BanList.Type.PROFILE);
    }
}
