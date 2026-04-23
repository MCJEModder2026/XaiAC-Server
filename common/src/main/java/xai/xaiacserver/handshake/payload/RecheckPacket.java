package xai.xaiacserver.handshake.payload;

public record RecheckPacket(String nonce, String sessionKeyBase64) {}
