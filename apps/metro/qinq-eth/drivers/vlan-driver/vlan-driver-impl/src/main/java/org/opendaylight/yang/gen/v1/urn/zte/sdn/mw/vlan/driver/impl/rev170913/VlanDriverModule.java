package org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.vlan.driver.impl.rev170913;

public class VlanDriverModule extends org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.vlan.driver.impl
        .rev170913.AbstractVlanDriverModule {
    public VlanDriverModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VlanDriverModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.vlan.driver.impl.rev170913.VlanDriverModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        throw new java.lang.UnsupportedOperationException();
    }
}
