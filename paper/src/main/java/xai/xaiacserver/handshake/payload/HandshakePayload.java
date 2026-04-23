package xai.xaiacserver.handshake.payload;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Handles encoding/decoding of handshake plugin messages.
 * Replicates Minecraft's FriendlyByteBuf#writeUtf wire format (VarInt length + UTF-8 bytes)
 * so the Fabric client codec remains compatible.
 */
public final class HandshakePayload {

    public static final String CHANNEL = "xaiac:handshake";

    private HandshakePayload() {}

    public static byte[] encode(String content) {
        byte[] stringBytes = content.getBytes(StandardCharsets.UTF_8);
        byte[] varInt = writeVarInt(stringBytes.length);
        byte[] result = new byte[varInt.length + stringBytes.length];
        System.arraycopy(varInt, 0, result, 0, varInt.length);
        System.arraycopy(stringBytes, 0, result, varInt.length, stringBytes.length);
        return result;
    }

    public static String decode(byte[] data) {
        int[] varint = readVarInt(data, 0);
        int length = varint[0];
        int offset = varint[1];
        return new String(data, offset, length, StandardCharsets.UTF_8);
    }

    private static byte[] writeVarInt(int value) {
        byte[] buf = new byte[5];
        int i = 0;
        while ((value & ~0x7F) != 0) {
            buf[i++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf[i++] = (byte) value;
        return Arrays.copyOf(buf, i);
    }

    // Returns [value, nextOffset]
    private static int[] readVarInt(byte[] data, int offset) {
        int value = 0, shift = 0;
        while (true) {
            byte b = data[offset++];
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        return new int[]{value, offset};
    }
}
