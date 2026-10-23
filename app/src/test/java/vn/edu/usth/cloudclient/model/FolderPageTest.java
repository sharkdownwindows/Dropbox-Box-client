package vn.edu.usth.cloudclient.model;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class FolderPageTest {
    @Test public void repeatedIdUpdatesMetadataWithoutDuplicatingOrReordering() {
        CloudItem a = new CloudItem("id:a", "old.txt", false, 1);
        CloudItem b = new CloudItem("id:b", "B", true, 0);
        CloudItem updated = new CloudItem("id:a", "renamed.txt", false, 42);
        CloudItem c = new CloudItem("id:c", "C", true, 0);
        List<CloudItem> previous = Arrays.asList(a, b);
        List<CloudItem> result = new FolderPage(Arrays.asList(updated, c), null).appendTo(previous);
        assertEquals(3, result.size());
        assertSame(updated, result.get(0));
        assertSame(b, result.get(1));
        assertSame(c, result.get(2));
        assertSame(a, previous.get(0)); // Do not mutate the prior UI snapshot.
    }
}
