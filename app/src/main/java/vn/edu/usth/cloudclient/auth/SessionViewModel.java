package vn.edu.usth.cloudclient.auth;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import vn.edu.usth.cloudclient.CloudClientApplication;
import vn.edu.usth.cloudclient.BuildConfig;
import vn.edu.usth.cloudclient.R;
import vn.edu.usth.cloudclient.data.CloudRepository;
import vn.edu.usth.cloudclient.data.dropbox.DropboxCloudRepository;
import vn.edu.usth.cloudclient.data.fake.FakeCloudRepository;

public final class SessionViewModel extends AndroidViewModel {
    public static final class State {
        public final CloudRepository repository;
        public final boolean demo;
        public final boolean busy;
        public final int message;
        public final String id;
        State(CloudRepository repository, boolean demo, boolean busy, int message) {
            this.repository = repository; this.demo = demo; this.busy = busy; this.message = message;
            id = UUID.randomUUID().toString();
        }
    }

    private final CredentialVault vault;
    private final SavedStateHandle saved;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final ExecutorService io;
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean cleared;

    public SessionViewModel(@NonNull Application app, SavedStateHandle saved) {
        super(app);
        this.saved = saved;
        CloudClientApplication application = (CloudClientApplication) app;
        vault = application.vault;
        io = application.sessionIo;
        long epoch = vault.invalidate();
        state.setValue(new State(null, false, true, R.string.session_loading));
        io.execute(() -> {
            try {
                String json = vault.read(epoch);
                if (json == null) { publish(epoch, signedOut(R.string.login_intro)); return; }
                DbxCredential credential = DbxCredential.Reader.readFully(json);
                if (BuildConfig.DROPBOX_APP_KEY.isEmpty() || !BuildConfig.DROPBOX_APP_KEY.equals(credential.getAppKey())) {
                    vault.clear(epoch);
                    publish(epoch, signedOut(R.string.configure_key));
                } else publish(epoch, connected(credential, epoch));
            } catch (Exception e) {
                try { vault.clear(epoch); }
                catch (IOException clearFailure) { publish(epoch, signedOut(R.string.session_clear_error)); return; }
                publish(epoch, signedOut(R.string.session_restore_error));
            }
        });
    }

    public static DbxRequestConfig requestConfig() {
        return DbxRequestConfig.newBuilder("usth-cloud-client/0.2").build();
    }
    public LiveData<State> state() { return state; }
    public boolean authPending() { return Boolean.TRUE.equals(saved.get("authPending")); }
    public void setAuthPending(boolean pending) { saved.set("authPending", pending); }
    public void showAuthError(int message) {
        setAuthPending(false);
        state.setValue(signedOut(message));
    }

    public void acceptCredential(DbxCredential credential) {
        setAuthPending(false);
        long epoch = vault.invalidate();
        state.setValue(new State(null, false, true, R.string.session_loading));
        io.execute(() -> {
            try {
                if (credential.getRefreshToken() == null ||
                    !BuildConfig.DROPBOX_APP_KEY.equals(credential.getAppKey())) throw new IOException("Invalid OAuth result");
                if (vault.save(epoch, credential.toString())) publish(epoch, connected(credential, epoch));
            } catch (IOException e) { publish(epoch, signedOut(R.string.session_save_error)); }
        });
    }

    public void useDemo() {
        if (authPending()) return;
        vault.invalidate();
        state.setValue(new State(new FakeCloudRepository(), true, false, R.string.demo_only));
    }

    public void logout() {
        setAuthPending(false);
        long epoch = vault.invalidate();
        state.setValue(new State(null, false, true, R.string.session_loading));
        io.execute(() -> {
            try { vault.clearForLogout(); publish(epoch, signedOut(R.string.login_intro)); }
            catch (IOException e) { publish(epoch, signedOut(R.string.session_clear_error)); }
        });
    }

    private State connected(DbxCredential credential, long epoch) {
        return new State(new DropboxCloudRepository(new DbxClientV2(requestConfig(), credential),
            credential, vault, epoch), false, false, R.string.dropbox_connected);
    }
    private State signedOut(int message) { return new State(null, false, false, message); }
    private void publish(long epoch, State value) {
        main.post(() -> { if (!cleared && vault.isCurrent(epoch)) state.setValue(value); });
    }
    @Override protected void onCleared() {
        cleared = true;
        // Do not cancel pending disk logout. The application owns the ordered queue.
        main.removeCallbacksAndMessages(null);
    }
}
