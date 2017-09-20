package org.opendaylight.yang.gen.v1.urn.zte.mw.sdn.netconf.connection.impl.rev170918;

import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.connections.impl.NetConfConnection;

public class NetconfConnectionModule extends org.opendaylight.yang.gen.v1.urn.zte.mw.sdn.netconf.connection.impl.rev170918.AbstractNetconfConnectionModule {
    public NetconfConnectionModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfConnectionModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            NetconfConnectionModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    private static final Logger LOG = LoggerFactory.getLogger(NetconfConnectionModule.class);

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("initialized");
        final BindingAwareBroker.ConsumerContext broker = getBrokerDependency().registerConsumer(context -> {});
        final MountPointService mountPointService = broker.getSALService(MountPointService.class);
        return new NetConfConnection(mountPointService);
    }
}
