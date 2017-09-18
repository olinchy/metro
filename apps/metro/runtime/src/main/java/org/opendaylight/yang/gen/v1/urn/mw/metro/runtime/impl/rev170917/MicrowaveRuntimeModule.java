package org.opendaylight.yang.gen.v1.urn.mw.metro.runtime.impl.rev170917;

import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(MicrowaveRuntimeModule.class);

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BindingAwareBroker.ConsumerContext broker = getBrokerDependency().registerConsumer(context -> {
        });
        final MountPointService mountPointService = broker.getSALService(MountPointService.class);

        new Thread(() -> {
            while (true) {
                try {
                    LOG.info("driver register has " + getDriverRegisterDependency().getRegistered().length
                                     + " drivers");
                    Thread.sleep(10000);
                } catch (InterruptedException e) {

                }
            }
        }).start();
        return new MicrowaveRuntimeImpl(mountPointService, getDriverRegisterDependency());
    }
}