package xai.xaiacserver.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xai.xaiacserver.util.HexUtil;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Config {

    public static final List<String> ALL_FLAGS = ConfigData.ALL_FLAGS;

    private static final Logger LOGGER = LoggerFactory.getLogger("xaiac-server");
    private static final Gson GSON = new Gson();
    private static ConfigData data = new ConfigData();
    private static Path configFile;

    public static void load(Path file) {
        configFile = file;
        reload();
    }

    public static void reload() {
        if (!Files.exists(configFile)) {
            copyDefaultConfig(configFile);
        }
        try (Reader reader = Files.newBufferedReader(configFile)) {
            ConfigData parsed = GSON.fromJson(reader, ConfigData.class);
            if (parsed != null) data = parsed;
        } catch (IOException e) {
            LOGGER.error("Failed to load xaiacserver.json, using defaults", e);
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save xaiacserver.json", e);
        }
    }

    private static void copyDefaultConfig(Path dest) {
        try (InputStream in = Config.class.getResourceAsStream("/config/xaiacserver.json")) {
            if (in == null) { LOGGER.warn("Default config not found in JAR"); return; }
            Files.createDirectories(dest.getParent());
            Files.copy(in, dest);
        } catch (IOException e) {
            LOGGER.error("Failed to copy default config", e);
        }
    }


    public static int getKickTimeoutSeconds()        { return data.kick_timeout_seconds; }
    public static int getRecheckIntervalSeconds()    { return data.recheck_interval_seconds; }
    public static int getTimestampToleranceSeconds() { return data.timestamp_tolerance_seconds; }
    public static byte[] getClientEcPublicKeyBytes() { return HexUtil.hexToBytes(data.client_ec_public_key_hex); }
    public static String getModMode()                { return data.mod_mode; }
    public static String getPackMode()               { return data.pack_mode; }
    public static List<String> getAllowedMods()       { return data.allowed_mods; }
    public static Map<String, String> getModHashes() { return data.mod_hashes; }
    public static List<String> getBannedMods()        { return data.banned_mods; }
    public static List<String> getAllowedPacks()      { return data.allowed_packs; }
    public static List<String> getBannedPacks()       { return data.banned_packs; }
    public static Map<String, String> getPackHashes() { return data.pack_hashes; }

    public static String getPunishmentMode(String flag) {
        return data.punishment_modes.getOrDefault(flag, "kick");
    }

    public static boolean isPlayerBypassed(UUID uuid) {
        return data.bypassed_players.containsKey(uuid.toString());
    }

    public static Map<String, String> getBypassedPlayers() {
        return Collections.unmodifiableMap(data.bypassed_players);
    }


    public static void setPunishmentMode(String flag, String mode) {
        data.punishment_modes.put(flag, mode);
        save();
    }

    public static void setAllPunishmentModes(String mode) {
        ALL_FLAGS.forEach(f -> data.punishment_modes.put(f, mode));
        save();
    }

    public static void addBypassedPlayer(UUID uuid, String name) {
        data.bypassed_players.put(uuid.toString(), name);
        save();
    }


    public static boolean removeBypassedPlayer(UUID uuid) {
        boolean removed = data.bypassed_players.remove(uuid.toString()) != null;
        if (removed) save();
        return removed;
    }
}
