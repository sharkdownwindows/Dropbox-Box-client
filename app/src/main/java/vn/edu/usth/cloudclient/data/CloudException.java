package vn.edu.usth.cloudclient.data;

import java.io.IOException;

public final class CloudException extends IOException {
    public enum Kind { AUTH, PERMISSION, NOT_FOUND, RATE_LIMIT, NETWORK, SERVICE, STORAGE, OTHER }
    public final Kind kind;

    public CloudException(Kind kind) {
        super(kind.name());
        this.kind = kind;
    }
}
