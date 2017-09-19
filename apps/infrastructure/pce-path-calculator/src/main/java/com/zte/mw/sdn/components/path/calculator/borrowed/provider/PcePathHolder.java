/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.ServiceHsbPathInstanceData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.ServicePathInstanceData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.TunnelGroupPathInstanceData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.TunnelHsbPathInstanceData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.TunnelPathInstanceData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicehsbpathinstancedata.ServiceHsbPathsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicepathinstancedata.ServicePathsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelgrouppathinstancedata.TunnelGroupsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelhsbpathinstancedata.TunnelHsbsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelpathinstancedata.TunnelPathsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.calendartunnel.CalendarTunnelMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelUnifyRecordKey;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelsRecordPerPort;
import com.zte.mw.sdn.components.path.calculator.borrowed.servicepath.ServiceHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.servicepath.ServicePathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.topology.TunnelsRecordPerTopology;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.TunnelGroupPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.TunnelGroupPathKey;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelhsbpath.TunnelHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.ITunnel;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathKey;

import com.zte.ngip.ipsdn.pce.path.api.RefreshTarget;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;

/**
 * Created by 10204924 on 2017/5/9.
 */
public class PcePathHolder {
    private PcePathHolder() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(PcePathHolder.class);
    private ConcurrentHashMap<TunnelPathKey, TunnelPathInstance> tunnelPaths = new ConcurrentHashMap<>();
    private ConcurrentHashMap<TunnelPathKey, TunnelHsbPathInstance> tunnelHsbPaths = new ConcurrentHashMap<>();
    private ConcurrentHashMap<TunnelGroupPathKey, TunnelGroupPathInstance> tunnelGroupPaths = new ConcurrentHashMap<>();
    private ConcurrentHashMap<TunnelPathKey, ServiceHsbPathInstance> serviceHsbPaths = new ConcurrentHashMap<>();
    private ConcurrentHashMap<TunnelPathKey, ServicePathInstance> servicePaths = new ConcurrentHashMap<>();
    private TunnelSegmentsAffectedByLinkCache tunnelSegmentsAffectedByLinkCache =
            new TunnelSegmentsAffectedByLinkCache();

    static PcePathHolder getInstance() {
        return SingletonHolder.instance;
    }

    private static void recoverTunnelGroupPathBw(TunnelGroupPathInstance tgInstance) {
        if (tgInstance == null) {
            Logs.error(LOG, "recoverTunnelGroupPathBw: TgInstance is null! ");
            return;
        }
        List<Link> path = tgInstance.getMasterLsp();
        TeArgumentBean teArg = tgInstance.getTeArgumentBean();
        if (!CollectionUtils.isNullOrEmpty(path)) {
            BandWidthMng.getInstance().recoverPathBw(path, teArg.getHoldPriority(), teArg.getBandWidth(),
                                                     new TunnelUnifyKey(tgInstance.getHeadNodeId(),
                                                                        tgInstance.getTunnelGroupId().intValue(), true,
                                                                        true,
                                                                        false), false);
        }

        List<Link> pathSlave = tgInstance.getSlaveLsp();
        if (!CollectionUtils.isNullOrEmpty(pathSlave)) {
            BandWidthMng.getInstance().recoverPathBw(pathSlave, teArg.getHoldPriority(), teArg.getBandWidth(),
                                                     new TunnelUnifyKey(tgInstance.getHeadNodeId(),
                                                                        tgInstance.getTunnelGroupId().intValue(), true,
                                                                        false, false), false);
        }
    }

    void removeTunnelPath(TunnelPathKey pathKey) {
        tunnelPaths.remove(pathKey);
    }

    void removeTunnelGroupPath(TunnelGroupPathKey pathKey) {
        tunnelGroupPaths.remove(pathKey);
    }

    void putTunnelHsbPath(TunnelPathKey pathKey, TunnelHsbPathInstance tunnelHsbPathInstance) {
        tunnelHsbPaths.put(pathKey, tunnelHsbPathInstance);
    }

    void putTunnelGroupPath(TunnelGroupPathKey pathKey, TunnelGroupPathInstance tunnelGroupPathInstance) {
        tunnelGroupPaths.put(pathKey, tunnelGroupPathInstance);
    }

    /**
     * write tunnel path instance.
     *
     * @param path tunnel path instance
     */
    void writeTunnel(TunnelPathInstance path) {
        if (null == path) {
            return;
        }
        TunnelPathKey key = new TunnelPathKey(path.getHeadNodeId(), path.getTunnelId());
        if (!path.isSimulate()) {
            tunnelPaths.put(key, path);
            path.writeDb();
        } else {
            CalendarTunnelMng.getInstance().putTunnelInstance(key, path);
        }
    }

    void writeHsbTunnel(TunnelHsbPathInstance path) {
        if (null == path) {
            return;
        }
        TunnelPathKey key = new TunnelPathKey(path.getHeadNodeId(), path.getTunnelId().intValue());
        if (!path.isSimulate()) {
            tunnelHsbPaths.put(key, path);
            path.writeDb();
        } else {
            CalendarTunnelMng.getInstance().putTunnelHsbInstance(key, path);
        }
    }

    void writeService(ServiceHsbPathInstance path) {
        if (null == path) {
            return;
        }
        TunnelPathKey key = new TunnelPathKey(path.getHeadNode(), path.getServiceName());
        if (!path.isSimulate()) {
            serviceHsbPaths.put(key, path);
            path.writeDb();
        }
    }

    void writeService(ServicePathInstance path) {
        if (null == path) {
            return;
        }
        TunnelPathKey key = new TunnelPathKey(path.getHeadNode(), path.getServiceName());
        if (!path.isSimulate()) {
            servicePaths.put(key, path);
            path.writeDb();
        }
    }

    /**
     * update normal tunnel.
     *
     * @param tunnel tunnel path instance
     */
    void updateTunnel(TunnelPathInstance tunnel) {
        TunnelUnifyKey key = tunnel.getTunnelUnifyKey();
        if (key.isSimulate()) {
            CalendarTunnelMng.getInstance()
                    .putTunnelInstance(new TunnelPathKey(key.getHeadNode(), key.getTunnelId()), tunnel);
        } else {
            tunnelPaths.put(new TunnelPathKey(key.getHeadNode(), key.getTunnelId()), tunnel);
        }
    }

    void updateTunnel(ServiceHsbPathInstance service) {
        TunnelUnifyKey key = service.getTunnelUnifyKey();
        if (!key.isSimulate()) {
            serviceHsbPaths.put(new TunnelPathKey(key.getHeadNode(), key.getServiceName()), service);
        }
    }

    void updateTunnel(ServicePathInstance service) {
        TunnelUnifyKey key = service.getTunnelUnifyKey();
        if (!key.isSimulate()) {
            servicePaths.put(new TunnelPathKey(key.getHeadNode(), key.getServiceName()), service);
        }
    }

    /**
     * update hsb tunnel instance.
     *
     * @param tunnel hsb tunnel
     */
    void updateTunnel(TunnelHsbPathInstance tunnel) {
        if (tunnel.isSimulate()) {
            CalendarTunnelMng.getInstance()
                    .putTunnelHsbInstance(
                            new TunnelPathKey(tunnel.getHeadNodeId(), tunnel.getTunnelId().intValue()),
                            tunnel);
        } else {
            tunnelHsbPaths.put(new TunnelPathKey(tunnel.getHeadNodeId(), tunnel.getTunnelId().intValue()), tunnel);
        }
    }

    TunnelPathInstance getTunnelInstance(NodeId headNode, int tunnelId, boolean isSimulate) {
        if (isSimulate) {
            return CalendarTunnelMng.getInstance().getTunnelInstance(headNode, tunnelId);
        } else {
            return tunnelPaths.get(new TunnelPathKey(headNode, tunnelId));
        }
    }

    TunnelPathInstance getTunnelPathInstance(TunnelPathKey pathKey) {
        return tunnelPaths.get(pathKey);
    }

    TunnelHsbPathInstance getTunnelHsbInstance(NodeId headNode, int tunnelId, boolean isSimulate) {
        if (isSimulate) {
            return CalendarTunnelMng.getInstance().getTunnelHsbInstance(headNode, tunnelId);
        } else {
            return tunnelHsbPaths.get(new TunnelPathKey(headNode, tunnelId));
        }
    }

    Map<TunnelPathKey, TunnelPathInstance> getTunnelPaths() {
        return tunnelPaths;
    }

    Map<TunnelPathKey, TunnelHsbPathInstance> getTunnelHsbPaths() {
        return tunnelHsbPaths;
    }

    void tnnlPathDbRecovery() throws ExecutionException, InterruptedException {
        Optional<TunnelPathInstanceData> tunnelPathsData = PcePathDb.getInstance().dataBroker
                .read(LogicalDatastoreType.CONFIGURATION, PcePathDb.getInstance().buildTnnlPathDbRootPath()).get();
        if (tunnelPathsData.isPresent()) {
            tnnlPathRecoveryDataFromDb(tunnelPathsData.get());
        } else {
            PcePathDb.getInstance().tunnelPathWriteDbRoot();
        }
    }

    private void tnnlPathRecoveryDataFromDb(TunnelPathInstanceData tnnlPathData) throws ExecutionException {
        if ((null == tnnlPathData) || (null == tnnlPathData.getTunnelPathsData())) {
            return;
        }
        for (TunnelPathsData data : tnnlPathData.getTunnelPathsData()) {
            TunnelPathInstance path = getTunnelInstance(data.getHeadNodeId(), data.getTunnelId());
            TunnelPathKey key = new TunnelPathKey(data.getHeadNodeId(), data.getTunnelId());

            if (path == null) {
                TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoGraphRecover(data.getTopologyId());
                path = PcePathDb.getInstance().tunnelPathConvert(data);
                tunnelPaths.put(key, path);
                TunnelUnifyKey tunnelRecordKey = new TunnelUnifyRecordKey(path.getTunnelUnifyKey());
                TunnelsRecordPerTopology.getInstance().add(path.getTopoId(), tunnelRecordKey);

                TunnelsRecordPerPort.getInstance().update(tunnelRecordKey, null, path.getPath());
                path.recoverTunnelPathBw();
            } else {
                Logs.error(
                        LOG,
                        "tnnlPathRecoveryDataFromDb: path is not null:{" + path.getHeadNodeId() + path.getTunnelId()
                                + "}!");
            }
        }
    }

    public TunnelPathInstance getTunnelInstance(NodeId headNode, int tunnelId) {
        return tunnelPaths.get(new TunnelPathKey(headNode, tunnelId));
    }

    void tnnlHsbDbRecovery() throws ExecutionException, InterruptedException {
        Optional<TunnelHsbPathInstanceData> tunnelHsbsData = PcePathDb.getInstance().dataBroker
                .read(LogicalDatastoreType.CONFIGURATION, PcePathDb.getInstance().buildTunnelHsbDbRootPath()).get();
        if (tunnelHsbsData.isPresent()) {
            tnnlHsbRecoveryDataFromDb(tunnelHsbsData.get());
        } else {
            PcePathDb.getInstance().tunnelHsbWriteDbRoot();
        }
    }

    private void tnnlHsbRecoveryDataFromDb(TunnelHsbPathInstanceData tunnelHsbData) throws ExecutionException {
        if (null == tunnelHsbData || null == tunnelHsbData.getTunnelHsbsData()) {
            return;
        }
        for (TunnelHsbsData data : tunnelHsbData.getTunnelHsbsData()) {
            TunnelHsbPathInstance path = getTunnelHsbInstance(data.getHeadNodeId(), data.getTunnelId().intValue());
            TunnelPathKey key = new TunnelPathKey(data.getHeadNodeId(), data.getTunnelId().intValue());
            if (path == null) {
                TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoGraphRecover(data.getTopologyId());
                path = PcePathDb.getInstance().tunnelHsbPathConvert(data);
                tunnelHsbPaths.put(key, path);

                TunnelUnifyKey masterRecordKey = new TunnelUnifyRecordKey(path.getMasterTunnelUnifyKey());
                TunnelUnifyKey slaveRecordKey = new TunnelUnifyRecordKey(path.getSlaveTunnelUnifyKey());

                TunnelsRecordPerTopology.getInstance().add(path.getTopoId(), masterRecordKey);
                TunnelsRecordPerTopology.getInstance().add(path.getTopoId(), slaveRecordKey);

                TunnelsRecordPerPort.getInstance().update(masterRecordKey, null, path.getMasterLsp());
                TunnelsRecordPerPort.getInstance().update(slaveRecordKey, null, path.getSlaveLsp());
                path.recoverHsbPathBw();
            }
        }
    }

    public TunnelHsbPathInstance getTunnelHsbInstance(NodeId headNode, int tunnelId) {
        return tunnelHsbPaths.get(new TunnelPathKey(headNode, tunnelId));
    }

    void serviceRecovery() {
        try {
            serviceDbRecovery();
        } catch (InterruptedException | ExecutionException e) {
            Logs.debug(LOG, "serviceDbRecovery read db failed " + e);
        }

        try {
            serviceHsbDbRecovery();
        } catch (InterruptedException | ExecutionException e) {
            Logs.debug(LOG, "serviceHsbDbRecovery read db failed " + e);
        }
    }

    private void serviceDbRecovery() throws ExecutionException, InterruptedException {
        Optional<ServicePathInstanceData> serviceDbData = PcePathDb.getInstance().dataBroker
                .read(LogicalDatastoreType.CONFIGURATION, PcePathDb.getInstance().buildServiceDbRootPath()).get();
        if (serviceDbData.isPresent()) {
            Logs.info(LOG, "serviceDbRecovery");
            serviceRecoveryDataFromDb(serviceDbData.get());
        } else {
            PcePathDb.getInstance().serviceWriteDbRoot();
        }
    }

    private void serviceHsbDbRecovery() throws ExecutionException, InterruptedException {
        Optional<ServiceHsbPathInstanceData> serviceHsbDbData = PcePathDb.getInstance().dataBroker
                .read(LogicalDatastoreType.CONFIGURATION, PcePathDb.getInstance().buildServiceHsbDbRootPath()).get();
        if (serviceHsbDbData.isPresent()) {
            Logs.info(LOG, "serviceHsbDbRecovery");
            serviceHsbRecoveryDataFromDb(serviceHsbDbData.get());
        } else {
            PcePathDb.getInstance().serviceHsbWriteDbRoot();
        }
    }

    private void serviceRecoveryDataFromDb(ServicePathInstanceData servicePathInstanceData) throws ExecutionException {
        if (null == servicePathInstanceData || null == servicePathInstanceData.getServicePathsData()) {
            return;
        }
        for (ServicePathsData data : servicePathInstanceData.getServicePathsData()) {
            ServicePathInstance path = getServiceInstance(data.getHeadNodeId(), data.getServiceName());
            TunnelPathKey key = new TunnelPathKey(data.getHeadNodeId(), data.getServiceName());
            if (path == null) {
                TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoGraphRecover(data.getTopologyId());
                Logs.info(LOG, "serviceRecoveryDataFromDb {}", data);
                path = new ServicePathInstance(data);
                servicePaths.put(key, path);

                TunnelUnifyKey recordKey = new TunnelUnifyRecordKey(path.getTunnelUnifyKey());
                TunnelsRecordPerTopology.getInstance().add(path.getTopoId(), recordKey);
                TunnelsRecordPerPort.getInstance().update(recordKey, null, path.getPath());
                path.recoverTunnelPathBw();
            }
        }
    }

    private void serviceHsbRecoveryDataFromDb(ServiceHsbPathInstanceData serviceHsbPathInstanceData)
            throws ExecutionException {
        if (null == serviceHsbPathInstanceData || null == serviceHsbPathInstanceData.getServiceHsbPathsData()) {
            return;
        }
        for (ServiceHsbPathsData data : serviceHsbPathInstanceData.getServiceHsbPathsData()) {
            ServiceHsbPathInstance path = getServiceHsbInstance(data.getHeadNodeId(), data.getServiceName());
            TunnelPathKey key = new TunnelPathKey(data.getHeadNodeId(), data.getServiceName());
            if (path == null) {
                TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoGraphRecover(data.getTopologyId());
                Logs.info(LOG, "serviceHsbRecoveryDataFromDb {}", data);
                path = new ServiceHsbPathInstance(data);
                serviceHsbPaths.put(key, path);

                TunnelUnifyKey masterRecordKey = new TunnelUnifyRecordKey(path.getMasterTunnelUnifyKey());
                TunnelUnifyKey slaveRecordKey = new TunnelUnifyRecordKey(path.getSlaveTunnelUnifyKey());

                TunnelsRecordPerTopology.getInstance().add(path.getTopoId(), masterRecordKey);
                TunnelsRecordPerTopology.getInstance().add(path.getTopoId(), slaveRecordKey);

                TunnelsRecordPerPort.getInstance().update(masterRecordKey, null, path.getMasterLsp());
                TunnelsRecordPerPort.getInstance().update(slaveRecordKey, null, path.getSlaveLsp());
                path.recoverHsbPathBw();
            }
        }
    }

    ServicePathInstance getServiceInstance(NodeId headNode, String serviceName) {
        return servicePaths.get(new TunnelPathKey(headNode, serviceName));
    }

    ServiceHsbPathInstance getServiceHsbInstance(NodeId headNode, String serviceName) {
        return serviceHsbPaths.get(new TunnelPathKey(headNode, serviceName));
    }

    TunnelGroupPathInstance getTunnelGroupInstance(TunnelGroupPathKey pathKey) {
        return tunnelGroupPaths.get(pathKey);
    }

    void tnnlGroupDbRecovery() throws ExecutionException, InterruptedException {
        Optional<TunnelGroupPathInstanceData> tunnelGroupsData = PcePathDb.getInstance().dataBroker
                .read(LogicalDatastoreType.CONFIGURATION, PcePathDb.buildTgsDbRootPath()).get();
        if (tunnelGroupsData.isPresent()) {
            tnnlGroupRecoveryDataFromDb(tunnelGroupsData.get());
        } else {
            PcePathDb.getInstance().tunnelGroupWriteDbRoot();
        }
    }

    private void tnnlGroupRecoveryDataFromDb(TunnelGroupPathInstanceData tgData) throws ExecutionException {
        if ((null == tgData) || (null == tgData.getTunnelGroupsData())) {
            return;
        }
        for (TunnelGroupsData data : tgData.getTunnelGroupsData()) {
            TunnelGroupPathInstance path =
                    getTunnelGroupInstance(data.getHeadNodeId(), data.getTunnelGroupId().intValue());
            TunnelGroupPathKey key = new TunnelGroupPathKey(data.getHeadNodeId(), data.getTunnelGroupId().intValue());
            if (path == null) {
                TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoGraphRecover(data.getTopologyId());
                path = PcePathDb.getInstance().tunnelGroupPathConvert(data);
                tunnelGroupPaths.put(key, path);
                TunnelsRecordPerTopology.getInstance().add(path.getTopoId(), path.getMasterTunnelUnifyKey());
                TunnelsRecordPerTopology.getInstance().add(path.getTopoId(), path.getSlaveTunnelUnifyKey());
                TunnelsRecordPerPort.getInstance().update(path.getMasterTunnelUnifyKey(), null, path.getMasterLsp());
                TunnelsRecordPerPort.getInstance().update(path.getSlaveTunnelUnifyKey(), null, path.getSlaveLsp());
                recoverTunnelGroupPathBw(path);
            }
        }
    }

    TunnelGroupPathInstance getTunnelGroupInstance(NodeId headNode, int tgId) {
        return tunnelGroupPaths.get(new TunnelGroupPathKey(headNode, tgId));
    }

    /**
     * getAllServices.
     *
     * @return all services
     */
    public List<ITunnel> getAllServices() {
        List<ITunnel> allServices = new ArrayList<>();
        allServices.addAll(getCommonServiceList());
        allServices.addAll(getHsbServiceList());
        return allServices;
    }

    public List<ITunnel> getCommonServiceList() {
        return new ArrayList<>(servicePaths.values());
    }

    public List<ITunnel> getHsbServiceList() {
        return new ArrayList<>(serviceHsbPaths.values());
    }

    void deleteReverseTunnel(ITunnel positiveTunnel) {
        if (positiveTunnel == null || !BiDirect.isBiDirectPositive(positiveTunnel.getBiDirect())) {
            return;
        }
        ITunnel reverseTunnel = getTunnelInstance(
                new TunnelUnifyKey(positiveTunnel.getTunnelUnifyKey(), positiveTunnel.getTailNode(),
                                   positiveTunnel.getBiDirect().getReverseId()));
        if (reverseTunnel != null) {
            Logs.debug(LOG, "remove reverse tunnels {}", reverseTunnel);
            reverseTunnel.destroy();
            removeTunnelInstance(reverseTunnel);
        } else {
            Logs.error(LOG, "Reverse tunnel:{} {} not in tunnelPaths", positiveTunnel.getTailNode(),
                       positiveTunnel.getBiDirect().getReverseId());
        }
    }

    /**
     * get tunnel instance br tunnel key.
     *
     * @param tunnelUnifyKey tunnelPathKey
     * @return ITunnel
     */
    public ITunnel getTunnelInstance(TunnelUnifyKey tunnelUnifyKey) {
        Logs.debug(LOG, "getTunnelInstance {}", tunnelUnifyKey);
        ITunnel tunnel = null;
        if (tunnelUnifyKey.isServiceInstance()) {
            tunnel = tunnelUnifyKey.isHsbFlag()
                    ? getServiceHsbInstance(tunnelUnifyKey.getHeadNode(), tunnelUnifyKey.getServiceName()) :
                    getServiceInstance(tunnelUnifyKey.getHeadNode(), tunnelUnifyKey.getServiceName());
        } else if (tunnelUnifyKey.isNormalTunnel()) {
            tunnel = getTunnelPathInstance(tunnelUnifyKey.getHeadNode(), tunnelUnifyKey.getId(),
                                           tunnelUnifyKey.isSimulate());
        } else if (tunnelUnifyKey.isHsbFlag()) {
            tunnel = getTunnelHsbInstanceByKey(tunnelUnifyKey.getHeadNode(), tunnelUnifyKey.getId(),
                                               tunnelUnifyKey.isSimulate());
        } else if (tunnelUnifyKey.isTg()) {
            tunnel = getTunnelGroupInstance(tunnelUnifyKey.getHeadNode(), tunnelUnifyKey.getTgId());
        }
        if (tunnel == null) {
            Logs.warn(LOG, "cannot find tunnel instance by key {}", tunnelUnifyKey);
        }
        return tunnel;
    }

    private void removeTunnelInstance(ITunnel tunnel) {
        if (tunnel instanceof TunnelPathInstance) {
            removeTunnelPathInstance((TunnelPathInstance) tunnel);
        } else if (tunnel instanceof TunnelHsbPathInstance) {
            removeTunnelHsbPathInstance((TunnelHsbPathInstance) tunnel);
        } else {
            removeTunnelGroupPathInstance((TunnelGroupPathInstance) tunnel);
        }
    }

    /**
     * get tunnel path instance.
     *
     * @param headNode   tunnel head
     * @param tunnelId   tunnel id
     * @param isSimulate is simulate
     * @return TunnelPathInstance
     */
    TunnelPathInstance getTunnelPathInstance(NodeId headNode, int tunnelId, Boolean isSimulate) {
        TunnelPathKey key = new TunnelPathKey(headNode, tunnelId);
        TunnelPathInstance path;
        if (ComUtility.isSimulateTunnel(isSimulate)) {
            path = CalendarTunnelMng.getInstance().getTunnelInstance(key);
        } else {
            path = tunnelPaths.get(key);
        }
        return path;
    }

    TunnelHsbPathInstance getTunnelHsbInstanceByKey(NodeId headNode, int tunnelId, Boolean isSimulate) {
        TunnelPathKey key = new TunnelPathKey(headNode, tunnelId);
        TunnelHsbPathInstance path;
        if (ComUtility.isSimulateTunnel(isSimulate)) {
            path = CalendarTunnelMng.getInstance().getTunnelHsbInstance(key);
        } else {
            path = tunnelHsbPaths.get(key);
        }
        return path;
    }

    /**
     * remove tunnel instance.
     *
     * @param path tunnel path instance
     */
    void removeTunnelPathInstance(TunnelPathInstance path) {
        if (null == path) {
            return;
        }
        TunnelPathKey key = new TunnelPathKey(path.getHeadNodeId(), path.getTunnelId());
        if (!path.isSimulate()) {
            tunnelPaths.remove(key);
            path.removeDb();
        } else {
            CalendarTunnelMng.getInstance().removeTunnelInstance(key);
        }
    }

    void removeTunnelHsbPathInstance(TunnelHsbPathInstance path) {
        if (null == path) {
            return;
        }

        TunnelPathKey key = new TunnelPathKey(path.getHeadNodeId(), path.getTunnelId().intValue());
        if (!path.isSimulate()) {
            tunnelHsbPaths.remove(key);
            path.removeDb();
        } else {
            CalendarTunnelMng.getInstance().removeTunnelHsbInstance(key);
        }
    }

    private void removeTunnelGroupPathInstance(TunnelGroupPathInstance path) {
        if (null == path) {
            return;
        }

        TunnelGroupPathKey key = new TunnelGroupPathKey(path.getHeadNodeId(), path.getTunnelGroupId().intValue());
        if (!path.isSimulate()) {
            tunnelGroupPaths.remove(key);
            path.removeDb();
        } else {
            throw new UnsupportedOperationException("Removing calendar TunnelGroupPathInstance is not supported yet!");
        }
    }

    void destroy() {
        tunnelPaths.clear();
        tunnelHsbPaths.clear();
        tunnelGroupPaths.clear();
        servicePaths.clear();
        serviceHsbPaths.clear();
    }

    Set<ITunnel> getTunnelsAffectedByLinksNeedRefreshSegments(
            boolean isSimulate, List<Link> links,
            Set<RefreshTarget> refreshTargets) {
        if (CollectionUtils.isNullOrEmpty(links) || CollectionUtils.isNullOrEmpty(refreshTargets) || !refreshTargets
                .contains(RefreshTarget.ALL_SEGMENTS)) {
            return Collections.emptySet();
        }
        Set<ITunnel> needRefreshSegments = tunnelSegmentsAffectedByLinkCache
                .getAffectedTunnels(links, this::getTunnelInstance,
                                    () -> getTunnelsNeedRefreshSegments(
                                            isSimulate, Sets.newHashSet(RefreshTarget.ALL_SEGMENTS)));
        Logs.debug(LOG, "segments affected: {}", needRefreshSegments);
        return needRefreshSegments;
    }

    private Set<ITunnel> getTunnelsNeedRefreshSegments(boolean isSimulate, Set<RefreshTarget> refreshTargets) {
        if (refreshTargets.contains(RefreshTarget.ALL_SEGMENTS)) {
            return getAllTunnels(isSimulate).stream().filter(ITunnel::isSrTunnel).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    List<ITunnel> getAllTunnels(boolean isSimulate) {
        List<ITunnel> tunnels = new ArrayList<>();

        tunnels.addAll(getAllTunnelPath(isSimulate));
        tunnels.addAll(getAllTunnelHsbPath(isSimulate));
        tunnels.addAll(getAllTunnelGroupPath(isSimulate));
        return tunnels;
    }

    private Collection<TunnelPathInstance> getAllTunnelPath(boolean isSimulate) {
        Collection<TunnelPathInstance> paths;
        if (ComUtility.isSimulateTunnel(isSimulate)) {
            paths = CalendarTunnelMng.getInstance().getAllTunnelInstance();
        } else {
            paths = tunnelPaths.values();
        }
        return paths;
    }

    Collection<TunnelHsbPathInstance> getAllTunnelHsbPath(boolean isSimulate) {
        Collection<TunnelHsbPathInstance> paths;
        if (ComUtility.isSimulateTunnel(isSimulate)) {
            paths = CalendarTunnelMng.getInstance().getAllTunnelHsbInstance();
        } else {
            paths = tunnelHsbPaths.values();
        }
        return paths;
    }

    private Collection<TunnelGroupPathInstance> getAllTunnelGroupPath(boolean isSimulate) {
        Collection<TunnelGroupPathInstance> paths;
        if (ComUtility.isSimulateTunnel(isSimulate)) {
            paths = CalendarTunnelMng.getInstance().getAllTunnelGroupInstance();
        } else {
            paths = tunnelGroupPaths.values();
        }
        return paths;
    }

    void updateTunnelSegmentsAffectedByLinkCache(Set<Link> usedLinks, TunnelUnifyKey tunnelUnifyKey) {
        tunnelSegmentsAffectedByLinkCache.update(usedLinks, tunnelUnifyKey);
    }

    /**
     * printTunnelInfo.
     *
     * @param headNodeId headNodeId
     * @param tunnelId   tunnelId
     */
    void printTunnelInfo(NodeId headNodeId, int tunnelId) {
        tunnelPaths.values().stream().filter(tunnelPath -> tunnelPath.isMatched(headNodeId, tunnelId))
                .forEach(tunnelPath -> {
                    tunnelPath.printDetailInfo();
                    ComUtility.debugInfoLog("\n");
                });
    }

    /**
     * printAllTunnelInfo.
     */
    void printAllTunnelInfo() {
        tunnelPaths.values().forEach(tunnelPath -> {
            tunnelPath.printSummaryInfo();
            ComUtility.debugInfoLog("\n");
        });
    }

    /**
     * printTunnelGroupInfo.
     *
     * @param headNodeId headNodeId
     * @param tunnelId   tunnelId
     */
    void printTunnelGroupInfo(NodeId headNodeId, int tunnelId) {
        tunnelGroupPaths.values().stream().filter(tunnelGroupPath -> tunnelGroupPath.isMatched(headNodeId, tunnelId))
                .forEach(tunnelGroupPath -> {
                    tunnelGroupPath.printDetailInfo();
                    ComUtility.debugInfoLog("\n");
                });
    }

    /**
     * printAllTunnelGroupInfo.
     */
    void printAllTunnelGroupInfo() {
        tunnelGroupPaths.values().forEach(tunnelGroupPath -> {
            tunnelGroupPath.printSummaryInfo();
            ComUtility.debugInfoLog("\n");
        });
    }

    /**
     * printTunnelHsbInfo.
     *
     * @param headNodeId headNodeId
     * @param tunnelId   tunnelId
     */
    void printTunnelHsbInfo(NodeId headNodeId, int tunnelId) {
        tunnelHsbPaths.values().stream().filter(tunnelHsbPath -> tunnelHsbPath.isMatched(headNodeId, tunnelId))
                .forEach(tunnelHsbPath -> {
                    tunnelHsbPath.printDetailInfo();
                    ComUtility.debugInfoLog("\n");
                });
    }

    /**
     * printAllTunnelHsbInfo.
     */
    void printAllTunnelHsbInfo() {
        tunnelHsbPaths.values().forEach(tunnelHsbPath -> {
            tunnelHsbPath.printSummaryInfo();
            ComUtility.debugInfoLog("\n");
        });
    }

    void tunnelNumPrint() {
        int noUpSum = 0;
        ComUtility.debugInfoLog("tunnelPath num:" + tunnelPaths.size());
        ComUtility.debugInfoLog("tunnelGroupPath num:" + tunnelGroupPaths.size());
        ComUtility.debugInfoLog("tunnelHsbPath num:" + tunnelHsbPaths.size());
        for (TunnelPathInstance tunnelPath : tunnelPaths.values()) {
            if (CollectionUtils.isNullOrEmpty(tunnelPath.getPath())) {
                noUpSum++;
            }
        }
        ComUtility.debugInfoLog("no path tunnel num:" + noUpSum);
    }

    ServicePathInstance removeServiceInstance(NodeId headNode, String serviceName) {
        return servicePaths.remove(new TunnelPathKey(headNode, serviceName));
    }

    ServiceHsbPathInstance removeServiceHsbInstance(NodeId headNode, String serviceName) {
        return serviceHsbPaths.remove(new TunnelPathKey(headNode, serviceName));
    }

    private static class SingletonHolder {
        private SingletonHolder() {
        }

        private static PcePathHolder instance = new PcePathHolder();
    }
}
