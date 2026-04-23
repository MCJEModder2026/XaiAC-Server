package xai.xaiacserver.commands.subcommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xai.xaiacserver.config.Config;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class BypassCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("bypass")
            .then(Commands.literal("add")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String input = StringArgumentType.getString(ctx, "name");

                        UUID uuid;
                        String displayName;
                        Player online = Bukkit.getPlayerExact(input);
                        if (online != null) {
                            uuid = online.getUniqueId();
                            displayName = online.getName();
                        } else {
                            try {
                                uuid = UUID.fromString(input);
                                displayName = input;
                            } catch (IllegalArgumentException e) {
                                ctx.getSource().getSender().sendMessage(Component.text(
                                    "[XaiAC] '" + input + "' is not online and is not a valid UUID.", NamedTextColor.RED));
                                return 0;
                            }
                        }

                        if (Config.isPlayerBypassed(uuid)) {
                            ctx.getSource().getSender().sendMessage(Component.text(
                                "[XaiAC] " + displayName + " is already bypassed.", NamedTextColor.RED));
                            return 0;
                        }

                        Config.addBypassedPlayer(uuid, displayName);
                        final String dn = displayName;
                        final UUID u = uuid;
                        ctx.getSource().getSender().sendMessage(Component.text(
                            "[XaiAC] Added " + dn + " (" + u + ") to bypass list."));
                        return 1;
                    })
                )
            )
            .then(Commands.literal("list")
                .executes(ctx -> {
                    Map<String, String> bypassed = Config.getBypassedPlayers();
                    if (bypassed.isEmpty()) {
                        ctx.getSource().getSender().sendMessage(Component.text("[XaiAC] Bypass list is empty."));
                        return 1;
                    }
                    StringBuilder sb = new StringBuilder("[XaiAC] Bypassed players:\n");
                    for (Map.Entry<String, String> entry : bypassed.entrySet()) {
                        sb.append("  ").append(entry.getKey()).append(" (").append(entry.getValue()).append(")\n");
                    }
                    ctx.getSource().getSender().sendMessage(Component.text(sb.toString().stripTrailing()));
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
                            ctx.getSource().getSender().sendMessage(Component.text(
                                "[XaiAC] '" + input + "' is not in the bypass list.", NamedTextColor.RED));
                            return 0;
                        }

                        final UUID finalUuid = uuid;
                        final String finalName = displayName;
                        Config.removeBypassedPlayer(uuid);
                        ctx.getSource().getSender().sendMessage(Component.text(
                            "[XaiAC] Removed " + finalName + " (" + finalUuid + ") from bypass list."));
                        return 1;
                    })
                )
            );
    }
}
