package xai.xaiacserver.handshake.payload;

import java.util.ArrayList;
import java.util.List;

/** Parsed representation of a decrypted client RESPONSE payload. */
public record ResponsePacket(String verdict, List<String> flags, String nonce, String uuid, long timestampMs) {

    /**
     * Parses the pipe-delimited decrypted payload: "verdict|flags|nonce|uuid|timestamp_ms"
     * The flags field is comma-separated. Returns null on any malformed input.
     */
    public static ResponsePacket parse(String decrypted) {
        String[] parts = decrypted.split("\\|", 5);
        if (parts.length < 5) return null;

        String verdict    = parts[0];
        String flagsCsv   = parts[1];
        String nonce      = parts[2];
        String uuid       = parts[3];
        long   timestampMs;
        try {
            timestampMs = Long.parseLong(parts[4].strip());
        } catch (NumberFormatException e) {
            return null;
        }

        List<String> flags = new ArrayList<>();
        if (!flagsCsv.isBlank()) {
            for (String f : flagsCsv.split(",")) {
                String sanitized = f.strip().replaceAll("[\\r\\n|]", "_");
                if (!sanitized.isBlank()) flags.add(sanitized);
            }
        }

        return new ResponsePacket(verdict, flags, nonce, uuid, timestampMs);
    }
}
