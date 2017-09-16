package org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.notification.listener.impl.rev170913;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationListenerModule extends org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.notification.listener
        .impl.rev170913.AbstractNotificationListenerModule {
    public NotificationListenerModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NotificationListenerModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.notification.listener.impl
                    .rev170913.NotificationListenerModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    private static final Logger LOG = LoggerFactory.getLogger(NotificationListenerModule.class);
    private BundleContext bundleContext;

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("start to register ietf listener");
        //        final NotificationListenerProvider provider = new NotificationListenerProvider
        // (getDataBrokerDependency(), getMicrowaveRuntime());
        return () -> {
            //            reg.close();
            //            provider.close();
        };
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
