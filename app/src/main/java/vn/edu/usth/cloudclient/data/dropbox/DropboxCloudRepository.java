package vn.edu.usth.cloudclient.data.dropbox;

import com.dropbox.core.*;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.oauth.DbxOAuthException;
import com.dropbox.core.oauth.DbxOAuthError;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import vn.edu.usth.cloudclient.auth.CredentialVault;
import vn.edu.usth.cloudclient.data.CloudException;
import vn.edu.usth.cloudclient.data.CloudRepository;
import vn.edu.usth.cloudclient.model.CloudItem;
import vn.edu.usth.cloudclient.model.FolderPage;

public final class DropboxCloudRepository implements CloudRepository {
    private final DbxClientV2 client;
    private final DbxCredential credential;
    private final CredentialVault vault;
    private final long session;

    public DropboxCloudRepository(DbxClientV2 client, DbxCredential credential,
                                  CredentialVault vault, long session) {
        this.client = client;
        this.credential = credential;
        this.vault = vault;
        this.session = session;
    }

    @Override public String rootId() { return ""; }

    @Override public synchronized FolderPage listFolder(String folderId, String cursor) throws IOException {
        if (!vault.isCurrent(session)) throw new CloudException(CloudException.Kind.AUTH);
        try {
            ListFolderResult result = cursor == null
                ? client.files().listFolderBuilder(folderId).withLimit(100L).start()
                : client.files().listFolderContinue(cursor);
            // The SDK may have refreshed the access token. Never revive a logged-out session.
            try {
                if (!vault.save(session, credential.toString())) throw new CloudException(CloudException.Kind.AUTH);
            } catch (CloudException e) { throw e; }
            catch (IOException e) { throw new CloudException(CloudException.Kind.STORAGE); }
            return mapPage(result);
        } catch (DbxException e) { throw mapError(e); }
    }

    public static FolderPage mapPage(ListFolderResult result) {
        List<CloudItem> items = new ArrayList<>();
        for (Metadata metadata : result.getEntries()) {
            if (metadata instanceof FolderMetadata) {
                FolderMetadata folder = (FolderMetadata) metadata;
                items.add(new CloudItem(folder.getId(), folder.getName(), true, 0));
            } else if (metadata instanceof FileMetadata) {
                FileMetadata file = (FileMetadata) metadata;
                items.add(new CloudItem(file.getId(), file.getName(), false, file.getSize()));
            }
        }
        return new FolderPage(items, result.getHasMore() ? result.getCursor() : null);
    }

    public static CloudException mapError(DbxException error) {
        CloudException.Kind kind = CloudException.Kind.OTHER;
        if (error instanceof InvalidAccessTokenException) {
            com.dropbox.core.v2.auth.AuthError auth = ((InvalidAccessTokenException) error).getAuthError();
            kind = auth != null && (auth.isMissingScope() || auth.isRouteAccessDenied())
                ? CloudException.Kind.PERMISSION : CloudException.Kind.AUTH;
        } else if (error instanceof DbxOAuthException && DbxOAuthError.INVALID_GRANT.equals(
            ((DbxOAuthException) error).getDbxOAuthError().getError())) kind = CloudException.Kind.AUTH;
        else if (error instanceof AccessErrorException) kind = CloudException.Kind.PERMISSION;
        else if (error instanceof RateLimitException) kind = CloudException.Kind.RATE_LIMIT;
        else if (error instanceof NetworkIOException) kind = CloudException.Kind.NETWORK;
        else if (error instanceof ServerException) kind = CloudException.Kind.SERVICE;
        else if (error instanceof ListFolderErrorException) {
            ListFolderError value = ((ListFolderErrorException) error).errorValue;
            if (value.isPath() && value.getPathValue().isNotFound()) kind = CloudException.Kind.NOT_FOUND;
        }
        return new CloudException(kind); // No raw server text or tokens go to UI/logs.
    }
}
