package vn.edu.usth.cloudclient.data.fake;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import vn.edu.usth.cloudclient.data.CloudRepository;
import vn.edu.usth.cloudclient.model.CloudItem;
import vn.edu.usth.cloudclient.model.FolderPage;

/** Teaching fixture, not a cache and not a Dropbox connection. */
public final class FakeCloudRepository implements CloudRepository {
    @Override public String rootId() { return ""; }

    @Override public FolderPage listFolder(String folderId, String cursor) throws IOException {
        List<CloudItem> items;
        switch (folderId) {
            case "":
                items = Arrays.asList(
                    new CloudItem("demo:course", "Course", true, 0),
                    new CloudItem("demo:empty", "Empty folder", true, 0),
                    new CloudItem("demo:error", "Simulated error", true, 0),
                    new CloudItem("demo:readme", "readme.txt", false, 512),
                    new CloudItem("demo:photo", "photo.jpg", false, 204800));
                break;
            case "demo:course":
                items = Collections.singletonList(
                    new CloudItem("demo:notes", "mobile-notes.pdf", false, 4096));
                break;
            case "demo:empty": items = Collections.emptyList(); break;
            case "demo:error": throw new IOException("Simulated connection failure");
            default: throw new IOException("Unknown folder");
        }
        // The fixture binds its cursor to a folder, like a real provider should.
        int offset = 0;
        if (cursor != null) {
            String prefix = folderId + "|";
            if (!cursor.startsWith(prefix)) throw new IOException("Cursor belongs to another folder");
            try { offset = Integer.parseInt(cursor.substring(prefix.length())); }
            catch (NumberFormatException e) { throw new IOException("Invalid cursor", e); }
            if (offset < 0 || offset >= items.size()) throw new IOException("Expired cursor");
        }
        int end = Math.min(offset + 3, items.size());
        return new FolderPage(items.subList(offset, end),
            end < items.size() ? folderId + "|" + end : null);
    }
}
