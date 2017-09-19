/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.servicepath;

import java.util.ArrayList;
import java.util.Optional;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.ServiceHsbPathData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicehsbpathdata.ServiceMasterPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicehsbpathdata.ServiceSlavePathsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicehsbpathdata.SlaveTeArgument;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicehsbpathdata.SlaveTeArgumentBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicehsbpathinstancedata.ServiceHsbPathsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicehsbpathinstancedata.ServiceHsbPathsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.CreateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.UpdateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bi.direct.argument.BiDirectContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathDb;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.srlg.PriorityAvoidLinks;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.HsbNewTeArgBuilder;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.HsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.DataBrokerDelegate;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.ZeroBandwidthUtils;

import com.zte.ngip.ipsdn.pce.path.api.srlg.SrlgAttribute;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBeanLsp;

public class ServiceHsbPathInstance extends HsbPathInstance {
    /**
     * TunnelPathInstance.
     *
     * @param dbData dbData
     */
    public ServiceHsbPathInstance(ServiceHsbPathData dbData) {
        super(dbData.getHeadNodeId(), dbData.getServiceName());
        this.topoId = dbData.getTopologyId();
        this.headNodeId = dbData.getHeadNodeId();
        this.tailNodeId = dbData.getTailNodeId();
        this.serviceName = dbData.getServiceName();
        this.masterPath =
                PcePathDb.getInstance().pathLinks2Links(topoId, false, dbData.getServiceMasterPaths().getPathLink());
        this.slavePath =
                PcePathDb.getInstance().pathLinks2Links(topoId, false, dbData.getServiceSlavePaths().getPathLink());
        this.masterMetric = dbData.getServiceMasterPaths().getLspMetric();
        this.slaveMetric = dbData.getServiceSlavePaths().getLspMetric();
        this.masterDelay = dbData.getServiceMasterPaths().getLspDelay();
        this.slaveDelay = dbData.getServiceSlavePaths().getLspDelay();
        this.masterSrlgAttr = dbData.getServiceMasterPaths().getSrlgs() == null
                ? new SrlgAttribute() :
                new SrlgAttribute(new ArrayList<>(dbData.getServiceMasterPaths().getSrlgs()));
        this.slaveSrlgAttr = dbData.getServiceSlavePaths().getSrlgs() == null
                ? new SrlgAttribute() :
                new SrlgAttribute(new ArrayList<>(dbData.getServiceSlavePaths().getSrlgs()));
        this.teArg = setTeArgInfo(dbData);
        this.teArg.setComputeLspWithBandWidth(dbData.isComputeSlaveLspWithBandwidth());
        this.teArgSlave = setSlaveTeArgInfo(dbData);
        this.masterTunnelUnifyKey = getMasterTunnelUnifyKey(dbData.getHeadNodeId(), dbData.getServiceName(),
                                                            dbData.getBwSharedGroupContainer(),
                                                            dbData.getBiDirectContainer());
        this.slaveTunnelUnifyKey = getSlaveTunnelUnifyKey(dbData.getHeadNodeId(), dbData.getServiceName(),
                                                          dbData.getBwSharedGroupContainer(),
                                                          dbData.getBiDirectContainer());
        this.biDirect = dbData.getBiDirectContainer() == null
                ? null : (new DefaultBidirectImpl(dbData));
        this.calculateStrategy = dbData.getCalculateStrategyContainer();
        this.recalcWithoutDelay = (null == dbData.isRecalcWithoutDelay())
                ? false : dbData.isRecalcWithoutDelay();
        this.isSrlgEnabled = (null == dbData.isSrlgEnabled()) ? false : dbData.isSrlgEnabled();
        setBwSharedGroup(dbData.getBwSharedGroupContainer());
    }

    public ServiceHsbPathInstance(CreateServiceInput input) {
        super(input.getHeadNodeId(), input.getServiceName());
        this.topoId = ComUtility.getTopoId(input.getTopologyId());
        this.headNodeId = input.getHeadNodeId();
        this.tailNodeId = input.getTailNodeId();
        this.serviceName = input.getServiceName();
        this.masterTunnelUnifyKey = getMasterTunnelUnifyKey(input.getHeadNodeId(), input.getServiceName(),
                                                            input.getBwSharedGroupContainer(),
                                                            input.getBiDirectContainer());
        this.slaveTunnelUnifyKey =
                getSlaveTunnelUnifyKey(input.getHeadNodeId(), input.getServiceName(), input.getBwSharedGroupContainer(),
                                       input.getBiDirectContainer());
        this.teArg = new TeArgumentBean(input, ComUtility.getTopoId(input.getTopologyId()));
        this.teArgSlave = new TeArgumentBeanLsp(input.getSlaveTeArgument(), topoId);
        this.setBwSharedGroup(input.getBwSharedGroupContainer());
        this.setCalculateStrategy(input.getCalculateStrategyContainer());

        if (this.teArg != null && input.isComputeSlaveLspWithBandwidth() != null) {
            this.teArg.setComputeLspWithBandWidth(input.isComputeSlaveLspWithBandwidth());
        }
        this.setBiDirect((input.getBiDirectContainer() == null) ? null : (new DefaultBidirectImpl(input)));
        this.setRecalcWithoutDelay(input.isRecalcWithoutDelay());
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServiceHsbPathInstance.class);
    private String serviceName;

    private static TeArgumentBean setTeArgInfo(ServiceHsbPathData dbData) {
        TeArgumentBean teAttr = new TeArgumentBean(dbData.getTeArgCommonData());
        teAttr.setExcludedNodes(dbData.getExcludingNode());
        teAttr.setExcludedPorts(dbData.getExcludingPort());
        teAttr.setNextAddress(dbData.getNextAddress());
        teAttr.setTryToAvoidLink(dbData.getTryToAvoidLink());
        teAttr.setContrainedAddress(dbData.getContrainedAddress(), dbData.getTopologyId());
        teAttr.setAffinityStrategy(dbData.getAffinityStrategy());
        return teAttr;
    }

    private static TeArgumentBeanLsp setSlaveTeArgInfo(ServiceHsbPathData dbData) {
        TeArgumentBeanLsp teAttr = new TeArgumentBeanLsp();
        teAttr.setExcludedNodes(dbData.getSlaveTeArgument().getExcludingNode());
        teAttr.setExcludedPorts(dbData.getSlaveTeArgument().getExcludingPort());
        teAttr.setNextAddress(dbData.getSlaveTeArgument().getNextAddress());
        teAttr.setTryToAvoidLink(dbData.getSlaveTeArgument().getTryToAvoidLink());
        teAttr.setContrainedAddress(dbData.getSlaveTeArgument().getContrainedAddress(), dbData.getTopologyId());
        teAttr.setAffinityStrategy(dbData.getSlaveTeArgument().getAffinityStrategy());
        return teAttr;
    }

    private static TunnelUnifyKey getMasterTunnelUnifyKey(
            NodeId nodeId, String serviceName,
            BwSharedGroupContainer bwSharedGroup, BiDirectContainer biDirect) {
        return new TunnelUnifyKey(nodeId, serviceName, false, true, true, bwSharedGroup != null,
                                  PceUtil.isBiDirectional(biDirect));
    }

    private static TunnelUnifyKey getSlaveTunnelUnifyKey(
            NodeId nodeId, String serviceName,
            BwSharedGroupContainer bwSharedGroup, BiDirectContainer biDirect) {
        return new TunnelUnifyKey(nodeId, serviceName, false, false, true, bwSharedGroup != null,
                                  PceUtil.isBiDirectional(biDirect));
    }

    @Override
    public void writeDb() {
        if (getTunnelState() == TUNNEL_INVALID) {
            Logs.info(LOG, "tunnel state is invalid can not write db {}", masterTunnelUnifyKey);
            return;
        }
        if (!getTunnelUnifyKey().isSimulate()) {
            DataBrokerDelegate.getInstance().put(
                    LogicalDatastoreType.CONFIGURATION,
                    PcePathDb.buildServiceHsbPathDbPath(headNodeId, getServiceName()),
                    serviceHsbPathsInstanceCreate());
        }
    }

    @Override
    public void removeDb() {
        if (!getTunnelUnifyKey().isSimulate()) {
            DataBrokerDelegate.getInstance().delete(
                    LogicalDatastoreType.CONFIGURATION,
                    PcePathDb.buildServiceHsbPathDbPath(headNodeId, getServiceName()));
        }
    }

    @Override
    public void writeMemory() {
        if (getTunnelState() == TUNNEL_INVALID) {
            Logs.info(LOG, "tunnel state is invalid can not write memory {}", masterTunnelUnifyKey);
            return;
        }
        PcePathProvider.getInstance().updateTunnel(this);
    }

    @Override
    public void notifySegmentsChange() {
        // do nothing
    }

    @Override
    public boolean isSimulate() {
        return getTunnelUnifyKey().isSimulate();
    }

    private ServiceHsbPathsData serviceHsbPathsInstanceCreate() {
        SlaveTeArgument slaveTeArgument = new SlaveTeArgumentBuilder()
                .setTryToAvoidLink(PcePathDb.tryToAvoidLinkConvert(getSlaveTeArg().getTryToAvoidLink()))
                .setNextAddress(PcePathDb.nextAddressConvert(getSlaveTeArg().getNextAddress()))
                .setExcludingNode(PcePathDb.excludeNodeConvert(getSlaveTeArg().getExcludedNodes()))
                .setExcludingPort(PcePathDb.excludePortConvert(getSlaveTeArg().getExcludedPorts()))
                .setAffinityStrategy(getSlaveTeArg().getAffinityStrategy()).build();
        return new ServiceHsbPathsDataBuilder().setHeadNodeId(headNodeId).setTailNodeId(tailNodeId)
                .setServiceName(serviceName).setTopologyId(getTopoId()).setTeArgCommonData(getTeArg().getArgComm())
                .setNextAddress(PcePathDb.nextAddressConvert(getTeArg().getNextAddress()))
                .setExcludingPort(PcePathDb.excludePortConvert(getTeArg().getExcludedPorts()))
                .setExcludingNode(PcePathDb.excludeNodeConvert(getTeArg().getExcludedNodes()))
                .setServiceName(getServiceName()).setServiceMasterPaths(
                        new ServiceMasterPathsBuilder().setPathLink(PcePathDb.pathLinkToLinkConvert(masterPath))
                                .setLspMetric(masterMetric).setLspDelay(masterDelay)
                                .setSrlgs(transSrlgAttr(masterSrlgAttr)).build())

                .setServiceSlavePaths(
                        new ServiceSlavePathsBuilder().setPathLink(PcePathDb.pathLinkToLinkConvert(slavePath))
                                .setLspMetric(slaveMetric).setLspDelay(slaveDelay)
                                .setSrlgs(transSrlgAttr(slaveSrlgAttr)).build())
                .setComputeSlaveLspWithBandwidth(teArg.isComputeLspWithBandWidth())
                .setCalculateStrategyContainer(getCalculateStrategy())
                .setBiDirectContainer(biDirect == null ? null : biDirect.getBiDirectContainer())
                .setAffinityStrategy(teArg.getAffinityStrategy()).setSlaveTeArgument(slaveTeArgument)
                .setRecalcWithoutDelay(recalcWithoutDelay).build();
    }

    public TeArgumentBean getTeArg() {
        return this.teArg;
    }

    public ListenableFuture<PceResult> calcPathWithFailRollBackAsync(boolean isFailRollback) {
        return calcPathAsync(isFailRollback, this.teArg, this.teArgSlave, this.bwSharedGroups, null);
    }

    @Override
    public boolean isSlaveIndependent() {
        return false;
    }

    @Override
    public PriorityAvoidLinks generatePriorityAvoidLinks(TeArgumentBean teArgBean) {
        return new PriorityAvoidLinks();
    }

    @Override
    protected void notifyReverseTunnel(boolean isNeedNotify) {
         /*do nothing*/
    }

    @Override
    public void decreaseBandwidth(long newBandwidth, BwSharedGroupContainer bwContainer) {
        /*
         * do nothing
         */
    }

    @Override
    public void notifyPathChange() {
        /*
         * do nothing
         */
    }

    @Override
    public BiDirect getBiDirect() {
        return biDirect;
    }

    @Override
    public TeArgumentBean getTeArgumentBean() {
        return teArg;
    }

    public synchronized PceResult updateWithoutRollback(UpdateServiceInput input) {
        final TeArgumentBean oldTeArg = this.teArg;
        final TeArgumentBeanLsp oldTeArgSlave = Optional.ofNullable(this.teArgSlave).orElseGet(TeArgumentBeanLsp::new);

        UpdateServiceInput newInput = input;
        if (ZeroBandwidthUtils.isZeroBandwidthPath(input.isZeroBandwidthPath())) {
            newInput = ZeroBandwidthUtils.generateZeroBandwidthUpdateServiceInput(input);
        }
        final PceResult pceResult = super.updateWithoutRollback(newInput, newInput.getSlaveTeArgument(),
                                                                newInput.getBwSharedGroupContainer());
        this.teArg = oldTeArg;
        this.teArg.update(input, topoId);
        this.teArg.setForceCalcPathWithBandwidth(false);
        this.teArgSlave = oldTeArgSlave;
        this.teArgSlave.update(input.getSlaveTeArgument(), topoId);
        this.bwSharedGroups = input.getBwSharedGroupContainer();
        setChangeToZeroBandWidth(ZeroBandwidthUtils.isZeroBandwidthPath(input.isZeroBandwidthPath()));
        return pceResult;
    }

    public synchronized PceResult updateWithRollback(UpdateServiceInput input) {
        final TeArgumentBean oldTeArg = this.teArg;

        UpdateServiceInput newInput = input;
        if (ZeroBandwidthUtils.isZeroBandwidthPath(input.isZeroBandwidthPath())) {
            newInput = ZeroBandwidthUtils.generateZeroBandwidthUpdateServiceInput(input);
        }
        final PceResult pceResult =
                super.update(newInput, newInput.getSlaveTeArgument(), newInput.getBwSharedGroupContainer(), null);

        if (!pceResult.isCalcFail()) {
            final HsbNewTeArgBuilder hsbNewTeArgBuilder = new HsbNewTeArgBuilder(oldTeArg, topoId);
            teArg = hsbNewTeArgBuilder.buildNewTeArg(input, input, null);
            teArgSlave = hsbNewTeArgBuilder.buildNewSlaveTeArg(input.getSlaveTeArgument(), null);
            this.bwSharedGroups = input.getBwSharedGroupContainer();
            setChangeToZeroBandWidth(ZeroBandwidthUtils.isZeroBandwidthPath(input.isZeroBandwidthPath()));
        }
        return pceResult;
    }
}
