package org.wso2.carbon.custom.tenant.cleanup.services.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.caching.impl.CacheManagerFactoryImpl;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.custom.tenant.cleanup.exception.TenantCleanUpServiceException;
import org.wso2.carbon.custom.tenant.cleanup.internal.TenantCleanUpComponentServiceHolder;
import org.wso2.carbon.custom.tenant.cleanup.model.ServiceResponse;
import org.wso2.carbon.custom.tenant.cleanup.model.TenantCleanUpContext;
import org.wso2.carbon.custom.tenant.cleanup.services.TenantCleanUpService;
import org.wso2.carbon.custom.tenant.cleanup.utils.TenantCleanUpConstants;
import org.wso2.carbon.custom.tenant.cleanup.utils.TenantCleanUpUtils;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.cache.Caching;

public class TenantCleanUpServiceImpl implements TenantCleanUpService {

    @SuppressWarnings("PackageAccessibility")
    static final Log auditLog = CarbonConstants.AUDIT_LOG;
    @SuppressWarnings("PackageAccessibility")
    private static final Log log = LogFactory.getLog(TenantCleanUpServiceImpl.class);
    private static TenantCleanUpServiceImpl instance = null;
    private static ThreadLocal<TenantCleanUpContext> tenantCleanUpContext = new ThreadLocal<>();

    /**
     * Get an instance from the TenantCleanUpServiceImpl class.s
     *
     * @return Instance of TenantCleanUpServiceImpl
     */
    public static TenantCleanUpServiceImpl getInstance() {

        if (instance == null) {
            synchronized (TenantCleanUpServiceImpl.class) {
                if (instance == null) {
                    instance = new TenantCleanUpServiceImpl();
                }
            }
        }
        return instance;
    }

    /**
     * Get the tenant id for a given tenant domain.
     *
     * @param tenantDomain tenant domain
     * @return If an error occurred while this will return the error message in String format.
     */
    @Override
    public String getTenantIdentifier(String tenantDomain) {

        try {
            TenantCleanUpUtils.validateDomain(tenantDomain);
            return Integer.toString(getTenantId(tenantDomain));
        } catch (TenantCleanUpServiceException | UserStoreException e) {
            return TenantCleanUpUtils.getResponseInStringFormat(
                    TenantCleanUpUtils
                            .buildFailureResponse(TenantCleanUpConstants.FailureScenario.ERROR_WHILE_TENANT_RETRIEVAL,
                                    tenantDomain, e.getMessage()));
        }
    }

    /**
     * Cleanup tenant for a given tenant domain.
     *
     * @param tenantDomain tenant domain
     * @return If an error occurred during the cleanup appropriate the error message will return.
     */
    @Override
    public ServiceResponse cleanupTenant(String tenantDomain) {

        synchronized ((TenantCleanUpConstants.CLEAN_UP_SERVICE + "-" + tenantDomain).intern()) {
            //store the thread local variables to be used in the listeners
            tenantCleanUpContext.set(new TenantCleanUpContext(tenantDomain));
            int tenantId = -1;
            try {
                TenantCleanUpUtils.validateDomain(tenantDomain);
                tenantId = getTenantId(tenantDomain);
                if (tenantId < 1) {
                    throw new TenantCleanUpServiceException(
                            TenantCleanUpConstants.EXCEPTION_MSG_INVALID_TENANT_ID + tenantDomain + " tenantId: " +
                                    tenantId);
                }
            } catch (TenantCleanUpServiceException | UserStoreException e) {
                if (log.isDebugEnabled()) {
                    log.debug(e.getMessage(), e);
                }
                String error = e.getMessage();
                auditLog.info(String.format(TenantCleanUpConstants.AUDIT_LOG_FORMAT, TenantCleanUpConstants.SUPER_ADMIN,
                        TenantCleanUpConstants.OPERATION_NAME, tenantDomain,
                        TenantCleanUpConstants.ERROR_INVALID_TENANT_DOMAIN + error,
                        TenantCleanUpConstants.FAILED_RESULT));
                tenantCleanUpContext.remove();
                return TenantCleanUpUtils
                        .buildFailureResponse(TenantCleanUpConstants.FailureScenario.ERROR_INVALID_TENANT_DOMAIN,
                                tenantDomain, error);
            }
            tenantDomain = tenantDomain.toLowerCase();


            try {
                cleanUpTenantInternal(tenantDomain, tenantId);
            } catch (UserStoreException | TenantCleanUpServiceException e) {
                String error = e.getMessage();
                auditLog.info(String.format(TenantCleanUpConstants.AUDIT_LOG_FORMAT,
                        TenantCleanUpConstants.SUPER_ADMIN,
                        TenantCleanUpConstants.OPERATION_NAME, tenantDomain,
                        TenantCleanUpConstants.ERROR_WHILE_WHILE_TENANT_CLEANUP + error,
                        TenantCleanUpConstants.FAILED_RESULT));
                tenantCleanUpContext.remove();
                return TenantCleanUpUtils
                        .buildFailureResponse(
                                TenantCleanUpConstants.FailureScenario.ERROR_WHILE_WHILE_TENANT_CLEANUP,
                                tenantDomain, error);
            }
            auditLog.info(String.format(TenantCleanUpConstants.AUDIT_LOG_FORMAT, TenantCleanUpConstants.SUPER_ADMIN,
                    TenantCleanUpConstants.OPERATION_NAME, tenantDomain,
                    TenantCleanUpConstants.SuccessScenario.SUCCESSFUL_TENANT_CLEANUP,
                    TenantCleanUpConstants.SUCCESSFUL_RESULT));
            tenantCleanUpContext.remove();
            return TenantCleanUpUtils
                    .buildSuccessResponse(TenantCleanUpConstants.SuccessScenario.SUCCESSFUL_TENANT_CLEANUP,
                            tenantDomain);
        }
    }

    /**
     * Check if tenant has cleaned up for a given tenant domain.
     *
     * @param tenantDomain tenant domain
     * @param tenantId     tenant Id
     * @return If an error occurred during the flow appropriate the error message will return.
     */
    @Override
    public Boolean verifyTenantCleanUp(String tenantDomain, int tenantId) {

        boolean verification = false;
        try {
            TenantCleanUpUtils.validateDomain(tenantDomain);
            TenantCleanUpUtils.validateTenantId(tenantId);
            verification = confirmTenantCleanUp(tenantDomain, tenantId);
        } catch (TenantCleanUpServiceException | UserStoreException e) {
            if (log.isDebugEnabled()) {
                log.debug("Tenant Cleanup verification failed.", e);
            }
            return false;
        }
        return verification;
    }

    /**
     * Check if tenant has deleted for a given tenant domain.
     *
     * @param tenantDomain tenant domain
     * @return If an error occurred during the flow appropriate the error message will return.
     */
    @Override
    public Boolean isTenantDeleted(String tenantDomain) {

        boolean verification;
        try {
            TenantCleanUpUtils.validateDomain(tenantDomain);
            verification = confirmTenantDeletion(tenantDomain);
        } catch (TenantCleanUpServiceException | UserStoreException e) {
            if (log.isDebugEnabled()) {
                log.debug("Tenant Deletion verification failed.", e);
            }
            return false;
        }
        return verification;
    }

    private int getTenantId(String tenantDomain)
            throws TenantCleanUpServiceException, UserStoreException {

        if (!StringUtils.isNotBlank(tenantDomain)) {
            String msg =
                    "Error: Given tenant domain is empty. Clean up service can not retrieving the tenant id for the " +
                            "given tenantDomain: " + tenantDomain;
            throw new TenantCleanUpServiceException(msg);
        }
        tenantDomain = tenantDomain.toLowerCase();
        TenantManager tenantManager = TenantCleanUpComponentServiceHolder.getTenantManager();
        int tenantId = -1; //Initialized as the invalid tenant id
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
            if (tenantId < 1) { //Break the flow in case of super tenantId or a invalid tenantId is retrieved
                String msg =
                        "Error while retrieving the tenant id for the tenant domain: " + tenantDomain +
                                ", TenantId: " + tenantId + " : Invalid tenantId.";
                throw new TenantCleanUpServiceException(msg);
            }
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new UserStoreException(msg, e);
        }
        return tenantId;
    }

    private boolean confirmTenantCleanUp(String tenantDomain, int tenantId)
            throws TenantCleanUpServiceException, UserStoreException {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String filter = TenantCleanUpUtils.buildCleanedUpTenantDomain(tenantDomain);

        try {
            dbConnection = TenantCleanUpUtils.getDatabaseConnection();
            preparedStatement = dbConnection.prepareStatement(TenantCleanUpConstants.SELECT_CLEAN_UP_TENANT);
            preparedStatement.setInt(1, tenantId);
            preparedStatement.setString(2, filter);
            preparedStatement.setQueryTimeout(TenantCleanUpConstants.SEARCH_TIME); //Timeout the search in 10 seconds.

            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            } else {
                String msg = String.format(TenantCleanUpConstants.EXCEPTION_MSG_TENANT_NOT_EXISTS, tenantDomain,
                        filter);
                if (log.isDebugEnabled()) {
                    log.debug(msg);
                }
                throw new TenantCleanUpServiceException(msg);
            }
        } catch (SQLException e) {
            String msg = "Error in validating the tenant cleanup for tenant domain: "
                    + tenantDomain + " tenantId: " + tenantId;
            if (log.isDebugEnabled()) {
                log.debug(msg, e);
            }
            throw new UserStoreException(msg, e);
        } finally {
            TenantCleanUpUtils.closeAllConnections(dbConnection, preparedStatement, resultSet);
        }
    }

    private boolean confirmTenantDeletion(String tenantDomain) throws TenantCleanUpServiceException,
            UserStoreException {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String filter = TenantCleanUpUtils.buildCleanedUpTenantDomain(tenantDomain);

        try {
            dbConnection = TenantCleanUpUtils.getDatabaseConnection();
            preparedStatement = dbConnection.prepareStatement(TenantCleanUpConstants.SELECT_DELETED_TENANT);
            preparedStatement.setString(1, filter);
            preparedStatement.setQueryTimeout(TenantCleanUpConstants.SEARCH_TIME); //Timeout the search in 10 seconds.

            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String msg = String.format(TenantCleanUpConstants.EXCEPTION_MSG_TENANT_STILL_EXISTS, tenantDomain,
                        filter);
                if (log.isDebugEnabled()) {
                    log.debug(msg);
                }
                throw new TenantCleanUpServiceException(msg);
            } else {
                return true; //No results in the database
            }
        } catch (SQLException e) {
            String msg =
                    String.format("Error while checking if the tenant with the tenant domain %s, is deleted; filter: " +
                            filter, tenantDomain);
            if (log.isDebugEnabled()) {
                log.debug(msg, e);
            }
            throw new UserStoreException(msg, e);
        } finally {
            TenantCleanUpUtils.closeAllConnections(dbConnection, preparedStatement, resultSet);
        }
    }

    private void cleanUpTenantInternal(String tenantDomain, int tenantId) throws UserStoreException,
            TenantCleanUpServiceException {

        //deactivate the tenant
        deactivateTenant(tenantDomain, tenantId);
        //remove tenant own caches and the cache managers
        removeTenantCacheManagers(tenantDomain, tenantId);
        //notify other nodes if present
        notifyOtherNodesOfTenantCleanUp(tenantId);
        //change the tenant's name from the database
        markTenantDomainForDeletion(tenantDomain, tenantId);
    }

    private void markTenantDomainForDeletion(String tenantDomain, int tenantId)
            throws UserStoreException {

        tenantDomain = tenantDomain.toLowerCase();
        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;
        try {
            dbConnection = TenantCleanUpUtils.getDatabaseConnection();
            preparedStatement =
                    dbConnection.prepareStatement(TenantCleanUpConstants.CHANGE_TENANT_DOMAIN_SQL);
            preparedStatement.setString(1, TenantCleanUpUtils.buildMarkedTenantDomain(tenantDomain));
            preparedStatement.setInt(2, tenantId);
            preparedStatement.setString(3, tenantDomain);
            preparedStatement.executeUpdate();
            dbConnection.commit();
            if (log.isDebugEnabled()) {
                log.debug(
                        "Successfully marked the tenant for deletion tenantDomain: " + tenantDomain);
            }
        } catch (SQLException e) {
            TenantCleanUpUtils.rollBack(dbConnection);
            String msg = "Error in marking the tenant for deletion. The tenant domain: "
                    + tenantDomain.toLowerCase() + " tenantId: " + tenantId;
            if (log.isDebugEnabled()) {
                log.debug(msg, e);
            }
            throw new UserStoreException(msg, e);
        } finally {
            TenantCleanUpUtils.closeAllConnections(dbConnection, preparedStatement);
        }
    }

    private void removeTenantCacheManagers(String tenantDomain, int tenantId) {

        if (log.isDebugEnabled()) {
            log.debug("Remove all caches of the tenantId: " + tenantId + ", tenantDomain: " + tenantDomain);
        }
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantId(tenantId, true);
            ((CacheManagerFactoryImpl) Caching.getCacheManagerFactory()).removeAllCacheManagers(tenantDomain);
            ((CacheManagerFactoryImpl) Caching.getCacheManagerFactory()).removeCacheManagerMap(tenantDomain);
            if (log.isDebugEnabled()) {
                log.debug("Successfully remove all caches and cache managers of the tenantId: " + tenantId + ", " +
                        "tenantDomain: " + tenantDomain);
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

    }

    private void deactivateTenant(String tenantDomain, int tenantId) throws UserStoreException {

        TenantManager tenantManager = TenantCleanUpComponentServiceHolder.getTenantManager();
        //Unload tenant artifacts.
        TenantMgtUtil.unloadTenantConfigurations(tenantDomain, tenantId);
        //Notify tenant deactivation all listeners.
        try {
            tenantManager.deactivateTenant(tenantId);
            TenantMgtUtil.triggerTenantDeactivation(tenantId);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error occurred during tenant deactivation.";
            log.error(msg, e);
            throw new UserStoreException(msg, e);
        } catch (StratosException e) {
            String msg = "Error occurred while notifying tenant management listeners of deactivation.";
            log.error(msg, e);
            throw new UserStoreException(msg, e);
        }
    }

    private void notifyOtherNodesOfTenantCleanUp(int tenantId) throws TenantCleanUpServiceException {

        try {
            TenantMgtUtil.deleteWorkernodesTenant(tenantId);
        } catch (Exception e) {
            String msg = "Error occurred while notifying other nodes of tenant cleanup.";
            log.error(msg, e);
            throw new TenantCleanUpServiceException(msg, e);
        }
    }

    public String getCleanUpContextDomainFromThreadLocal() {
        if (tenantCleanUpContext.get() == null ) {
            return null;
        }
        return tenantCleanUpContext.get().getTenantDomain();
    }

    public void removeCleanUpContextThreadLocal() {

        if (tenantCleanUpContext.get() != null ) {
            tenantCleanUpContext.remove();        }
    }
}
