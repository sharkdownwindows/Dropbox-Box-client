package vn.edu.usth.cloudclient;

import android.app.Application;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.usth.cloudclient.auth.CredentialVault;
import vn.edu.usth.cloudclient.auth.EncryptedCredentialStore;

/** One vault and ordered session-I/O queue per process, across Activity recreation. */
public final class CloudClientApplication extends Application {
    public final ExecutorService sessionIo = Executors.newSingleThreadExecutor();
    public CredentialVault vault;

    @Override public void onCreate() {
        super.onCreate();
        vault = new CredentialVault(new EncryptedCredentialStore(this));
    }
}
