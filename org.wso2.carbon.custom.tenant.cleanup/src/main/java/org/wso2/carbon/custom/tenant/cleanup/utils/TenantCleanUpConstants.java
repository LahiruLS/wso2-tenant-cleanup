package org.wso2.carbon.custom.tenant.cleanup.utils;

public class TenantCleanUpConstants {

    public static final String CHANGE_TENANT_DOMAIN_SQL = "UPDATE UM_TENANT SET UM_DOMAIN_NAME=? WHERE UM_ID=? AND " +
            "UM_DOMAIN_NAME=?";
    public static final String SELECT_CLEAN_UP_TENANT = "SELECT UM_DOMAIN_NAME FROM UM_TENANT WHERE " +
            "UM_ID = ? AND UM_DOMAIN_NAME LIKE ?";
    public static final String SELECT_DELETED_TENANT = "SELECT UM_DOMAIN_NAME FROM UM_TENANT WHERE " +
            "UM_DOMAIN_NAME LIKE ?";
    public static final int SEARCH_TIME = 10000;

    public static final String prefix = "marked_to_delete";
    public static final String PREFIX_SEPARATOR = "_";
    public static final String SCENARIO_SEPARATOR = "-";
    public static final String CLEAN_UP_SERVICE = "tenant-cleanup-service";

    public static final String ILLEGAL_CHARACTERS_FOR_TENANT_DOMAIN = ".*[^a-z0-9\\._\\-].*";
    public static final String DOMAIN_SEPARATOR = ".";

    public static final String AUDIT_LOG_FORMAT =
            "Initiator : %s | Action : %s | Target : %s | Data : %s | Result : %s";
    public static final String SUPER_ADMIN = "carbon.super.admin.user";
    public static final String OPERATION_NAME = "Tenant Cleanup";
    public static final String ERROR_INVALID_TENANT_DOMAIN = "Bad request! Tenant domain validation failed.";
    public static final String ERROR_WHILE_WHILE_TENANT_CLEANUP = "Error occurred while cleaning up the tenant.";
    public static final String SUCCESSFUL_RESULT = "Success!";
    public static final String FAILED_RESULT = "Failed!";

    public static final String EXCEPTION_MSG_INVALID_TENANT_ID = "Invalid tenant Id retrieved for the tenant domain: ";
    public static final String EXCEPTION_MSG_TENANT_STILL_EXISTS = "Tenant with the given domain is not deleted yet. " +
            "tenant domain: %s, and the filter: %s";
    public static final String EXCEPTION_MSG_TENANT_NOT_EXISTS = "Tenant clean up not verified. Marked domain " +
            "can not be found in the database for tenant domain: %s, and the filter: %s";

    /**
     * Enum which contains the error scenarios with corresponding error messages and error codes.
     */
    public enum FailureScenario {

        ERROR_WHILE_TENANT_RETRIEVAL("500", "Error occurred while retrieving the " +
                "tenant domain: %s. Error reason: %s"),
        ERROR_INVALID_SERVICE_CALL("400", "Bad request! Invalid tenant domain provided: " +
                "tenant domain: %s "),
        ERROR_INVALID_TENANT_DOMAIN("400", "Bad request! Tenant domain validation failed: " +
                "tenant domain: %s. Error Reason: %s "),
        ERROR_WHILE_WHILE_TENANT_CLEANUP("500", "Error occurred while cleaning up the tenant" +
                " domain: %s, Error reason: %s");

        private final String code;
        private final String message;

        FailureScenario(String code, String message) {

            this.code = code;
            this.message = message;
        }

        public String getCode() {

            return code;
        }

        public String getMessage() {

            return message;
        }

        @Override
        public String toString() {

            return code + SCENARIO_SEPARATOR + message;
        }
    }

    /**
     * Enum which contains the success scenarios with corresponding success messages and success codes.
     */
    public enum SuccessScenario {

        SUCCESSFUL_TENANT_CLEANUP("200", "TenantDomain %s cleanup successfully finished.");

        private final String code;
        private final String message;

        SuccessScenario(String code, String message) {

            this.code = code;
            this.message = message;
        }

        public String getCode() {

            return code;
        }

        public String getMessage() {

            return message;
        }

        @Override
        public String toString() {

            return code + SCENARIO_SEPARATOR + message;
        }
    }
}
