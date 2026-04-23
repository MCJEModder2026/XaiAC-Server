package xai.xaiacserver.commands.subcommands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;
import xai.xaiacserver.punishment.FlagLogger;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EnforceCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("enforce")
            .then(Commands.literal("player")
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        ctx.getSource().getServer().getPlayerList()
                            .getPlayers().forEach(p -> builder.suggest(p.getName().getString()));
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
                                ctx.getSource().sendFailure(Component.literal(
                                    "[XaiAC] No flag log found for check: " + checkName));
                                return 0;
                            }
                            StringBuilder sb = new StringBuilder("[XaiAC] Flagged for ").append(checkName).append(":\n");
                            for (UUID uuid : entries.keySet()) {
                                String name = FlagLogger.readPlayerName(uuid);
                                sb.append("  ").append(name != null ? name : uuid.toString()).append("\n");
                            }
                            String msg = sb.toString().stripTrailing();
                            ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                            return entries.size();
                        }

                        if (!action.equals("ban") && !action.equals("banip")) {
                            ctx.getSource().sendFailure(Component.literal(
                                "[XaiAC] Invalid action '" + action + "'. Use: ban, banip, view"));
                            return 0;
                        }

                        Map<UUID, String> entries = FlagLogger.readCheckEntries(checkName);
                        if (entries.isEmpty()) {
                            ctx.getSource().sendFailure(Component.literal(
                                "[XaiAC] No flag log found for check: " + checkName));
                            return 0;
                        }

                        MinecraftServer server = ctx.getSource().getServer();
                        String adminName = ctx.getSource().getTextName();
                        String reason = "XaiAC enforce: " + checkName;
                        int banned = 0, alreadyBanned = 0, skipped = 0;

                        for (Map.Entry<UUID, String> entry : entries.entrySet()) {
                            UUID uuid = entry.getKey();
                            String detail = entry.getValue();
                            String flagDisplay = checkName + (detail.isEmpty() ? "" : ": " + detail);

                            if (action.equals("ban")) {
                                if (server.getPlayerList().getBans().isBanned(new NameAndId(new GameProfile(uuid, "")))) {
                                    alreadyBanned++;
                                    continue;
                                }
                                String name = FlagLogger.readPlayerName(uuid);
                                if (name == null) name = uuid.toString();
                                server.getPlayerList().getBans().add(
                                    new UserBanListEntry(new NameAndId(new GameProfile(uuid, name)),
                                        null, "[XaiAC]", null, reason));
                                ServerPlayer online = server.getPlayerList().getPlayer(uuid);
                                if (online != null)
                                    online.connection.disconnect(Component.literal("[XaiAC] " + reason));
                                FlagLogger.logEnforceBan(name, flagDisplay, adminName);
                                banned++;
                            } else {
                                ServerPlayer online = server.getPlayerList().getPlayer(uuid);
                                if (online == null) { skipped++; continue; }
                                String ip = ((InetSocketAddress) online.connection
                                    .getRemoteAddress()).getAddress().getHostAddress();
                                if (server.getPlayerList().getIpBans().isBanned(ip)) {
                                    alreadyBanned++;
                                    continue;
                                }
                                server.getPlayerList().getIpBans().add(
                                    new IpBanListEntry(ip, null, "[XaiAC]", null, reason));
                                online.connection.disconnect(Component.literal("[XaiAC] " + reason));
                                String playerName = FlagLogger.readPlayerName(uuid);
                                if (playerName == null) playerName = uuid.toString();
                                FlagLogger.logEnforceBan(playerName, flagDisplay, adminName);
                                banned++;
                            }
                        }

                        int finalBanned = banned, finalAlready = alreadyBanned, finalSkipped = skipped;
                        ctx.getSource().sendSuccess(() -> {
                            StringBuilder sb = new StringBuilder("[XaiAC] enforce ")
                                .append(checkName).append(' ').append(action).append(": banned ")
                                .append(finalBanned).append(", already banned ").append(finalAlready);
                            if (finalSkipped > 0)
                                sb.append(", ").append(finalSkipped).append(" skipped (offline)");
                            return Component.literal(sb.toString());
                        }, true);
                        return banned;
                    })
                )
            );
    }

    private static int viewPlayerOffenses(CommandContext<CommandSourceStack> ctx) {
        String target = StringArgumentType.getString(ctx, "target");
        MinecraftServer server = ctx.getSource().getServer();

        UUID uuid = resolveUuid(target, server);
        if (uuid == null) {
            ctx.getSource().sendFailure(Component.literal(
                "[XaiAC] Player '" + target + "' has never joined this server."));
            return 0;
        }

        List<String> offenses = FlagLogger.readPlayerOffenses(uuid);
        String name = FlagLogger.readPlayerName(uuid);
        String display = name != null ? name : uuid.toString();

        if (offenses.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                "[XaiAC] No offenses recorded for " + display), false);
            return 0;
        }

        StringBuilder sb = new StringBuilder("[XaiAC] Offenses for ")
            .append(display).append(" (").append(offenses.size()).append("):\n");
        offenses.forEach(line -> sb.append("  ").append(line).append("\n"));
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString().stripTrailing()), false);
        return offenses.size();
    }

    private static UUID resolveUuid(String target, MinecraftServer server) {
        try { return UUID.fromString(target); } catch (IllegalArgumentException ignored) {}
        ServerPlayer online = server.getPlayerList().getPlayerByName(target);
        if (online != null) return online.getUUID();
        return server.services().nameToIdCache().get(target)
            .map(NameAndId::id)
            .orElse(null);
    }
}
