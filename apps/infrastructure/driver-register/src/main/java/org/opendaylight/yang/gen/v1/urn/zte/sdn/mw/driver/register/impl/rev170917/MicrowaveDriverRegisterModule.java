package org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.driver.register.impl.rev170917;

import com.zte.mw.sdn.components.DriverHolder;

public class MicrowaveDriverRegisterModule extends org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.driver.register.impl.rev170917.AbstractMicrowaveDriverRegisterModule {
    public MicrowaveDriverRegisterModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MicrowaveDriverRegisterModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.driver.register.impl.rev170917.MicrowaveDriverRegisterModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new DriverHolder();
    }
}
