package org.wso2.carbon.custom.tenant.cleanup;

import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.custom.tenant.cleanup.model.ServiceResponse;
import org.wso2.carbon.custom.tenant.cleanup.services.impl.TenantCleanUpServiceImpl;

public class TenantCleanUpAdminService extends AbstractAdmin {

    /**
     * Get the tenant id for a given tenant domain.
     *
     * @param tenantDomain tenant domain
     * @return If an error occurred while this will return the error message in String format.
     */
    public String getTenantIdentifier(String tenantDomain) {

        TenantCleanUpServiceImpl tenantCleanUpService = TenantCleanUpServiceImpl.getInstance();
        return tenantCleanUpService.getTenantIdentifier(tenantDomain);
    }

    /**
     * Cleanup tenant for a given tenant domain.
     *
     * @param tenantDomain tenant domain
     * @return If an error occurred during the cleanup appropriate the error message will return.
     */
    public ServiceResponse cleanupTenant(String tenantDomain) {

        TenantCleanUpServiceImpl tenantCleanUpService = TenantCleanUpServiceImpl.getInstance();
        return tenantCleanUpService.cleanupTenant(tenantDomain);
    }

    /**
     * Check if tenant has cleaned up for a given tenant domain.
     *
     * @param tenantDomain tenant domain
     * @param tenantId     tenant Id
     * @return If an error occurred during the flow appropriate the error message will return.
     */
    public Boolean verifyTenantCleanUp(String tenantDomain, int tenantId) {

        TenantCleanUpServiceImpl tenantCleanUpService = TenantCleanUpServiceImpl.getInstance();
        return tenantCleanUpService.verifyTenantCleanUp(tenantDomain, tenantId);
    }

    /**
     * Check if tenant has deleted for a given tenant domain.
     *
     * @param tenantDomain tenant domain
     * @return If an error occurred during the flow appropriate the error message will return.
     */
    public Boolean verifyTenantDeletion(String tenantDomain) {

        TenantCleanUpServiceImpl tenantCleanUpService = TenantCleanUpServiceImpl.getInstance();
        return tenantCleanUpService.verifyTenantDeletion(tenantDomain);
    }
}
