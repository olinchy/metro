package org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.notification.listener.impl.rev170913;

import com.zte.sdn.mw.e2e.qinq.service.notification.listener.impl.NotificationListenerProvider;
import org.osgi.framework.BundleContext;

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

    private BundleContext bundleContext;

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final NotificationListenerProvider provider = new NotificationListenerProvider();
        //        provider.setNotifySer(getNotificationServiceDependency());
        //        provider.setContext(new NotificationListenerContext(bundleContext));
        //        final BindingAwareBroker.RpcRegistration<?> reg = getRpcRegistryDependency()
        //                .addRpcImplementation(NotificationListenerService.class, provider);
        //        bundleContext.registerService(SchemaContextListener.class, provider, new Hashtable<String, String>());

        return () -> {
            //            reg.close();
            provider.close();
        };
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
