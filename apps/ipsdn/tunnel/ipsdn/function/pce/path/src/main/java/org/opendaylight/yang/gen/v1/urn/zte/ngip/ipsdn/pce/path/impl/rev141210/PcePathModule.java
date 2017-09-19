package org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.impl.rev141210;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.leader.rev160128.DistributedLeaderListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.leader.rev160128.DistributedLeaderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.leader.rev160128.GetConfigLeaderOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.leader.rev160128.LeaderChange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.leader.rev160128.Leaders;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.PceHControlService;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.PcePathService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.zte.ngip.ipsdn.pce.path.api.TopoChangeProviderContext;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;
import com.zte.ngip.ipsdn.pce.path.impl.bandwidth.BandWidthMng;
import com.zte.ngip.ipsdn.pce.path.impl.bandwidth.BwSharedGroupMng;
import com.zte.ngip.ipsdn.pce.path.impl.calendartunnel.CalendarTunnelMng;
import com.zte.ngip.ipsdn.pce.path.impl.command.FrameworkDebugCommandProvider;
import com.zte.ngip.ipsdn.pce.path.impl.level.LevelProvider;
import com.zte.ngip.ipsdn.pce.path.impl.provider.DbProvider;
import com.zte.ngip.ipsdn.pce.path.impl.provider.NotificationProvider;
import com.zte.ngip.ipsdn.pce.path.impl.provider.PceHServiceProvider;
import com.zte.ngip.ipsdn.pce.path.calculator.impl.PcePathCalculatorImpl;
import com.zte.ngip.ipsdn.pce.path.impl.provider.PcePathProvider;
import com.zte.ngip.ipsdn.pce.path.impl.topology.TopoChangeServiceProxy;
import com.zte.ngip.ipsdn.pce.path.impl.topology.TunnelsRecordPerTopology;
import com.zte.ngip.ipsdn.pce.path.impl.util.BundleProperty;
import com.zte.ngip.ipsdn.pce.path.impl.util.DataBrokerDelegate;

public class PcePathModule extends AbstractPcePathModule {
    private BundleContext bundleContext;
    private BundleProperty bundleProperty;
    private BindingAwareBroker.RpcRegistration<PcePathService> pcePathRegistration = null;
    private BindingAwareBroker.RpcRegistration<PceHControlService> pceHServiceRegistration = null;
    private DistributedLeaderService distributedLeaderProvider;
    private ListeningExecutorService executor;
    private boolean isInit = false;
    private static final Logger LOG = LoggerFactory.getLogger(PcePathModule.class);
    private ListenerRegistration<NotificationListener> notificationReg = null;

    public PcePathModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight
            .controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PcePathModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                         org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                         org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.impl.rev141210
                                 .PcePathModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    public void setBundleContext(BundleContext context) {
        this.bundleContext = context;
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    private class ClusterSlaveToMasterTask implements Runnable {

        LeaderChange leaderChange;

        public ClusterSlaveToMasterTask(LeaderChange clusterInput) {
            this.leaderChange = clusterInput;
        }

        @Override
        public void run() {
            if ((null != leaderChange.isIsMaster()) && (leaderChange.isIsMaster())) {
                if (!isInit) {
                    LOG.info("PCE ClusterSlaveToMasterTask begin module init");
                    moduleInit();
                }
            } else {
                moduleClose();
            }
        }
    }

    private boolean isNeedModuleInit(Leaders leaders) {
        return leaders.isIsMaster() != null && leaders.isIsMaster() && !isInit;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

        distributedLeaderProvider = getRpcRegistryDependency().getRpcService(DistributedLeaderService.class);
        getNotificationServiceDependency().registerNotificationListener(
                (DistributedLeaderListener) leaderChange -> executor
                        .submit(new ClusterSlaveToMasterTask(leaderChange)));

        if (null != distributedLeaderProvider) {
            RpcResult<GetConfigLeaderOutput> out = null;
            try {
                out = distributedLeaderProvider.getConfigLeader().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("PCE createInstance error {}", e);
            }
            if ((null != out) && (out.isSuccessful()) && isNeedModuleInit(out.getResult())) {
                LOG.info("PCE createInstance begin module init");
                moduleInit();
            }
        }
        return new PcePathCalculatorImpl(PcePathProvider.getInstance(), this);
    }

    private void readCustomProperties() {
        BandWidthMng.getInstance().setBandwidthUtilization(bundleProperty.getBandUtilization());
        PcePathProvider.setTunnelDispatchBandwidth(bundleProperty.getTunnelDispatchBandwidthFlag());
    }


    void moduleInit() {
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
        RpcProviderRegistry rpcRegistryDependency = getRpcRegistryDependency();

        pcePathRegistration = rpcRegistryDependency
                .addRpcImplementation(PcePathService.class, pcePathProvider);
        pceHServiceRegistration = rpcRegistryDependency
                .addRpcImplementation(PceHControlService.class, pceHServiceProvider);

        TunnelsRecordPerTopology.getInstance().setPcePathService(pcePathProvider);
        CalendarTunnelMng.getInstance().setPcePathProvider(pcePathProvider);
        FrameworkDebugCommandProvider
                .init(pcePathProvider, BandWidthMng.getInstance(), getPceToporoviderServiceDependency(),
                        BwSharedGroupMng.getInstance(), LevelProvider.getInstance());
        LevelProvider.getInstance().init();
        // sr-lea
        pcePathProvider.setLabelEncodingService(getPceLabelEncodingServiceDependency());
    }

    public void moduleClose() {
        if (null != pcePathRegistration) {
            pcePathRegistration.close();
        }
        if (null != pceHServiceRegistration) {
            pceHServiceRegistration.close();
        }
        if (null != notificationReg) {
            notificationReg.close();
        }
    }
}