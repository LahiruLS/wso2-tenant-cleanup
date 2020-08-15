package org.wso2.carbon.custom.tenant.cleanup.exception;

public class TenantCleanUpServiceException extends Exception {

    private static final long serialVersionUID = 339507180738687586L;

    public TenantCleanUpServiceException() {

        super();
    }

    public TenantCleanUpServiceException(String message, Throwable cause) {

        super(message, cause);
    }

    public TenantCleanUpServiceException(String message) {

        super(message);
    }

    public TenantCleanUpServiceException(Throwable cause) {

        super(cause);
    }
}
