package org.opendaylight.yang.gen.v1.urn.zte.mw.sdn.vlan.driver.rev170913;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.e2e.qinq.drivers.VlanDriver;

public class VlanDriverModule extends org.opendaylight.yang.gen.v1.urn.zte.mw.sdn.vlan.driver.rev170913.AbstractVlanDriverModule {
    public VlanDriverModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VlanDriverModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            VlanDriverModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    private static final Logger LOG = LoggerFactory.getLogger(VlanDriverModule.class);
    private VlanDriver driver;

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        LOG.info("vlan driver initialized");
        getDriverRegisterDependency().register(driver = new VlanDriver());

        return () -> getDriverRegisterDependency().remove(driver);
    }
}
