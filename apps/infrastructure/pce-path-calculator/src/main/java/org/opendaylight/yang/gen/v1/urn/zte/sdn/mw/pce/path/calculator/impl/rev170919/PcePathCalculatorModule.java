package org.opendaylight.yang.gen.v1.urn.zte.sdn.mw.pce.path.calculator.impl.rev170919;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.PcePathCalculatorImpl;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroupMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.calendartunnel.CalendarTunnelMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.command.FrameworkDebugCommandProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.level.LevelProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.DbProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.NotificationProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceHServiceProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.topology.TopoChangeServiceProxy;
import com.zte.mw.sdn.components.path.calculator.borrowed.topology.TunnelsRecordPerTopology;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.BundleProperty;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.DataBrokerDelegate;

import com.zte.ngip.ipsdn.pce.path.api.TopoChangeProviderContext;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;

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
    private BundleProperty bundleProperty;
    private BundleContext bundleContext;

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("pce path calculator start to init");
        moduleInit();
        LOG.info("pce path calculator started");

        return new PcePathCalculatorImpl();
    }

    private void moduleInit() {
        DbProvider.getInstance().setDataBroker(getDataBrokerDependency());
        DataBrokerDelegate.getInstance().setDataBroker(getDataBrokerDependency());
        NotificationProvider.getInstance().setNotificationService(getNotificationServiceDependency());
        TopoServiceAdapter.getInstance().setPceTopoProvider(getPceToporoviderServiceDependency());
        TopoServiceAdapter.getInstance().setThreadFactory(getPceFactoryServiceDependency());
        bundleProperty = new BundleProperty();
        bundleProperty.setBundleContext(bundleContext);
        readCustomProperties();
        PcePathProvider pcePathProvider = PcePathProvider.getInstance();
        PceHServiceProvider pceHServiceProvider = new PceHServiceProvider();
        TopoChangeServiceProxy topoChangeServiceProxy =
                new TopoChangeServiceProxy(pcePathProvider, pceHServiceProvider, BandWidthMng.getInstance());
        TopoChangeProviderContext topoChangeProviderContext = getPceTopoChangeServiceDependency();
        topoChangeProviderContext.registerTopoChangeListener(topoChangeServiceProxy);
        pcePathProvider.recoveryDb();
        pcePathProvider.setZeroBandWidthFlag(getCanZeroBandWidth());

        TopoServiceAdapter.getInstance().getPceTopoProvider().regDbListenAll();

        TunnelsRecordPerTopology.getInstance().setPcePathService(pcePathProvider);
        CalendarTunnelMng.getInstance().setPcePathProvider(pcePathProvider);
        FrameworkDebugCommandProvider
                .init(pcePathProvider, BandWidthMng.getInstance(), getPceToporoviderServiceDependency(),
                      BwSharedGroupMng.getInstance(), LevelProvider.getInstance());
        LevelProvider.getInstance().init();
        // sr-lea
        pcePathProvider.setLabelEncodingService(getPceLabelEncodingServiceDependency());
    }

    private void readCustomProperties() {
        BandWidthMng.getInstance().setBandwidthUtilization(bundleProperty.getBandUtilization());
        PcePathProvider.setTunnelDispatchBandwidth(bundleProperty.getTunnelDispatchBandwidthFlag());
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
