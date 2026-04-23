package xai.xaiacserver.commands.subcommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xai.xaiacserver.config.Config;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ConfigCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("config")
            .then(Commands.literal("load")
                .executes(ctx -> {
                    Config.reload();
                    ctx.getSource().getSender().sendMessage(Component.text("[XaiAC] Config reloaded."));
                    return 1;
                })
            )
            .then(Commands.literal("punishment_mode")
                .then(Commands.argument("check", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        builder.suggest("ALL");
                        Config.ALL_FLAGS.forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            List.of("kick", "log", "ban", "get").forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String check = StringArgumentType.getString(ctx, "check");
                            String mode  = StringArgumentType.getString(ctx, "mode");

                            boolean isAll = check.equalsIgnoreCase("ALL");
                            if (!isAll && !Config.ALL_FLAGS.contains(check)) {
                                ctx.getSource().getSender().sendMessage(Component.text(
                                    "[XaiAC] Unknown check: " + check, NamedTextColor.RED));
                                return 0;
                            }

                            if (mode.equals("get")) {
                                if (isAll) {
                                    StringBuilder sb = new StringBuilder("[XaiAC] Punishment modes:\n");
                                    Config.ALL_FLAGS.forEach(f ->
                                        sb.append("  ").append(f).append(": ").append(Config.getPunishmentMode(f)).append("\n"));
                                    ctx.getSource().getSender().sendMessage(Component.text(sb.toString().stripTrailing()));
                                    return Config.ALL_FLAGS.size();
                                } else {
                                    ctx.getSource().getSender().sendMessage(Component.text(
                                        "[XaiAC] " + check + " punishment mode: " + Config.getPunishmentMode(check)));
                                    return 1;
                                }
                            }

                            if (!List.of("kick", "log", "ban").contains(mode)) {
                                ctx.getSource().getSender().sendMessage(Component.text(
                                    "[XaiAC] Invalid mode '" + mode + "'. Use: kick, log, ban, get", NamedTextColor.RED));
                                return 0;
                            }

                            if (isAll) {
                                Config.setAllPunishmentModes(mode);
                                ctx.getSource().getSender().sendMessage(Component.text(
                                    "[XaiAC] Set ALL punishment modes to " + mode + "."));
                                return Config.ALL_FLAGS.size();
                            } else {
                                Config.setPunishmentMode(check, mode);
                                ctx.getSource().getSender().sendMessage(Component.text(
                                    "[XaiAC] Set " + check + " punishment mode to " + mode + "."));
                                return 1;
                            }
                        })
                    )
                )
            );
    }
}
