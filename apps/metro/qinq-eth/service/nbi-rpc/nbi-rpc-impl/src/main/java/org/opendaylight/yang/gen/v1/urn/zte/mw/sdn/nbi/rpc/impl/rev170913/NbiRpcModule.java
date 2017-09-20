package org.opendaylight.yang.gen.v1.urn.zte.mw.sdn.nbi.rpc.impl.rev170913;

public class NbiRpcModule extends org.opendaylight.yang.gen.v1.urn.zte.mw.sdn.nbi.rpc.impl.rev170913.AbstractNbiRpcModule {
    public NbiRpcModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NbiRpcModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.urn.zte.mw.sdn.nbi.rpc.impl.rev170913.NbiRpcModule oldModule,
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
        return () -> {};
    }
}
