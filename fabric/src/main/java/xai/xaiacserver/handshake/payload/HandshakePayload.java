package xai.xaiacserver.handshake.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HandshakePayload(String content) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HandshakePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("xaiac", "handshake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HandshakePayload> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeUtf(value.content()),
            buf -> new HandshakePayload(buf.readUtf())
    );

    @Override
    public CustomPacketPayload.Type<HandshakePayload> type() {
        return TYPE;
    }
}
