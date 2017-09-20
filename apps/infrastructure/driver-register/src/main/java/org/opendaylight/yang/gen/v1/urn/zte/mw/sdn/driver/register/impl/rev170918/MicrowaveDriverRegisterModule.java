package org.opendaylight.yang.gen.v1.urn.zte.mw.sdn.driver.register.impl.rev170918;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.DriverHolder;

public class MicrowaveDriverRegisterModule extends org.opendaylight.yang.gen.v1.urn.zte.mw.sdn.driver.register.impl.rev170918.AbstractMicrowaveDriverRegisterModule {
    public MicrowaveDriverRegisterModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MicrowaveDriverRegisterModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            MicrowaveDriverRegisterModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    private static final Logger LOG = LoggerFactory.getLogger(MicrowaveDriverRegisterModule.class);

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("initialized");
        return new DriverHolder();
    }
}
