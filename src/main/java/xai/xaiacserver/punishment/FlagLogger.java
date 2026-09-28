package xai.xaiacserver.punishment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FlagLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("xaiac-server");
    private static Path rootDir;

    /** Logs that a player was flagged with the given flags. */
    public static void logFlags(UUID uuid, String username, List<String> flags) {
        if (flags.isEmpty()) return;
        String timestamp = Instant.now().toString();
        appendPlayerLog(uuid, username, timestamp, flags);
        appendCheckLogs(uuid, flags);
    }

    /** Logs that a player was automatically banned. */
    public static void logBan(String username, List<String> flags) {
        String flagsDisplay = flags.stream()
                .map(f -> f.contains(":") ? f.replaceFirst(":", ": ") : f)
                .collect(Collectors.joining(", "));
        appendLine(root().resolve("bans.log"),
                "Player " + username + " banned: \"" + flagsDisplay + "\" " + Instant.now());
    }

    /** Logs that a player timed out without responding to a challenge. */
    public static void logTimeout(UUID uuid, String username) {
        logFlags(uuid, username, List.of("ANTICHEAT_TIMEOUT"));
    }

    /** Logs a ban issued by an admin via a command. */
    public static void logEnforceBan(String username, String flagDisplay, String adminName) {
        appendLine(root().resolve("bans.log"),
                "Player " + username + " banned: \"" + flagDisplay + "\" Banwave issued by " + adminName);
    }

    // --- Read helpers (used by commands) ---

    /** Returns UUID → detail (empty string if no detail) for all entries in the given check log. */
    public static Map<UUID, String> readCheckEntries(String checkName) {
        Path file = root().resolve("checks").resolve(sanitize(checkName) + ".log");
        if (!Files.exists(file)) return Collections.emptyMap();
        Map<UUID, String> entries = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.strip();
                if (trimmed.isBlank()) continue;
                int sep = trimmed.indexOf(": ");
                String uuidStr = sep != -1 ? trimmed.substring(0, sep) : trimmed;
                String detail  = sep != -1 ? trimmed.substring(sep + 2) : "";
                try { entries.put(UUID.fromString(uuidStr), detail); } catch (IllegalArgumentException ignored) {}
            }
        } catch (IOException e) {
            LOGGER.error("[XaiAC] Failed to read check log {}: {}", file, e.getMessage());
        }
        return entries;
    }

    public static Set<UUID> readCheckUuids(String checkName) {
        return readCheckEntries(checkName).keySet();
    }

    /** Returns the last known player name from their player log, or null if unavailable. */
    public static String readPlayerName(UUID uuid) {
        Path file = root().resolve("players").resolve(uuid + ".jsonl");
        if (!Files.exists(file)) return null;
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i).strip();
                if (line.isBlank()) continue;
                Matcher m = NAME_PATTERN.matcher(line);
                if (m.find()) return m.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
                break;
            }
        } catch (IOException e) {
            LOGGER.error("[XaiAC] Failed to read player log {}: {}", file, e.getMessage());
        }
        return null;
    }

    /** Returns all offense entries for a player, newest-first, as formatted display strings. */
    public static List<String> readPlayerOffenses(UUID uuid) {
        Path file = root().resolve("players").resolve(uuid + ".jsonl");
        if (!Files.exists(file)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        Pattern entry = Pattern.compile("\"t\":\"([^\"]+)\".*?\"f\":\\[([^]]*)]");
        Instant now = Instant.now();
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i).strip();
                if (line.isBlank()) continue;
                Matcher m = entry.matcher(line);
                if (!m.find()) continue;
                String flags = m.group(2).replace("\"", "").replace(",", ", ");
                String formatted;
                try {
                    Instant ts = Instant.parse(m.group(1));
                    double totalHours = (now.toEpochMilli() - ts.toEpochMilli()) / 3_600_000.0;
                    long days = (long) (totalHours / 24);
                    double remHours = totalHours - days * 24;
                    formatted = days == 0
                        ? String.format("%.1f Hours ago", remHours)
                        : days + (days == 1 ? " Day" : " Days") + String.format(" and %.1f Hours ago", remHours);
                } catch (Exception e) {
                    formatted = m.group(1);
                }
                result.add(formatted + " - " + flags);
            }
        } catch (IOException e) {
            LOGGER.error("[XaiAC] Failed to read player log {}: {}", file, e.getMessage());
        }
        return result;
    }

    public static List<String> listCheckNames() {
        Path checksDir = root().resolve("checks");
        if (!Files.isDirectory(checksDir)) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(checksDir, "*.log")) {
            stream.forEach(p -> {
                String fname = p.getFileName().toString();
                names.add(fname.substring(0, fname.length() - 4));
            });
        } catch (IOException e) {
            LOGGER.error("[XaiAC] Failed to list check logs: {}", e.getMessage());
        }
        return names;
    }

    // --- Private helpers ---

    private static void appendPlayerLog(UUID uuid, String username, String timestamp, List<String> flags) {
        Path file = root().resolve("players").resolve(uuid + ".jsonl");
        StringBuilder sb = new StringBuilder("{\"t\":\"").append(timestamp)
                .append("\",\"n\":\"").append(escapeJson(username))
                .append("\",\"f\":[");
        for (int i = 0; i < flags.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(escapeJson(flags.get(i))).append('"');
        }
        sb.append("]}");
        appendLine(file, sb.toString());
    }

    private static void appendCheckLogs(UUID uuid, List<String> flags) {
        Path checksDir = root().resolve("checks");
        for (String flag : flags) {
            String checkName = sanitize(flag.contains(":") ? flag.substring(0, flag.indexOf(':')) : flag);
            String detail    = flag.contains(":") ? flag.substring(flag.indexOf(':') + 1) : null;
            String line      = detail != null ? uuid + ": " + detail : uuid.toString();
            appendLine(checksDir.resolve(checkName + ".log"), line);
        }
    }

    public static void setRoot(Path root) {
        rootDir = root;
    }

    private static synchronized void appendLine(Path file, String line) {
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(file,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(line);
                w.newLine();
            }
        } catch (IOException e) {
            LOGGER.error("[XaiAC] Failed to write flag log to {}: {}", file, e.getMessage());
        }
    }

    private static Path root() {
        return rootDir;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static final Pattern NAME_PATTERN = Pattern.compile("\"n\":\"((?:[^\"\\\\]|\\\\.)*)\"");
}
