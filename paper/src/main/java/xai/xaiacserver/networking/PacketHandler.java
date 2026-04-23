package xai.xaiacserver.networking;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import xai.xaiacserver.handshake.payload.HandshakePayload;

public class PacketHandler implements PluginMessageListener {

    public static void register(Plugin plugin) {
        PacketHandler handler = new PacketHandler();
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, HandshakePayload.CHANNEL, handler);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, HandshakePayload.CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(HandshakePayload.CHANNEL)) return;
        String content = HandshakePayload.decode(message);
        PacketRouter.route(player, content);
    }
}
