package vn.edu.usth.cloudclient.data;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.oauth.DbxOAuthError;
import com.dropbox.core.oauth.DbxOAuthException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;
import vn.edu.usth.cloudclient.auth.CredentialVault;
import vn.edu.usth.cloudclient.data.dropbox.DropboxCloudRepository;
import vn.edu.usth.cloudclient.model.FolderPage;
import static org.junit.Assert.*;

/** Real SDK serialization/parsing, fake HTTP transport. No Dropbox account or network. */
public final class DropboxCloudRepositoryTest {
    private static final String EMPTY_PAGE = "{\"entries\":[],\"cursor\":\"opaque-next\",\"has_more\":false}";
    private final StubHttp http = new StubHttp();
    private final MemoryStore store = new MemoryStore();
    private final CredentialVault vault = new CredentialVault(store);
    private final long epoch = vault.invalidate();
    private final DbxCredential credential = new DbxCredential("test-only-not-a-token");
    private final DropboxCloudRepository repo = new DropboxCloudRepository(
        new DbxClientV2(DbxRequestConfig.newBuilder("unit-test").withHttpRequestor(http).build(), credential),
        credential, vault, epoch);

    @Test public void initialListUsesRootAndPersistsCredential() throws IOException {
        http.json = EMPTY_PAGE;
        FolderPage page = repo.listFolder(repo.rootId(), null);
        assertTrue(http.url.endsWith("/2/files/list_folder"));
        assertTrue(http.body.contains("\"path\":\"\""));
        assertTrue(http.body.contains("\"limit\":100"));
        assertTrue(page.items.isEmpty());
        assertNull(page.nextCursor);
        assertEquals(credential.toString(), store.value);
    }

    @Test public void continuationUsesOpaqueCursorWithoutReconstructingPath() throws IOException {
        http.json = EMPTY_PAGE;
        repo.listFolder("id:folder", "opaque/cursor=123");
        assertTrue(http.url.endsWith("/2/files/list_folder/continue"));
        assertEquals("{\"cursor\":\"opaque/cursor=123\"}", http.body);
    }

    @Test public void sdkMetadataMapsIdsAndKeepsCursorEvenForEmptyPage() {
        FolderPage mapped = DropboxCloudRepository.mapPage(new ListFolderResult(Arrays.asList(
            new FolderMetadata("Course", "id:folder"),
            new FileMetadata("note.txt", "id:file", new Date(0), new Date(0), "123456789", 42),
            new DeletedMetadata("gone.txt")), "next", true));
        assertEquals(2, mapped.items.size());
        assertTrue(mapped.items.get(0).folder);
        assertEquals("id:folder", mapped.items.get(0).id);
        assertFalse(mapped.items.get(1).folder);
        assertEquals(42, mapped.items.get(1).sizeBytes);
        assertEquals("next", mapped.nextCursor);
        assertEquals("next", DropboxCloudRepository.mapPage(
            new ListFolderResult(Collections.emptyList(), "next", true)).nextCursor);
    }

    @Test public void logoutDuringResponseDiscardsPageAndCredential() throws IOException {
        http.json = EMPTY_PAGE;
        http.beforeResponse = () -> { vault.invalidate(); vault.clearForLogout(); };
        expectError(CloudException.Kind.AUTH);
        assertNull(store.value);
    }

    @Test public void invalidatedSessionDoesNotSendRequest() throws IOException {
        vault.invalidate();
        expectError(CloudException.Kind.AUTH);
        assertNull(http.url);
    }

    @Test public void revokedAccessIsAnAuthError() throws IOException {
        http.status = 401;
        http.json = "{\"error_summary\":\"invalid_access_token/\",\"error\":{\".tag\":\"invalid_access_token\"}}";
        expectError(CloudException.Kind.AUTH);
    }

    @Test public void missingScopeIsPermissionError() throws IOException {
        http.status = 401;
        http.json = "{\"error_summary\":\"missing_scope/\",\"error\":{\".tag\":\"missing_scope\",\"required_scope\":\"files.metadata.read\"}}";
        expectError(CloudException.Kind.PERMISSION);
    }

    @Test public void revokedRefreshTokenRequiresLogin() {
        assertEquals(CloudException.Kind.AUTH, DropboxCloudRepository.mapError(
            new DbxOAuthException("test", new DbxOAuthError("invalid_grant", "revoked"))).kind);
    }

    @Test public void rateLimitDoesNotBecomeEmptySuccess() throws IOException {
        http.status = 429;
        http.json = "{\"error_summary\":\"too_many_requests/\",\"error\":{\"reason\":{\".tag\":\"too_many_requests\"},\"retry_after\":1}}";
        expectError(CloudException.Kind.RATE_LIMIT);
    }

    @Test public void missingFolderHasDistinctRecovery() throws IOException {
        http.status = 409;
        http.json = "{\"error_summary\":\"path/not_found/\",\"error\":{\".tag\":\"path\",\"path\":{\".tag\":\"not_found\"}}}";
        expectError(CloudException.Kind.NOT_FOUND);
    }

    @Test public void storageFailureIsNotAListingSuccess() throws IOException {
        http.json = EMPTY_PAGE;
        store.failWrite = true;
        expectError(CloudException.Kind.STORAGE);
    }

    @Test public void expiredAccessRefreshesAndPersistsNewCredential() throws Exception {
        DbxCredential expired = new DbxCredential("expired-test-token", 0L, "test-refresh", "testappkey");
        DropboxCloudRepository refreshRepo = new DropboxCloudRepository(new DbxClientV2(
            DbxRequestConfig.newBuilder("unit-test").withHttpRequestor(http).build(), expired), expired, vault, epoch);
        http.oauthJson = "{\"access_token\":\"new-test-token\",\"token_type\":\"bearer\",\"expires_in\":14400}";
        refreshRepo.listFolder("", null);
        assertEquals(2, http.urls.size());
        assertTrue(http.urls.get(0).endsWith("/oauth2/token"));
        assertTrue(http.urls.get(1).endsWith("/2/files/list_folder"));
        DbxCredential stored = DbxCredential.Reader.readFully(store.value);
        assertEquals("new-test-token", stored.getAccessToken());
        assertEquals("test-refresh", stored.getRefreshToken());
    }

    private void expectError(CloudException.Kind expected) throws IOException {
        try { repo.listFolder("", null); fail("Expected " + expected); }
        catch (CloudException e) { assertEquals(expected, e.kind); }
    }

    private static final class MemoryStore implements CredentialVault.Store {
        String value;
        boolean failWrite;
        public String read() { return value; }
        public void write(String value) throws IOException {
            if (failWrite) throw new IOException("Simulated disk failure");
            this.value = value;
        }
        public void clear() { value = null; }
    }

    private interface BeforeResponse { void run() throws IOException; }
    private static final class StubHttp extends HttpRequestor {
        int status = 200;
        String json = EMPTY_PAGE;
        String oauthJson;
        final List<String> urls = new ArrayList<>();
        String url;
        String body;
        BeforeResponse beforeResponse = () -> {};
        @Override public Response doGet(String url, Iterable<Header> headers) { throw new AssertionError("Unexpected GET"); }
        @Override public Uploader startPut(String url, Iterable<Header> headers) { throw new AssertionError("Unexpected PUT"); }
        @Override public Uploader startPost(String target, Iterable<Header> headers) {
            url = target;
            urls.add(target);
            return new Uploader() {
                final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                @Override public OutputStream getBody() { return bytes; }
                @Override public void close() {}
                @Override public void abort() {}
                @Override public Response finish() throws IOException {
                    body = bytes.toString(StandardCharsets.UTF_8.name());
                    beforeResponse.run();
                    Map<String, List<String>> responseHeaders = new HashMap<>();
                    responseHeaders.put("Content-Type", Collections.singletonList("application/json"));
                    responseHeaders.put("X-Dropbox-Request-Id", Collections.singletonList("test-request"));
                    responseHeaders.put("Retry-After", Collections.singletonList("1"));
                    String response = target.endsWith("/oauth2/token") && oauthJson != null ? oauthJson : json;
                    return new Response(status, new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), responseHeaders);
                }
            };
        }
    }
}
