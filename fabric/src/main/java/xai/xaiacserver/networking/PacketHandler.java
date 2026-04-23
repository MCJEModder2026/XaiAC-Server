package xai.xaiacserver.networking;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import xai.xaiacserver.handshake.RecheckScheduler;
import xai.xaiacserver.handshake.payload.HandshakePayload;

public class PacketHandler {

    public static void register() {
        PayloadTypeRegistry.playC2S().register(HandshakePayload.TYPE, HandshakePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HandshakePayload.TYPE, HandshakePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HandshakePayload.TYPE,
                (payload, context) -> PacketRouter.route(context.player(), payload));

    }
}
