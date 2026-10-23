package vn.edu.usth.cloudclient.ui.files;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import vn.edu.usth.cloudclient.R;
import vn.edu.usth.cloudclient.databinding.FragmentFilesBinding;
import vn.edu.usth.cloudclient.auth.SessionViewModel;
import vn.edu.usth.cloudclient.data.CloudException;

public final class FilesFragment extends Fragment {
    private FragmentFilesBinding binding;
    private boolean demo;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                @Nullable ViewGroup container, @Nullable Bundle saved) {
        binding = FragmentFilesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        FilesViewModel model = new ViewModelProvider(this).get(FilesViewModel.class);
        SessionViewModel session = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        session.state().observe(getViewLifecycleOwner(), value -> {
            demo = value.demo;
            model.connect(value.repository, value.id);
        });
        CloudItemAdapter adapter = new CloudItemAdapter(item -> {
            if (item.folder) model.open(item);
            else Toast.makeText(requireContext(), R.string.file_not_connected, Toast.LENGTH_SHORT).show();
        });
        binding.files.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.files.setAdapter(adapter);
        binding.refresh.setOnClickListener(v -> model.refresh());
        binding.up.setOnClickListener(v -> model.up());
        binding.more.setOnClickListener(v -> model.loadMore());
        binding.reconnect.setOnClickListener(v -> session.logout());
        OnBackPressedCallback back = new OnBackPressedCallback(model.canGoUp()) {
            @Override public void handleOnBackPressed() { model.up(); }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), back);
        model.state().observe(getViewLifecycleOwner(), state -> {
            binding.path.setText(state.folderId.isEmpty()
                ? getString(demo ? R.string.root_folder : R.string.dropbox_root) : model.folderName());
            binding.progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            binding.status.setText(state.failed ? errorMessage(state.error)
                : state.items.isEmpty() && !state.loading && state.nextCursor == null ? R.string.empty_folder
                : state.items.isEmpty() && !state.loading ? R.string.more_available
                : demo ? R.string.demo_only : R.string.dropbox_connected);
            binding.reconnect.setVisibility(state.error == CloudException.Kind.AUTH ||
                state.error == CloudException.Kind.PERMISSION ? View.VISIBLE : View.GONE);
            binding.up.setEnabled(model.canGoUp());
            back.setEnabled(model.canGoUp());
            binding.more.setVisibility(state.nextCursor == null ? View.GONE : View.VISIBLE);
            binding.more.setEnabled(!state.loading);
            binding.refresh.setEnabled(!state.loading);
            adapter.submitList(state.items);
        });
    }

    private int errorMessage(CloudException.Kind kind) {
        switch (kind) {
            case AUTH: return R.string.error_auth;
            case PERMISSION: return R.string.error_permission;
            case NOT_FOUND: return R.string.error_not_found;
            case RATE_LIMIT: return R.string.error_rate_limit;
            case SERVICE: return R.string.error_service;
            case STORAGE: return R.string.session_save_error;
            default: return R.string.load_error;
        }
    }

    @Override public void onDestroyView() {
        binding.files.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }
}
