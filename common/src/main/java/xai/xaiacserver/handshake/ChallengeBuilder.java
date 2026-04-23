package xai.xaiacserver.handshake;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import xai.xaiacserver.config.Config;
import xai.xaiacserver.handshake.payload.ChallengePacket;

import java.util.Base64;

public class ChallengeBuilder {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    /** Builds the plaintext challenge JSON to be encrypted and sent to the client. */
    public static String build(String playerUuid, String nonce, byte[] sessionKey) {
        ChallengePacket packet = new ChallengePacket(
                nonce,
                playerUuid,
                Base64.getEncoder().encodeToString(sessionKey),
                Config.getModMode(),
                Config.getAllowedMods(),
                Config.getModHashes(),
                Config.getBannedMods(),
                Config.getPackMode(),
                Config.getAllowedPacks(),
                Config.getPackHashes(),
                Config.getBannedPacks()
        );
        return toJson(packet);
    }

    private static String toJson(ChallengePacket p) {
        JsonObject obj = new JsonObject();
        obj.addProperty("nonce",           p.nonce());
        obj.addProperty("player_uuid",     p.playerUuid());
        obj.addProperty("session_key",     p.sessionKeyBase64());
        obj.addProperty("mod_mode",        p.modMode());

        JsonArray allowedMods = new JsonArray();
        p.allowedMods().forEach(allowedMods::add);
        obj.add("allowed_mods", allowedMods);

        JsonObject modHashes = new JsonObject();
        p.modHashes().forEach(modHashes::addProperty);
        obj.add("mod_hashes", modHashes);

        JsonArray bannedMods = new JsonArray();
        p.bannedMods().forEach(bannedMods::add);
        obj.add("banned_mods", bannedMods);

        obj.addProperty("pack_mode",       p.packMode());

        JsonArray allowedPacks = new JsonArray();
        p.allowedPacks().forEach(allowedPacks::add);
        obj.add("allowed_packs", allowedPacks);

        JsonArray bannedPacks = new JsonArray();
        p.bannedPacks().forEach(bannedPacks::add);
        obj.add("banned_packs", bannedPacks);

        JsonObject packHashes = new JsonObject();
        p.packHashes().forEach(packHashes::addProperty);
        obj.add("pack_hashes", packHashes);

        return GSON.toJson(obj);
    }
}
