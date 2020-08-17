package org.wso2.carbon.custom.tenant.cleanup.listener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.custom.tenant.cleanup.exception.TenantCleanUpServiceException;
import org.wso2.carbon.custom.tenant.cleanup.internal.TenantCleanUpComponentServiceHolder;
import org.wso2.carbon.custom.tenant.cleanup.services.impl.TenantCleanUpServiceImpl;
import org.wso2.carbon.custom.tenant.cleanup.utils.TenantCleanUpConstants;
import org.wso2.carbon.custom.tenant.cleanup.utils.TenantCleanUpUtils;
import org.wso2.carbon.identity.core.AbstractIdentityTenantMgtListener;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.ldap.LDAPConnectionContext;
import org.wso2.carbon.user.core.ldap.ReadOnlyLDAPUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.JNDIUtil;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

public class TenantCleanUpMgtListener extends AbstractIdentityTenantMgtListener {

    @SuppressWarnings("PackageAccessibility")
    private static final Log log = LogFactory.getLog(TenantCleanUpMgtListener.class);
    private static LDAPConnectionContext connectionSource = null;

    /**
     * Validate the tenant domain before tenant activation and rename
     *
     * @param domainName tenant domain
     * @throws TenantCleanUpServiceException, if invalid tenant domain name is given
     */
    public static void validateTenantDomain(String domainName) throws TenantCleanUpServiceException {

        if (!StringUtils.isNotBlank(domainName)) {
            String msg = "Provided domain name is empty or null.";
            if (log.isDebugEnabled()) {
                log.debug(msg);
            }
            throw new TenantCleanUpServiceException(msg);
        }
        //check if the tenant is already marked for deletion
        if (StringUtils.startsWith(domainName, TenantCleanUpConstants.prefix)) {
            String msg =
                    "Illegal Attempt! Trying to active already marked tenant for deletion. The tenant domain " +
                            "starting with: " + TenantCleanUpConstants.prefix;
            if (log.isDebugEnabled()) {
                log.debug(msg);
            }
            throw new TenantCleanUpServiceException(msg);
        }
    }

    //TODO: These onPreTenantActivation and onPreTenantRename need to be in the product for more flexibility. Here we
    // need to prevent marked tenant activation and renaming
    public void onPreTenantActivation(int tenantId) throws StratosException {

        try {
            String tenantDomain = TenantCleanUpUtils.getTenantDomain(tenantId);
            validateTenantDomain(tenantDomain);
        } catch (TenantCleanUpServiceException e) {
            String msg = "Error occurred while tenant activation for tenantId: " + tenantId + ", " + e.getMessage();
            log.error(msg, e);
            throw new StratosException(msg, e);
        }
    }

    public void onPreTenantRename(int tenantId, String tenantDomain, String newTenantDomain) throws StratosException {

        try {
            validateTenantDomain(tenantDomain);
        } catch (TenantCleanUpServiceException e) {
            String msg =
                    "Error occurred while renaming  the  tenant : " + tenantDomain + ", " + e.getMessage();
            log.error(msg, e);
            throw new StratosException(msg, e);
        }
    }

    @Override
    public void onTenantDeactivation(int tenantId) throws StratosException {

        TenantCleanUpServiceImpl tenantCleanUpService = TenantCleanUpServiceImpl.getInstance();
        String tenantDomain = tenantCleanUpService.getCleanUpContextDomainFromThreadLocal();

        if (StringUtils.isNotBlank(tenantDomain)) {
            RealmService realmService = TenantCleanUpComponentServiceHolder.getInstance().getRealmService();
            RealmConfiguration realmConfiguration = realmService.getBootstrapRealmConfiguration();

            UserStoreManager userStoreManager = null;
            try {
                userStoreManager = realmService.getUserRealm(realmConfiguration).getUserStoreManager();
                if (userStoreManager instanceof ReadOnlyLDAPUserStoreManager && !userStoreManager.isReadOnly()) {
                    //Rename Ldap Organizational Unit from LDAP
                    renameOrganizationalContextFromLDAPBasedUserStore(tenantCleanUpService, realmService,
                            realmConfiguration,
                            tenantDomain);
                }
            } catch (UserStoreException e) {
                String error = e.getMessage();
                log.error(error, e);
                throw new StratosException(error, e);
            }
            //Removes the User Portal Service Provider
            removeUserPortalServiceProvider(tenantId, tenantDomain);
        }
    }

    private void renameOrganizationalContextFromLDAPBasedUserStore(TenantCleanUpServiceImpl tenantCleanUpService,
                                                                   RealmService realmService,
                                                                   RealmConfiguration realmConfiguration,
                                                                   String tenantDomain) throws StratosException {

        if (realmConfiguration == null) {
            throw new StratosException("Error: Bootstrap realm config return as null.");
        }
        String newDnOfOrganizationalContext = null;
        DirContext dirContext = null;

        try {
            if (connectionSource == null) {
                connectionSource = new LDAPConnectionContext(realmConfiguration);
            }
            dirContext = connectionSource.getContext();
            String partitionDN = realmService.getTenantMgtConfiguration().getTenantStoreProperties().get(
                    UserCoreConstants.TenantMgtConfig.PROPERTY_ROOT_PARTITION);
            String organizationNameAttribute = realmService.getTenantMgtConfiguration().getTenantStoreProperties().get(
                    UserCoreConstants.TenantMgtConfig.PROPERTY_ORGANIZATIONAL_ATTRIBUTE);
            String dnOfOrganizationalContext = organizationNameAttribute + "=" + tenantDomain + "," +
                    partitionDN;
            newDnOfOrganizationalContext =
                    organizationNameAttribute + "=" + TenantCleanUpUtils.buildMarkedTenantDomain(tenantDomain) +
                            partitionDN;
            dirContext.rename(dnOfOrganizationalContext, newDnOfOrganizationalContext);
            //TODO:Need to rename groups and the users entries as well
        } catch (UserStoreException e) {
            String msg = "Error occurred while getting the LDAP Directory context from LDAPConnectionContext.";
            log.error(msg, e);
            throw new StratosException(msg, e);
        } catch (NamingException e) {
            String msg =
                    String.format("Error occurred while renaming the organizational context: %s for the " +
                            "tenantDomain: %s", tenantDomain, newDnOfOrganizationalContext);
            log.error(msg, e);
            throw new StratosException(msg, e);
        } finally {
            try {
                JNDIUtil.closeContext(dirContext);
            } catch (UserStoreException e) {
                String msg = "Error occurred while closing the LDAP Directory context.";
                log.error(msg, e);
            }
        }
    }

    private void removeUserPortalServiceProvider(int tenantId, String tenantDomain) {

        //TODO:Need to check if the user portal service provider is available and delete it. This is only relavant
        // for Identity server 5.10.0
    }
}
