package vn.edu.usth.cloudclient.ui.files;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vn.edu.usth.cloudclient.model.CloudItem;
import vn.edu.usth.cloudclient.data.CloudException;

public final class FilesState {
    public final String folderId;
    public final List<CloudItem> items;
    public final String nextCursor;
    public final boolean loading;
    public final boolean failed;
    public final CloudException.Kind error;

    public FilesState(String folderId, List<CloudItem> items, String nextCursor,
                      boolean loading, CloudException.Kind error) {
        this.folderId = folderId;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.nextCursor = nextCursor;
        this.loading = loading;
        this.failed = error != null;
        this.error = error;
    }
}
