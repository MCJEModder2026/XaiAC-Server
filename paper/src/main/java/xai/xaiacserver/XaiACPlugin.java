package xai.xaiacserver;

import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xai.xaiacserver.commands.XaiACCommand;
import xai.xaiacserver.config.Config;
import xai.xaiacserver.handshake.RecheckScheduler;
import xai.xaiacserver.networking.PacketHandler;
import xai.xaiacserver.punishment.FlagLogger;

public class XaiACPlugin extends JavaPlugin {

    public static final Logger LOGGER = LoggerFactory.getLogger("xaiac-server");
    public static XaiACPlugin INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;
        Config.load(getDataFolder().toPath().toAbsolutePath().getParent().getParent().resolve("config").resolve("xaiacserver.json"));
        FlagLogger.setRoot(getDataFolder().toPath().toAbsolutePath().getParent().getParent().resolve("Xai-AntiCheat"));
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        PacketHandler.register(this);
        XaiACCommand.register(this);
        RecheckScheduler.register(this);
        LOGGER.info("XaiAC-Server initialized");
    }

    @Override
    public void onDisable() {
        INSTANCE = null;
    }
}
