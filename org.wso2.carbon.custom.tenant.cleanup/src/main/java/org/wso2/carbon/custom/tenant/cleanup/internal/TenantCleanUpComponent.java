package org.wso2.carbon.custom.tenant.cleanup.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.custom.tenant.cleanup.listner.TenantCleanUpMgtListener;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.service.RealmService;

@Component(
        name = "org.wso2.carbon.custom.tenant.cleanup.component",
        immediate = true
)
public class TenantCleanUpComponent {

    @SuppressWarnings("PackageAccessibility")
    private static final Log LOGGER = LogFactory.getLog(TenantCleanUpComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(TenantMgtListener.class.getName(), new TenantCleanUpMgtListener(), null);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Custom Tenant Cleanup Component is activated.");
            }
        } catch (Throwable e) {
            LOGGER.error("Error occurred during Tenant Clean Up component bundle activation. ", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Custom Tenant Cleanup Component is deactivated.");
        }
        TenantCleanUpComponentServiceHolder.getInstance().setRealmService(null);
    }

    @Reference(
            name = "user.realmservice.default",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService"
    )
    protected void setRealmService(RealmService realmService) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("RealmService is set in the bundle");
        }
        TenantCleanUpComponentServiceHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("RealmService is unset in the bundle");
        }
        TenantCleanUpComponentServiceHolder.getInstance().setRealmService(null);
    }
}
