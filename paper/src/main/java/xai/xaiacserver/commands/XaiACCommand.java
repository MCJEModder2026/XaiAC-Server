package xai.xaiacserver.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;
import xai.xaiacserver.commands.subcommands.BypassCommand;
import xai.xaiacserver.commands.subcommands.ConfigCommand;
import xai.xaiacserver.commands.subcommands.EnforceCommand;

@SuppressWarnings("UnstableApiUsage")
public class XaiACCommand {

    public static void register(Plugin plugin) {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(
                Commands.literal("xaiac")
                    .requires(source -> source.getSender().hasPermission("xaiac.admin"))
                    .then(ConfigCommand.build())
                    .then(EnforceCommand.build())
                    .then(BypassCommand.build())
                    .build()
            );
        });
    }
}
