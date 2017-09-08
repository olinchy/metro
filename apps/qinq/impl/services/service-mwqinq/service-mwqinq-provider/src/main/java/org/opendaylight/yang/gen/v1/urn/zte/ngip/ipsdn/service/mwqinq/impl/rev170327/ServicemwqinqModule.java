package org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.impl.rev170327;

import com.zte.ngip.ipsdn.impl.ServicemwqinqProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicemwqinqModule extends org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.impl.rev170327.AbstractServicemwqinqModule {
    public ServicemwqinqModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
        getLogger().info("ServicemwqinqModule construct!");
    }

    public ServicemwqinqModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.impl.rev170327.ServicemwqinqModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        getLogger().info("ServicemwqinqModule initialized!");
        //throw new java.lang.UnsupportedOperationException();

        ServicemwqinqProvider servicemwqinq = new ServicemwqinqProvider();

        final class ServicemwqinqAutoClose implements AutoCloseable {
            @Override
            public void close() throws Exception {
                servicemwqinq.close();
            }
        }
        return new ServicemwqinqAutoClose();
    }

}
