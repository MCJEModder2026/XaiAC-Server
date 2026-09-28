package xai.xaiacserver.handshake.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//~ if minecraft:<1.21.11 'Identifier' -> 'ResourceLocation'
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record HandshakePayload(String content) implements CustomPacketPayload {

    public static final Type<HandshakePayload> TYPE =
        //~ if minecraft:<1.21.11 'Identifier' -> 'ResourceLocation'
        new Type<>(Identifier.fromNamespaceAndPath("xaiac", "handshake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HandshakePayload> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeUtf(value.content()),
            buf -> new HandshakePayload(buf.readUtf())
    );

    @Override
    public @NonNull Type<HandshakePayload> type() {
        return TYPE;
    }
}
