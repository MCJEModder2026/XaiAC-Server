package xai.xaiacserver.util;

public class HexUtil {

    public static byte[] hexToBytes(String hex) {
        hex = hex.strip();
        if (hex.length() % 2 != 0) throw new IllegalArgumentException("Odd-length hex string");
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++)
            out[i] = (byte) Integer.parseInt(hex, i * 2, i * 2 + 2, 16);
        return out;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
