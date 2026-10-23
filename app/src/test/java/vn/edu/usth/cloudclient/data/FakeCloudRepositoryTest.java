package vn.edu.usth.cloudclient.data;

import org.junit.Test;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import vn.edu.usth.cloudclient.data.fake.FakeCloudRepository;
import vn.edu.usth.cloudclient.model.CloudItem;
import vn.edu.usth.cloudclient.model.FolderPage;
import static org.junit.Assert.*;

public final class FakeCloudRepositoryTest {
    private final FakeCloudRepository repo = new FakeCloudRepository();

    @Test public void paginationReturnsEveryItemOnce() throws IOException {
        Set<String> ids = new HashSet<>();
        String cursor = null;
        int pages = 0;
        do {
            FolderPage page = repo.listFolder(repo.rootId(), cursor);
            for (CloudItem item : page.items) assertTrue(ids.add(item.id));
            cursor = page.nextCursor;
            assertTrue(++pages <= 3);
        } while (cursor != null);
        assertEquals(5, ids.size());
        assertEquals(2, pages);
    }

    @Test public void emptyFolderIsSuccess() throws IOException {
        FolderPage page = repo.listFolder("demo:empty", null);
        assertTrue(page.items.isEmpty());
        assertNull(page.nextCursor);
    }

    @Test(expected = IOException.class) public void failureIsNotAnEmptyFolder() throws IOException {
        repo.listFolder("demo:error", null);
    }

    @Test(expected = IOException.class) public void cursorCannotCrossFolders() throws IOException {
        String cursor = repo.listFolder("", null).nextCursor;
        repo.listFolder("demo:course", cursor);
    }

    @Test(expected = IOException.class) public void invalidCursorFailsExplicitly() throws IOException {
        repo.listFolder("", "|999");
    }

    @Test(expected = UnsupportedOperationException.class) public void pageCannotBeMutatedByUi() throws IOException {
        repo.listFolder("", null).items.clear();
    }
}
