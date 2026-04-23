package xai.xaiacserver.commands.subcommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import xai.xaiacserver.punishment.FlagLogger;
import xai.xaiacserver.punishment.Punisher;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class EnforceCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("enforce")
            .then(Commands.literal("player")
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                        return builder.buildFuture();
                    })
                    .then(Commands.literal("view")
                        .executes(ctx -> viewPlayerOffenses(ctx))
                    )
                )
            )
            .then(Commands.argument("checkname", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    FlagLogger.listCheckNames().forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .then(Commands.argument("action", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        List.of("ban", "banip", "view").forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String checkName = StringArgumentType.getString(ctx, "checkname");
                        String action    = StringArgumentType.getString(ctx, "action");

                        if (action.equals("view")) {
                            Map<UUID, String> entries = FlagLogger.readCheckEntries(checkName);
                            if (entries.isEmpty()) {
                                ctx.getSource().getSender().sendMessage(Component.text(
                                    "[XaiAC] No flag log found for check: " + checkName, NamedTextColor.RED));
                                return 0;
                            }
                            StringBuilder sb = new StringBuilder("[XaiAC] Flagged for ").append(checkName).append(":\n");
                            for (UUID uuid : entries.keySet()) {
                                String name = FlagLogger.readPlayerName(uuid);
                                sb.append("  ").append(name != null ? name : uuid.toString()).append("\n");
                            }
                            ctx.getSource().getSender().sendMessage(Component.text(sb.toString().stripTrailing()));
                            return entries.size();
                        }

                        if (!action.equals("ban") && !action.equals("banip")) {
                            ctx.getSource().getSender().sendMessage(Component.text(
                                "[XaiAC] Invalid action '" + action + "'. Use: ban, banip, view", NamedTextColor.RED));
                            return 0;
                        }

                        Map<UUID, String> entries = FlagLogger.readCheckEntries(checkName);
                        if (entries.isEmpty()) {
                            ctx.getSource().getSender().sendMessage(Component.text(
                                "[XaiAC] No flag log found for check: " + checkName, NamedTextColor.RED));
                            return 0;
                        }

                        BanList<PlayerProfile> profileBans = Punisher.profileBanList();
                        String adminName = ctx.getSource().getSender().getName();
                        String reason = "XaiAC enforce: " + checkName;
                        int banned = 0, alreadyBanned = 0, skipped = 0;

                        for (Map.Entry<UUID, String> entry : entries.entrySet()) {
                            UUID uuid = entry.getKey();
                            String detail = entry.getValue();
                            String flagDisplay = checkName + (detail.isEmpty() ? "" : ": " + detail);

                            if (action.equals("ban")) {
                                if (profileBans.isBanned(Bukkit.createPlayerProfile(uuid))) {
                                    alreadyBanned++;
                                    continue;
                                }
                                String name = FlagLogger.readPlayerName(uuid);
                                if (name == null) { skipped++; continue; } // need a name for /ban
                                // CraftProfileBanList.addBan() is broken in Paper 1.21.1 — use the
                                // vanilla /ban command which goes through NMS directly and also kicks
                                // the player if they are currently online.
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                    "ban " + name + " [XaiAC] enforce: " + checkName);
                                FlagLogger.logEnforceBan(name, flagDisplay, adminName);
                                banned++;
                            } else {
                                Player online = Bukkit.getPlayer(uuid);
                                if (online == null) { skipped++; continue; }
                                String ip = online.getAddress().getAddress().getHostAddress();
                                if (Bukkit.getBanList(BanList.Type.IP).isBanned(ip)) {
                                    alreadyBanned++;
                                    continue;
                                }
                                Bukkit.getBanList(BanList.Type.IP).addBan(ip, reason, (java.util.Date) null, "[XaiAC]");
                                online.kick(Component.text("[XaiAC] " + reason));
                                String playerName = FlagLogger.readPlayerName(uuid);
                                if (playerName == null) playerName = uuid.toString();
                                FlagLogger.logEnforceBan(playerName, flagDisplay, adminName);
                                banned++;
                            }
                        }

                        int finalBanned = banned, finalAlready = alreadyBanned, finalSkipped = skipped;
                        StringBuilder sb = new StringBuilder("[XaiAC] enforce ")
                            .append(checkName).append(' ').append(action).append(": banned ")
                            .append(finalBanned).append(", already banned ").append(finalAlready);
                        if (finalSkipped > 0)
                            sb.append(", ").append(finalSkipped).append(" skipped (offline)");
                        ctx.getSource().getSender().sendMessage(Component.text(sb.toString()));
                        return banned;
                    })
                )
            );
    }

    private static int viewPlayerOffenses(CommandContext<CommandSourceStack> ctx) {
        String target = StringArgumentType.getString(ctx, "target");

        UUID uuid = resolveUuid(target);
        if (uuid == null) {
            ctx.getSource().getSender().sendMessage(Component.text(
                "[XaiAC] Player '" + target + "' has never joined this server.", NamedTextColor.RED));
            return 0;
        }

        List<String> offenses = FlagLogger.readPlayerOffenses(uuid);
        String name = FlagLogger.readPlayerName(uuid);
        String display = name != null ? name : uuid.toString();

        if (offenses.isEmpty()) {
            ctx.getSource().getSender().sendMessage(Component.text(
                "[XaiAC] No offenses recorded for " + display));
            return 0;
        }

        StringBuilder sb = new StringBuilder("[XaiAC] Offenses for ")
            .append(display).append(" (").append(offenses.size()).append("):\n");
        offenses.forEach(line -> sb.append("  ").append(line).append("\n"));
        ctx.getSource().getSender().sendMessage(Component.text(sb.toString().stripTrailing()));
        return offenses.size();
    }

    @SuppressWarnings("deprecation")
    private static UUID resolveUuid(String target) {
        try { return UUID.fromString(target); } catch (IllegalArgumentException ignored) {}
        Player online = Bukkit.getPlayerExact(target);
        if (online != null) return online.getUniqueId();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(target);
        return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
    }
}
