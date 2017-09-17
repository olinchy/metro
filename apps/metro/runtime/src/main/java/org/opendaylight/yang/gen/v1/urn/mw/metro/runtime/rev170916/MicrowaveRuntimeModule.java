package org.opendaylight.yang.gen.v1.urn.mw.metro.runtime.rev170916;

import com.zte.sdn.mw.e2e.runtime.MicrowaveRuntimeImpl;

public class MicrowaveRuntimeModule extends org.opendaylight.yang.gen.v1.urn.mw.metro.runtime
        .rev170916.AbstractMicrowaveRuntimeModule {
    public MicrowaveRuntimeModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MicrowaveRuntimeModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.urn.mw.metro.runtime.rev170916.MicrowaveRuntimeModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new MicrowaveRuntimeImpl();
    }
}
