package vn.edu.usth.cloudclient.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;

public final class FolderPage {
    public final List<CloudItem> items;
    /** Null means no next page. Provider cursors are opaque. */
    public final String nextCursor;

    public FolderPage(List<CloudItem> items, String nextCursor) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.nextCursor = nextCursor;
    }

    /** Preserve order and replace metadata when the same stable ID appears twice. */
    public List<CloudItem> appendTo(List<CloudItem> previous) {
        LinkedHashMap<String, CloudItem> merged = new LinkedHashMap<>();
        for (CloudItem item : previous) merged.put(item.id, item);
        for (CloudItem item : items) merged.put(item.id, item);
        return new ArrayList<>(merged.values());
    }
}
