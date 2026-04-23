package xai.xaiacserver.handshake;

public class Session {

    public enum State { PENDING, ANSWERED }

    public final String username;

    public String nonce;
    public byte[] sessionKey;
    public State state = State.PENDING;

    // Tick countdowns — decremented each server tick on the main thread.
    public int kickTicksRemaining;
    public int recheckTicksRemaining;

    public Session(String username, String nonce, byte[] sessionKey, int kickTicksRemaining, int recheckTicksRemaining) {
        this.username              = username;
        this.nonce                 = nonce;
        this.sessionKey            = sessionKey;
        this.kickTicksRemaining    = kickTicksRemaining;
        this.recheckTicksRemaining = recheckTicksRemaining;
    }
}
