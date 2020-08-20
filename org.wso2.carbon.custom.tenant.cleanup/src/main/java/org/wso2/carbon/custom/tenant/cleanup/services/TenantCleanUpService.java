package org.wso2.carbon.custom.tenant.cleanup.services;

import org.wso2.carbon.custom.tenant.cleanup.model.ServiceResponse;

public interface TenantCleanUpService {

    /**
     * Get the tenant id for a given tenant domain.
     *
     * @param tenantDomain tenant domain
     * @return If an error occurred while this will return the error message in String format.
     */
    String getTenantIdentifier(String tenantDomain);

    /**
     * Cleanup tenant for a given tenant domain.
     *
     * @param tenantDomain tenant domain
     * @return If an error occurred during the cleanup appropriate the error message will return.
     */
    ServiceResponse cleanupTenant(String tenantDomain);

    /**
     * Check if tenant has cleaned up for a given tenant domain.
     *
     * @param tenantDomain tenant domain
     * @param tenantId tenant Id
     * @return If an error occurred during the flow appropriate the error message will return.
     */
    Boolean verifyTenantCleanUp(String tenantDomain, int tenantId);

    /**
     * Check if tenant has deleted for a given tenant domain.
     *
     * @param tenantDomain tenant domain
     * @return If an error occurred during the flow appropriate the error message will return.
     */
    Boolean isTenantDeleted(String tenantDomain);

}

