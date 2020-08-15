package org.wso2.carbon.custom.tenant.cleanup.model;

public class TenantCleanUpContext {

    private final String tenantDomain;

    public TenantCleanUpContext(String tenantDomain) {

        this.tenantDomain = tenantDomain;
    }

    public String getTenantDomain() {

        return tenantDomain;
    }
}
