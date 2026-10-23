package vn.edu.usth.cloudclient.auth;

import java.io.IOException;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class CredentialVaultTest {
    private static final class MemoryStore implements CredentialVault.Store {
        String value;
        boolean failWrite;
        @Override public String read() { return value; }
        @Override public void write(String next) throws IOException {
            if (failWrite) throw new IOException("Simulated full storage");
            value = next;
        }
        @Override public void clear() { value = null; }
    }

    @Test public void lateRefreshCannotRestoreLoggedOutCredential() throws IOException {
        MemoryStore store = new MemoryStore();
        CredentialVault vault = new CredentialVault(store);
        long loggedIn = vault.invalidate();
        assertTrue(vault.save(loggedIn, "initial"));
        vault.invalidate();
        vault.clearForLogout();
        assertFalse(vault.save(loggedIn, "late-refresh"));
        assertNull(store.value);
        assertNull(vault.read(loggedIn));
    }

    @Test public void oldRestoreCannotClearNewAccount() throws IOException {
        MemoryStore store = new MemoryStore();
        CredentialVault vault = new CredentialVault(store);
        long old = vault.invalidate();
        long current = vault.invalidate();
        vault.save(current, "new-account");
        vault.clear(old);
        assertEquals("new-account", vault.read(current));
    }

    @Test public void queuedLogoutStillClearsBeforeLaterRestore() throws IOException {
        MemoryStore store = new MemoryStore();
        CredentialVault vault = new CredentialVault(store);
        vault.save(vault.invalidate(), "old-account");
        vault.invalidate(); // Logout is queued on application sessionIo.
        long recreatedScreen = vault.invalidate(); // Activity recreated before disk clear.
        vault.clearForLogout(); // Ordered before the queued restore.
        assertNull(vault.read(recreatedScreen));
    }

    @Test public void currentSessionCanPersistAndReloadRefresh() throws IOException {
        MemoryStore store = new MemoryStore();
        CredentialVault vault = new CredentialVault(store);
        long session = vault.invalidate();
        vault.save(session, "initial");
        vault.save(session, "refreshed");
        assertEquals("refreshed", vault.read(session));
        vault.clear(session);
        assertNull(vault.read(session));
    }

    @Test(expected = IOException.class) public void diskFailureIsNotReportedAsSuccess() throws IOException {
        MemoryStore store = new MemoryStore();
        store.failWrite = true;
        CredentialVault vault = new CredentialVault(store);
        vault.save(vault.invalidate(), "credential");
    }

    @Test public void invalidationDoesNotWaitForBlockedStorageWrite() throws Exception {
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CredentialVault vault = new CredentialVault(new CredentialVault.Store() {
            public String read() { return null; }
            public void clear() {}
            public void write(String value) throws IOException {
                writing.countDown();
                try { if (!release.await(10, TimeUnit.SECONDS)) throw new IOException("Test timeout"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
            }
        });
        ExecutorService workers = Executors.newFixedThreadPool(2);
        long old = vault.invalidate();
        try {
            Future<Boolean> saving = workers.submit(() -> vault.save(old, "credential"));
            assertTrue(writing.await(5, TimeUnit.SECONDS));
            // A UI invalidation must finish while the disk write is still blocked.
            workers.submit(vault::invalidate).get(5, TimeUnit.SECONDS);
            release.countDown();
            assertFalse(saving.get(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            workers.shutdownNow();
        }
    }
}
