package vn.edu.usth.cloudclient.ui.files;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import vn.edu.usth.cloudclient.data.CloudRepository;
import vn.edu.usth.cloudclient.data.CloudException;
import vn.edu.usth.cloudclient.model.CloudItem;
import vn.edu.usth.cloudclient.model.FolderPage;

public final class FilesViewModel extends ViewModel {
    private CloudRepository repository;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<FilesState> state = new MutableLiveData<>();
    private final SavedStateHandle saved;
    private final ArrayList<String> parents;
    private final ArrayList<String> parentNames;
    private String folder;
    private String folderName;
    private int generation;
    private Future<?> pending;

    public FilesViewModel(SavedStateHandle saved) {
        this.saved = saved;
        folder = saved.contains("folder") ? saved.get("folder") : "";
        ArrayList<String> restored = saved.get("parents");
        parents = restored == null ? new ArrayList<>() : new ArrayList<>(restored);
        folderName = saved.contains("folderName") ? saved.get("folderName") : "";
        ArrayList<String> restoredNames = saved.get("parentNames");
        parentNames = restoredNames == null ? new ArrayList<>() : new ArrayList<>(restoredNames);
        if (parentNames.size() != parents.size()) {
            parents.clear(); parentNames.clear(); folder = ""; folderName = "";
        }
        state.setValue(new FilesState(folder, Collections.emptyList(), null, false, null));
    }

    public void connect(CloudRepository next, String sessionId) {
        if (repository == next) return;
        generation++;
        if (pending != null) pending.cancel(true);
        repository = next;
        if (next == null) {
            parents.clear();
            parentNames.clear();
            folder = ""; folderName = "";
            state.setValue(new FilesState("", Collections.emptyList(), null, false, null));
            return;
        }
        if (!sessionId.equals(saved.get("session"))) {
            folder = next.rootId();
            folderName = "";
            parents.clear();
            parentNames.clear();
            saved.set("session", sessionId);
            persistLocation();
        }
        refresh();
    }

    public LiveData<FilesState> state() { return state; }
    public boolean canGoUp() { return !parents.isEmpty(); }
    public String folderName() { return folderName; }

    public void open(CloudItem item) {
        if (!item.folder) return;
        parents.add(folder);
        parentNames.add(folderName);
        folder = item.id;
        folderName = item.name;
        persistLocation();
        refresh();
    }

    public void up() {
        if (!canGoUp()) return;
        folder = parents.remove(parents.size() - 1);
        folderName = parentNames.remove(parentNames.size() - 1);
        persistLocation();
        refresh();
    }

    private void persistLocation() {
        saved.set("folder", folder);
        saved.set("parents", new ArrayList<>(parents));
        saved.set("folderName", folderName);
        saved.set("parentNames", new ArrayList<>(parentNames));
    }

    public void refresh() { load(null, Collections.emptyList()); }

    public void loadMore() {
        FilesState current = state.getValue();
        if (current != null && !current.loading && current.nextCursor != null) {
            load(current.nextCursor, current.items);
        }
    }

    private void load(String cursor, List<CloudItem> previous) {
        CloudRepository source = repository;
        if (source == null) return;
        int requestGeneration = ++generation;
        if (pending != null) pending.cancel(true);
        String requestedFolder = folder;
        List<CloudItem> snapshot = new ArrayList<>(previous);
        state.setValue(new FilesState(folder, snapshot, cursor, true, null));
        pending = io.submit(() -> {
            try {
                FolderPage page = source.listFolder(requestedFolder, cursor);
                List<CloudItem> combined = page.appendTo(snapshot);
                main.post(() -> {
                    // A late response from a previous folder must not overwrite this one.
                    if (requestGeneration == generation) {
                        state.setValue(new FilesState(requestedFolder, combined, page.nextCursor, false, null));
                    }
                });
            } catch (IOException e) {
                CloudException.Kind kind = e instanceof CloudException
                    ? ((CloudException) e).kind : CloudException.Kind.NETWORK;
                main.post(() -> {
                    if (requestGeneration == generation) {
                        state.setValue(new FilesState(requestedFolder, snapshot, cursor, false, kind));
                    }
                });
            }
        });
    }

    @Override protected void onCleared() {
        generation++;
        if (pending != null) pending.cancel(true);
        io.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }
}
