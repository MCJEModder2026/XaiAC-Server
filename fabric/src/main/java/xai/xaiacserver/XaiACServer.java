package xai.xaiacserver;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import xai.xaiacserver.commands.XaiACCommand;
import xai.xaiacserver.config.Config;
import xai.xaiacserver.handshake.RecheckScheduler;
import xai.xaiacserver.networking.PacketHandler;
import xai.xaiacserver.punishment.FlagLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XaiACServer implements ModInitializer {
    public static final String MOD_ID = "xaiac-server";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static MinecraftServer SERVER;

    @Override
    public void onInitialize() {
        Config.load(FabricLoader.getInstance().getConfigDir().resolve("xaiacserver.json"));
        FlagLogger.setRoot(FabricLoader.getInstance().getGameDir().resolve("Xai-AntiCheat"));
        PlayerJoinListener.register();
        PacketHandler.register();
        XaiACCommand.register();
        ServerTickEvents.END_SERVER_TICK.register(RecheckScheduler::tick);
        ServerLifecycleEvents.SERVER_STARTED.register(s -> SERVER = s);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> SERVER = null);
        LOGGER.info("XaiAC-Server initialized");
    }
}