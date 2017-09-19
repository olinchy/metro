package org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.pce.path.calculator.impl.rev170919;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.PcePathCalculatorImpl;

public class PcePathCalculatorModule extends org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.pce.path.calculator.impl.rev170919.AbstractPcePathCalculatorModule {
    public PcePathCalculatorModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PcePathCalculatorModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.pce.path.calculator.impl.rev170919.PcePathCalculatorModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    private static final Logger LOG = LoggerFactory.getLogger(PcePathCalculatorModule.class);

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("pce path calculator started");
        return new PcePathCalculatorImpl();
    }
}
