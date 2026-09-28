package xai.xaiacserver.handshake;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class SessionManager {

    private static final HashMap<UUID, Session> sessions = new HashMap<>();

    public static void put(UUID uuid, Session session) {
        sessions.put(uuid, session);
    }

    public static Session get(UUID uuid) {
        return sessions.get(uuid);
    }

    public static void remove(UUID uuid) {
        sessions.remove(uuid);
    }

    public static Collection<Session> all() {
        return sessions.values();
    }

    public static boolean isPending(UUID uuid) {
        Session s = sessions.get(uuid);
        if  (s == null) {
            return false;
        }
        return (s.state == Session.State.PENDING);
    }
}
