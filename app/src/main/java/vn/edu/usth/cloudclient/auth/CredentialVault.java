package vn.edu.usth.cloudclient.auth;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/** Serializes disk changes and rejects writes from a session invalidated by logout. */
public final class CredentialVault {
    public interface Store {
        String read() throws IOException;
        void write(String value) throws IOException;
        void clear() throws IOException;
    }

    private final Store store;
    private final AtomicLong generation = new AtomicLong();

    public CredentialVault(Store store) { this.store = store; }
    // Called by UI: never wait for slow Keystore/disk operations holding this monitor.
    public long invalidate() { return generation.incrementAndGet(); }
    public boolean isCurrent(long expected) { return generation.get() == expected; }
    public synchronized String read(long expected) throws IOException {
        return isCurrent(expected) ? store.read() : null;
    }
    public synchronized boolean save(long expected, String value) throws IOException {
        if (!isCurrent(expected)) return false;
        store.write(value);
        return isCurrent(expected);
    }
    public synchronized void clear(long expected) throws IOException {
        if (isCurrent(expected)) store.clear();
    }
    /** Logout is ordered on the application session queue, before any later login/restore. */
    public synchronized void clearForLogout() throws IOException { store.clear(); }
}
