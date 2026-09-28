package xai.xaiacserver.handshake.payload;

import java.util.List;
import java.util.Map;

public record ChallengePacket(
        String nonce,
        String playerUuid,
        String sessionKeyBase64,
        String modMode,
        List<String> allowedMods,
        Map<String, String> modHashes,
        List<String> bannedMods,
        String packMode,
        List<String> allowedPacks,
        Map<String, String> packHashes,
        List<String> bannedPacks
) {}
