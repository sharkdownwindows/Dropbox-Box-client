package vn.edu.usth.cloudclient.data;

import java.io.IOException;
import vn.edu.usth.cloudclient.model.FolderPage;

/** Synchronous contract. Caller must run I/O off the main thread. */
public interface CloudRepository {
    String rootId();
    /** cursor == null starts a new listing; otherwise continue that folder's listing. */
    FolderPage listFolder(String folderId, String cursor) throws IOException;
}
