package xai.xaiacserver.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
//? if minecraft:>=1.21.11
import net.minecraft.server.permissions.Permissions;
import xai.xaiacserver.commands.subcommands.BypassCommand;
import xai.xaiacserver.commands.subcommands.ConfigCommand;
import xai.xaiacserver.commands.subcommands.EnforceCommand;

public class XaiACCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("xaiac")
                //? if minecraft:>=1.21.11 {
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                //? } else {
                /*.requires(source -> source.hasPermission(4))
                *///? }
                .then(ConfigCommand.build())
                .then(EnforceCommand.build())
                .then(BypassCommand.build())
            )
        );
    }
}
