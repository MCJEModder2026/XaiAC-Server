package xai.xaiacserver.commands.subcommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import xai.xaiacserver.config.Config;

import java.util.List;

public class ConfigCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("config")
            .then(Commands.literal("load")
                .executes(ctx -> {
                    Config.reload();
                    ctx.getSource().sendSuccess(() -> Component.literal("[XaiAC] Config reloaded."), true);
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
                                ctx.getSource().sendFailure(Component.literal("[XaiAC] Unknown check: " + check));
                                return 0;
                            }

                            if (mode.equals("get")) {
                                if (isAll) {
                                    StringBuilder sb = new StringBuilder("[XaiAC] Punishment modes:\n");
                                    Config.ALL_FLAGS.forEach(f ->
                                        sb.append("  ").append(f).append(": ").append(Config.getPunishmentMode(f)).append("\n"));
                                    String msg = sb.toString().stripTrailing();
                                    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                    return Config.ALL_FLAGS.size();
                                } else {
                                    String current = Config.getPunishmentMode(check);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                        "[XaiAC] " + check + " punishment mode: " + current), false);
                                    return 1;
                                }
                            }

                            if (!List.of("kick", "log", "ban").contains(mode)) {
                                ctx.getSource().sendFailure(Component.literal(
                                    "[XaiAC] Invalid mode '" + mode + "'. Use: kick, log, ban, get"));
                                return 0;
                            }

                            if (isAll) {
                                Config.setAllPunishmentModes(mode);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "[XaiAC] Set ALL punishment modes to " + mode + "."), true);
                                return Config.ALL_FLAGS.size();
                            } else {
                                Config.setPunishmentMode(check, mode);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "[XaiAC] Set " + check + " punishment mode to " + mode + "."), true);
                                return 1;
                            }
                        })
                    )
                )
            );
    }
}
