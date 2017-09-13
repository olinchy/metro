package org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.qinq.impl.rev170327;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QinqModule extends org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.qinq.impl.rev170327.AbstractQinqModule {
    public QinqModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
        LOG.info("QinqModule started 5");
    }

    public QinqModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, QinqModule oldModule, AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
        LOG.info("QinqModule started 10");
    }

    private static final Logger LOG = LoggerFactory.getLogger(QinqModule.class);

    @Override
    public void customValidation() {
        LOG.info("QinqModule started 15");
        // add custom validation form module attributes here.
    }

    @Override
    public AutoCloseable createInstance() {
        LOG.info("QinqModule started 21");
        // TODO:implement
        throw new UnsupportedOperationException();
    }

}
