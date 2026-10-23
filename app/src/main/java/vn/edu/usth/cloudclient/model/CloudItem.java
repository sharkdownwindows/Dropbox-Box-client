package vn.edu.usth.cloudclient.model;

import java.util.Objects;

/** Metadata only: file bytes never belong in a list row. */
public final class CloudItem {
    public final String id;
    public final String name;
    public final boolean folder;
    public final long sizeBytes;

    public CloudItem(String id, String name, boolean folder, long sizeBytes) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.folder = folder;
        this.sizeBytes = sizeBytes;
    }
}
