package vn.edu.usth.cloudclient.auth;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Versioned AES-GCM ciphertext, excluded from backup. All callers use a worker. */
public final class EncryptedCredentialStore implements CredentialVault.Store {
    private static final String ALIAS = "cloudclient.dropbox.v1";
    private static final byte VERSION = 1;
    private static final int MAX_BYTES = 65536;
    private final AtomicFile file;

    public EncryptedCredentialStore(Context context) {
        file = new AtomicFile(new File(context.getNoBackupFilesDir(), "dropbox-session.bin"));
    }

    private KeyStore keyStore() throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        return store;
    }

    private SecretKey key(boolean create) throws GeneralSecurityException, IOException {
        KeyStore store = keyStore();
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        if (!create) throw new GeneralSecurityException("Missing session key");
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build());
        return generator.generateKey();
    }

    @Override public String read() throws IOException {
        byte[] encoded;
        try (FileInputStream input = file.openRead(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (bytes.size() + count > MAX_BYTES) throw new IOException("Session file too large");
                bytes.write(buffer, 0, count);
            }
            encoded = bytes.toByteArray();
        } catch (FileNotFoundException absent) { return null; }
        if (encoded.length < 30 || encoded[0] != VERSION) throw new IOException("Invalid session format");
        ByteBuffer data = ByteBuffer.wrap(encoded);
        data.get();
        int ivLength = Byte.toUnsignedInt(data.get());
        if (ivLength != 12 || data.remaining() < ivLength + 16) throw new IOException("Invalid session IV");
        byte[] iv = new byte[ivLength]; data.get(iv);
        byte[] ciphertext = new byte[data.remaining()]; data.get(ciphertext);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(false), new GCMParameterSpec(128, iv));
            cipher.updateAAD(new byte[]{VERSION});
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) { throw new IOException("Cannot decrypt session", e); }
    }

    @Override public void write(String value) throws IOException {
        byte[] encoded;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(true)); // Keystore generates a fresh IV.
            cipher.updateAAD(new byte[]{VERSION});
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            if (iv.length != 12 || ciphertext.length + 14 > MAX_BYTES) throw new IOException("Invalid session size");
            encoded = ByteBuffer.allocate(2 + iv.length + ciphertext.length)
                .put(VERSION).put((byte) iv.length).put(iv).put(ciphertext).array();
        } catch (GeneralSecurityException e) { throw new IOException("Cannot encrypt session", e); }
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            output.write(encoded);
            file.finishWrite(output);
        } catch (IOException e) {
            file.failWrite(output);
            throw e;
        }
    }

    @Override public void clear() throws IOException {
        try { keyStore().deleteEntry(ALIAS); }
        catch (GeneralSecurityException e) { throw new IOException("Cannot clear session key", e); }
        file.delete(); // Any undeleted ciphertext is unusable after deleting its key.
    }
}
