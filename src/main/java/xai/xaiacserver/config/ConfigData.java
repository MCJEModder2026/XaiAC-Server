package xai.xaiacserver.config;

import java.util.*;

public class ConfigData {

    static final List<String> ALL_FLAGS = List.of(
        "MOD_UNLISTED", "MOD_HASH_MISMATCH",
        "PACK_BLACKLISTED", "PACK_NOT_WHITELISTED", "PACK_HASH_MISMATCH",
        "MODULE_NO_PATH", "MODULE_TEMP_PATH",
        "SELF_HASH_MISMATCH", "BYTECODE_TAMPERED", "DLL_CODE_TAMPERED",
        "CRYPTO_INVALID"
    );

    int kick_timeout_seconds        = 10;
    int recheck_interval_seconds    = 60;
    int timestamp_tolerance_seconds = 30;
    String client_ec_public_key_hex = "4744073bafe08da674a95b4ee07610260fec08cd1cae57fab6d36cd19848dae6b3203ccdfebc1a661c7eb0c67845deaa3b3c2d0d20a2f15596e79788d77e6f27";
    String mod_mode                 = "blacklist";
    String pack_mode                = "blacklist";
    List<String> allowed_mods       = new ArrayList<>();
    Map<String, String> mod_hashes  = new HashMap<>();
    List<String> banned_mods        = new ArrayList<>();
    List<String> allowed_packs      = new ArrayList<>(List.of("Default"));
    List<String> banned_packs       = new ArrayList<>(List.of("xray"));
    Map<String, String> pack_hashes = new HashMap<>();
    Map<String, String> punishment_modes  = defaultPunishmentModes();
    Map<String, String> bypassed_players  = new LinkedHashMap<>();

    static Map<String, String> defaultPunishmentModes() {
        Map<String, String> m = new LinkedHashMap<>();
        ALL_FLAGS.forEach(f -> m.put(f, "kick"));
        return m;
    }
}
