package xai.xaiacserver.commands.subcommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xai.xaiacserver.config.Config;

import java.util.Map;
import java.util.UUID;

public class BypassCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("bypass")
            .then(Commands.literal("add")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        ctx.getSource().getServer().getPlayerList().getPlayers()
                            .forEach(p -> builder.suggest(p.getName().getString()));
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String input = StringArgumentType.getString(ctx, "name");

                        UUID uuid;
                        String displayName;
                        ServerPlayer online = ctx.getSource().getServer().getPlayerList().getPlayerByName(input);
                        if (online != null) {
                            uuid = online.getUUID();
                            displayName = online.getName().getString();
                        } else {
                            try {
                                uuid = UUID.fromString(input);
                                displayName = input;
                            } catch (IllegalArgumentException e) {
                                ctx.getSource().sendFailure(Component.literal(
                                    "[XaiAC] '" + input + "' is not online and is not a valid UUID."));
                                return 0;
                            }
                        }

                        if (Config.isPlayerBypassed(uuid)) {
                            ctx.getSource().sendFailure(Component.literal(
                                "[XaiAC] " + displayName + " is already bypassed."));
                            return 0;
                        }

                        Config.addBypassedPlayer(uuid, displayName);
                        final String dn = displayName;
                        final UUID u = uuid;
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "[XaiAC] Added " + dn + " (" + u + ") to bypass list."), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("list")
                .executes(ctx -> {
                    Map<String, String> bypassed = Config.getBypassedPlayers();
                    if (bypassed.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("[XaiAC] Bypass list is empty."), false);
                        return 1;
                    }
                    StringBuilder sb = new StringBuilder("[XaiAC] Bypassed players:\n");
                    for (Map.Entry<String, String> entry : bypassed.entrySet()) {
                        sb.append("  ").append(entry.getKey()).append(" (").append(entry.getValue()).append(")\n");
                    }
                    String msg = sb.toString().stripTrailing();
                    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                    return bypassed.size();
                })
            )
            .then(Commands.literal("remove")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        Config.getBypassedPlayers().forEach((uuidStr, name) ->
                            builder.suggest(name.isEmpty() ? uuidStr : name));
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String input = StringArgumentType.getString(ctx, "name");

                        UUID uuid = null;
                        String displayName = null;

                        // Try UUID parse first
                        try {
                            UUID parsed = UUID.fromString(input);
                            if (Config.isPlayerBypassed(parsed)) {
                                uuid = parsed;
                                displayName = Config.getBypassedPlayers().getOrDefault(uuid.toString(), uuid.toString());
                            }
                        } catch (IllegalArgumentException ignored) {}

                        // Fall back to name match
                        if (uuid == null) {
                            for (Map.Entry<String, String> entry : Config.getBypassedPlayers().entrySet()) {
                                if (entry.getValue().equalsIgnoreCase(input)) {
                                    uuid = UUID.fromString(entry.getKey());
                                    displayName = entry.getValue();
                                    break;
                                }
                            }
                        }

                        if (uuid == null) {
                            ctx.getSource().sendFailure(Component.literal(
                                "[XaiAC] '" + input + "' is not in the bypass list."));
                            return 0;
                        }

                        final UUID finalUuid = uuid;
                        final String finalName = displayName;
                        Config.removeBypassedPlayer(uuid);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "[XaiAC] Removed " + finalName + " (" + finalUuid + ") from bypass list."), true);
                        return 1;
                    })
                )
            );
    }
}
