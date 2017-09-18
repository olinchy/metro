package org.opendaylight.yang.gen.v1.urn.mw.metro.runtime.impl.rev170917;

import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;

import com.zte.sdn.mw.e2e.runtime.MicrowaveRuntimeImpl;

public class MicrowaveRuntimeModule extends org.opendaylight.yang.gen.v1.urn.mw.metro.runtime.impl
        .rev170917.AbstractMicrowaveRuntimeModule {
    public MicrowaveRuntimeModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MicrowaveRuntimeModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.urn.mw.metro.runtime.impl.rev170917.MicrowaveRuntimeModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BindingAwareBroker.ConsumerContext broker = getBrokerDependency().registerConsumer(context -> {
        });
        final MountPointService mountPointService = broker.getSALService(MountPointService.class);
        return new MicrowaveRuntimeImpl(mountPointService, getDriverRegisterDependency());
    }
}
