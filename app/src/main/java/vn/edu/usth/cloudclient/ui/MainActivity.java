package vn.edu.usth.cloudclient.ui;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.dropbox.core.android.Auth;
import com.dropbox.core.oauth.DbxCredential;
import java.util.Collections;
import vn.edu.usth.cloudclient.BuildConfig;
import vn.edu.usth.cloudclient.R;
import vn.edu.usth.cloudclient.auth.SessionViewModel;
import vn.edu.usth.cloudclient.databinding.ActivityMainBinding;
import vn.edu.usth.cloudclient.ui.files.FilesFragment;

public final class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private SessionViewModel session;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        session = new ViewModelProvider(this).get(SessionViewModel.class);
        session.state().observe(this, this::renderSession);
        binding.login.setOnClickListener(v -> login());
        binding.demo.setOnClickListener(v -> session.useDemo());
        binding.logout.setOnClickListener(v -> session.logout());
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new FilesFragment()).commit();
        }
    }

    private void renderSession(SessionViewModel.State state) {
        boolean connected = state.repository != null;
        binding.sessionStatus.setText(state.message);
        binding.authProgress.setVisibility(state.busy ? View.VISIBLE : View.GONE);
        binding.loginControls.setVisibility(connected ? View.GONE : View.VISIBLE);
        boolean canLogin = !state.busy && !session.authPending() && state.message != R.string.session_clear_error;
        binding.login.setEnabled(canLogin);
        binding.demo.setEnabled(canLogin);
        binding.logout.setVisibility(connected ? View.VISIBLE : View.GONE);
        binding.fragmentContainer.setVisibility(connected ? View.VISIBLE : View.GONE);
    }

    private void login() {
        if (BuildConfig.DROPBOX_APP_KEY.isEmpty()) {
            session.showAuthError(R.string.configure_key);
            return;
        }
        session.setAuthPending(true);
        binding.login.setEnabled(false);
        binding.demo.setEnabled(false);
        try {
            Auth.startOAuth2PKCE(this, BuildConfig.DROPBOX_APP_KEY, SessionViewModel.requestConfig(),
                Collections.singletonList("files.metadata.read"));
        } catch (IllegalStateException | IllegalArgumentException e) {
            session.showAuthError(R.string.auth_start_error);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (session.authPending()) {
            session.setAuthPending(false);
            DbxCredential credential = Auth.getDbxCredential();
            if (credential == null) session.showAuthError(R.string.auth_cancelled);
            else session.acceptCredential(credential);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
