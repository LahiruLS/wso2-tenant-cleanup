package org.wso2.carbon.custom.tenant.cleanup.internal;

import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;

public class TenantCleanUpComponentServiceHolder {

    private static final TenantCleanUpComponentServiceHolder instance = new TenantCleanUpComponentServiceHolder();
    private static RealmService realmService;

    private TenantCleanUpComponentServiceHolder() {

    }

    public static TenantCleanUpComponentServiceHolder getInstance() {

        return instance;
    }

    public static TenantManager getTenantManager() {

        return realmService.getTenantManager();
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        TenantCleanUpComponentServiceHolder.realmService = realmService;
    }
}
