package xai.xaiacserver.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import xai.xaiacserver.handshake.payload.HandshakePayload;

public class PacketHandler {

    public static void register() {
        //~ if minecraft:>=26 'playC2S' -> 'serverboundPlay'
        PayloadTypeRegistry.playC2S().register(HandshakePayload.TYPE, HandshakePayload.CODEC);
        //~ if minecraft:>=26 'playS2C' -> 'clientboundPlay'
        PayloadTypeRegistry.playS2C().register(HandshakePayload.TYPE, HandshakePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HandshakePayload.TYPE,
                (payload, context) -> PacketRouter.route(context.player(), payload));

    }
}
