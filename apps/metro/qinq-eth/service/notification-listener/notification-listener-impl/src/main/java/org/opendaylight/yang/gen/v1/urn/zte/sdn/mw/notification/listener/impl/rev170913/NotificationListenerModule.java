package org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.notification.listener.impl.rev170913;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.databroker.DataBrokerProvider;

import com.zte.sdn.mw.e2e.qinq.service.notification.listener.impl.NotificationListenerProvider;

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

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        DataBrokerProvider.getInstance().setDataBroker(getDataBrokerDependency());
        LOG.info("start to register ietf listener");
        new NotificationListenerProvider
                (getDataBrokerDependency(), getMwRuntimeServiceDependency()).start();

        LOG.info("path calculator is " + getMwPcePathCalculatorDependency().toString());
        return () -> {};
    }
}
