/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelhsbpath;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tnnlhsbdata.HsbMasterPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tnnlhsbdata.HsbSlavePathsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tnnlhsbdata.MasterPathSegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tnnlhsbdata.SlavePathSegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tnnlhsbdata.SlaveTeArgument;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tnnlhsbdata.SlaveTeArgumentBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelhsbpathinstancedata.TunnelHsbsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelhsbpathinstancedata.TunnelHsbsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelhsbpathinstancedata.TunnelHsbsDataKey;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.BiDirectArgument;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateSlaveTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelHsbPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelHsbPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.DirectRole;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TunnelPathUpdate;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TunnelPathUpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathWithoutRollbackInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.adjust.tunnel.bandwidth.input.PathAdjustRequest;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bi.direct.argument.BiDirectContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bi.direct.argument.BiDirectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bi.direct.argument.bi.direct.container.bidirect.type.BidirectBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bi.direct.argument.bi.direct.container.bidirect.type.Bidirectional;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.tunnel.path.update.TunnelPathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.tunnel.path.update.TunnelPathSegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.tunnel.path.update.TunnelProtectedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.tunnel.path.update.TunnelProtectedPathSegmentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelUnifyRecordKey;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelsRecordPerPort;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.NotificationProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathDb;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.srlg.PriorityAvoidLinks;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.HsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.DataBrokerDelegate;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.MplsLinkTools;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.srlg.AovidLinks;
import com.zte.ngip.ipsdn.pce.path.api.srlg.SrlgAttribute;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBeanLsp;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;

public class TunnelHsbPathInstance extends HsbPathInstance {
    /**
     * constructor.
     *
     * @param input input
     */
    public TunnelHsbPathInstance(CreateTunnelHsbPathInput input) {
        super(input.getHeadNodeId(), input.getTailNodeId(), ComUtility.getTopoId(input.getTopologyId()),
              getSimuMasterTunnelUnifyKey(input.getHeadNodeId(), input.getTunnelId().intValue(),
                                          input.getBwSharedGroupContainer(),
                                          isBiDirectional(input.getBiDirectContainer()),
                                          ComUtility.isSimulateTunnel(input.isSimulateTunnel())),
              getSimuSlaveTunnelUnifyKey(input.getHeadNodeId(), input.getTunnelId().intValue(),
                                         input.getBwSharedGroupContainer(),
                                         isBiDirectional(input.getBiDirectContainer()),
                                         ComUtility.isSimulateTunnel(input.isSimulateTunnel())),
              new TeArgumentBean(input, ComUtility.getTopoId(input.getTopologyId())));
        this.teArgSlave = Optional.ofNullable(input.getSlaveTeArgument())
                .map(slaveTeArgument -> new TeArgumentBeanLsp(slaveTeArgument, input.getTopologyId()))
                .orElseGet(TeArgumentBeanLsp::new);
        this.setBwSharedGroup(input.getBwSharedGroupContainer());
        this.setCalculateStrategy(input.getCalculateStrategyContainer());

        this.tunnelId = input.getTunnelId();
        if (this.teArg != null && input.isComputeSlaveLspWithBandwidth() != null) {
            this.teArg.setComputeLspWithBandWidth(input.isComputeSlaveLspWithBandwidth());
        }
        this.setBiDirect((input.getBiDirectContainer() == null) ? null : (new TunnelHsbBiDirect(input, tunnelId)));
        this.setBiDirectRole(DirectRole.Positive);
        this.setRecalcWithoutDelay(input.isRecalcWithoutDelay());
        this.isSrlgEnabled = input.isSrlgEnabled() == null ? true : input.isSrlgEnabled();
        this.multiplePathsParam = input.getMultiplePathsParam();
        this.srArgument = input.getSrArgument();
    }

    /**
     * constructor.
     *
     * @param positiveTunnelInstance positiveTunnelInstance
     */
    public TunnelHsbPathInstance(TunnelHsbPathInstance positiveTunnelInstance) {
        super(positiveTunnelInstance.getTailNodeId(), positiveTunnelInstance.getHeadNodeId(),
              positiveTunnelInstance.getTopoId(), getSimuMasterTunnelUnifyKey(positiveTunnelInstance.getTailNodeId(),
                                                                              (int) positiveTunnelInstance.getBiDirect().getReverseId(),
                                                                              null, false,
                                                                              positiveTunnelInstance.isSimulate()),
              getSimuSlaveTunnelUnifyKey(positiveTunnelInstance.getTailNodeId(),
                                         (int) positiveTunnelInstance.getBiDirect().getReverseId(), null, false,
                                         positiveTunnelInstance.isSimulate()), positiveTunnelInstance.teArg);
        this.teArgSlave = new TeArgumentBeanLsp();
        this.setBwSharedGroup(null);
        this.setCalculateStrategy(positiveTunnelInstance.getCalculateStrategy());

        this.tunnelId = positiveTunnelInstance.getBiDirect().getReverseId();
        BiDirectContainer biDirectContainer = new BiDirectContainerBuilder()
                .setBidirectType(new BidirectBindingBuilder()
                                         .setDirectRole(DirectRole.Reverse)
                                         .setReverseId(positiveTunnelInstance.getTunnelId())
                                         .build())
                .build();

        this.setBiDirect(new TunnelHsbBiDirect(
                new CreateTunnelHsbPathInputBuilder().setBiDirectContainer(biDirectContainer).build()));
        if (this.teArg != null && positiveTunnelInstance.getTeArgumentBean() != null) {
            this.teArg
                    .setComputeLspWithBandWidth(positiveTunnelInstance.getTeArgumentBean().isComputeLspWithBandWidth());
        }
        this.biDirect.setPositiveInstanceReverseId(tailNodeId, tunnelId);
        this.isSrlgEnabled = positiveTunnelInstance.isSrlgEnabled;
        this.multiplePathsParam = positiveTunnelInstance.multiplePathsParam;
        this.srArgument = positiveTunnelInstance.srArgument;
    }

    /**
     * constructor.
     *
     * @param tunnelHsbPathInstance tunnelHsbPathInstance
     * @param isSimulate            isSimulate
     */
    public TunnelHsbPathInstance(TunnelHsbPathInstance tunnelHsbPathInstance, boolean isSimulate) {
        super(tunnelHsbPathInstance.getHeadNodeId(), tunnelHsbPathInstance.getTailNodeId(),
              tunnelHsbPathInstance.getTopoId(), getSimuMasterTunnelUnifyKey(
                        tunnelHsbPathInstance.getHeadNodeId(),
                        tunnelHsbPathInstance.getTunnelId().intValue(),
                        tunnelHsbPathInstance.bwSharedGroups,
                        tunnelHsbPathInstance.getTunnelUnifyKey().isBiDirectional(),
                        isSimulate),
              getSimuSlaveTunnelUnifyKey(tunnelHsbPathInstance.getHeadNodeId(),
                                         tunnelHsbPathInstance.getTunnelId().intValue(),
                                         tunnelHsbPathInstance.bwSharedGroups,
                                         tunnelHsbPathInstance.getTunnelUnifyKey().isBiDirectional(), isSimulate),
              null);
        this.teArgSlave = new TeArgumentBeanLsp();
        this.setBwSharedGroup(tunnelHsbPathInstance.bwSharedGroups);
        this.setCalculateStrategy(tunnelHsbPathInstance.getCalculateStrategy());

        this.tunnelId = tunnelHsbPathInstance.getTunnelId();
        this.setBiDirect(tunnelHsbPathInstance.getBiDirect() == null
                                 ? null : new TunnelHsbBiDirect(tunnelHsbPathInstance.getBiDirect()));
        this.masterPath = tunnelHsbPathInstance.getMasterLspLinkedList();
        this.slavePath = tunnelHsbPathInstance.getSlaveLspLinkedList();
        this.masterDelay = tunnelHsbPathInstance.getMasterDelay();
        this.slaveDelay = tunnelHsbPathInstance.getSlaveDelay();
        this.masterMetric = tunnelHsbPathInstance.getMasterMetric();
        this.slaveMetric = tunnelHsbPathInstance.getSlaveMetric();
        this.masterPathSegments = tunnelHsbPathInstance.getMasterPathSegments();
        this.slavePathSegments = tunnelHsbPathInstance.getSlavePathSegments();
        this.teArg = new TeArgumentBean(tunnelHsbPathInstance.getTeArgumentBean());
        this.overlapPath = ComUtility.clonePathLink(tunnelHsbPathInstance.getOverlapPath());
        this.isSrlgEnabled = tunnelHsbPathInstance.isSrlgEnabled;
        this.multiplePathsParam = tunnelHsbPathInstance.multiplePathsParam;
        this.srArgument = tunnelHsbPathInstance.srArgument;
    }

    /**
     * constructor.
     *
     * @param masterTunnelInstance masterTunnelInstance
     * @param input                input
     */
    public TunnelHsbPathInstance(TunnelPathInstance masterTunnelInstance, CreateSlaveTunnelPathInput input) {
        super(input.getHeadNodeId(), input.getTailNodeId(), ComUtility.getTopoId(input.getTopologyId()),
              getSimuMasterTunnelUnifyKey(input.getHeadNodeId(), input.getTunnelId().intValue(), null,
                                          isBiDirectional(masterTunnelInstance.getBiDirect() != null
                                                                  ? masterTunnelInstance.getBiDirect().getBiDirectContainer() :
                                                                  null),
                                          ComUtility.isSimulateTunnel(input.isSimulateTunnel())),
              getSimuSlaveTunnelUnifyKey(input.getHeadNodeId(), input.getTunnelId().intValue(), null, isBiDirectional(
                      masterTunnelInstance.getBiDirect() != null
                              ? masterTunnelInstance.getBiDirect().getBiDirectContainer() :
                              null), ComUtility.isSimulateTunnel(input.isSimulateTunnel())),
              masterTunnelInstance.getTeArg());
        this.teArgSlave = new TeArgumentBeanLsp(input, ComUtility.getTopoId(input.getTopologyId()));
        this.setBwSharedGroup(masterTunnelInstance.getBwSharedGroups());
        this.setCalculateStrategy(masterTunnelInstance.getCalculateStrategy());

        this.tunnelId = input.getTunnelId();
        this.setBiDirect(masterTunnelInstance.getBiDirect());
        if (masterTunnelInstance.getMasterPath() != null) {
            this.masterPath = new LinkedList<>();
            this.masterPath.addAll(masterTunnelInstance.getMasterPath());
            this.masterPathSegments = masterTunnelInstance.getSegments();
            LinkedList<Link> reverseMasterPath = new LinkedList<>();
            if (biDirect != null) {
                reverseMasterPath = (LinkedList<Link>) masterTunnelInstance.getBiDirect()
                        .getReversePathByPositive(masterPath, topoId);
            }
            BandWidthMng.getInstance().updateUnifyKey(this.masterPath, reverseMasterPath,
                                                      masterTunnelInstance.getTunnelUnifyKey(),
                                                      masterTunnelUnifyKey, teArg.getHoldPriority());
        }
        this.masterMetric = masterTunnelInstance.getLspMetric();

        masterTunnelInstance.destroy2Hotstandby();
        TunnelUnifyKey masterKey = new TunnelUnifyRecordKey(masterTunnelUnifyKey);
        TunnelsRecordPerPort.getInstance().update(new TunnelUnifyRecordKey(masterKey), null, this.masterPath);

        teArgSlave.resetTryAvoidLink();
        this.multiplePathsParam = input.getMultiplePathsParam();
        this.srArgument = masterTunnelInstance.getSrArgument();
    }

    /**
     * constructor.
     *
     * @param dbData dbData
     */
    public TunnelHsbPathInstance(TunnelHsbsData dbData) {
        super();
        this.topoId = dbData.getTopologyId();
        this.headNodeId = dbData.getHeadNodeId();
        this.tailNodeId = dbData.getTailNodeId();
        this.tunnelId = dbData.getTunnelId();
        this.masterPath =
                PcePathDb.getInstance().pathLinks2Links(topoId, false, dbData.getHsbMasterPaths().getPathLink());
        this.slavePath =
                PcePathDb.getInstance().pathLinks2Links(topoId, false, dbData.getHsbSlavePaths().getPathLink());
        this.masterMetric = dbData.getHsbMasterPaths().getLspMetric();
        this.slaveMetric = dbData.getHsbSlavePaths().getLspMetric();
        this.masterDelay = dbData.getHsbMasterPaths().getLspDelay();
        this.slaveDelay = dbData.getHsbSlavePaths().getLspDelay();
        this.masterPathSegments =
                PceUtil.transformFromSegment(dbData.getMasterPathSegments().getSegment(), false, topoId);
        this.slavePathSegments =
                PceUtil.transformFromSegment(dbData.getSlavePathSegments().getSegment(), false, topoId);
        this.masterSrlgAttr = dbData.getHsbMasterPaths().getSrlgs() == null
                ? new SrlgAttribute() :
                new SrlgAttribute(new ArrayList<>(dbData.getHsbMasterPaths().getSrlgs()));
        this.slaveSrlgAttr = dbData.getHsbSlavePaths().getSrlgs() == null
                ? new SrlgAttribute() :
                new SrlgAttribute(new ArrayList<>(dbData.getHsbSlavePaths().getSrlgs()));
        this.teArg = setTeArgInfo(dbData);
        this.teArgSlave = setSlaveTeArgInfo(dbData);
        this.masterTunnelUnifyKey = getMasterTunnelUnifyKey(dbData.getHeadNodeId(), dbData.getTunnelId().intValue(),
                                                            dbData.getBwSharedGroupContainer(),
                                                            dbData.getBiDirectContainer());
        this.slaveTunnelUnifyKey = getSlaveTunnelUnifyKey(dbData.getHeadNodeId(), dbData.getTunnelId().intValue(),
                                                          dbData.getBwSharedGroupContainer(),
                                                          dbData.getBiDirectContainer());
        this.biDirect = dbData.getBiDirectContainer() == null ? null : (new TunnelHsbBiDirect(dbData));
        this.bwSharedGroups = dbData.getBwSharedGroupContainer();
        this.calculateStrategy = dbData.getCalculateStrategyContainer();
        this.recalcWithoutDelay = dbData.isRecalcWithoutDelay();
        this.isSrlgEnabled = dbData.isSrlgEnabled();
        this.isChangeToZeroBandWidth = dbData.isZeroBandWidth();
        this.srArgument = dbData.getSrArgument();
        setBwSharedGroup(dbData.getBwSharedGroupContainer());
    }

    private static final Logger LOG = LoggerFactory.getLogger(TunnelHsbPathInstance.class);
    private Long tunnelId;

    private static TunnelUnifyKey getSimuMasterTunnelUnifyKey(
            NodeId nodeId, int tunnelId,
            BwSharedGroupContainer bwSharedGroup, boolean isBiDirect, boolean isSimulate) {
        return new TunnelUnifyKey(nodeId, tunnelId, false, true, true, bwSharedGroup != null, isBiDirect)
                .setSimulateFlag(isSimulate);
    }

    private static boolean isBiDirectional(BiDirectContainer biDirect) {
        return biDirect != null && biDirect.getBidirectType() != null
                && (biDirect.getBidirectType() instanceof Bidirectional);
    }

    private static TunnelUnifyKey getSimuSlaveTunnelUnifyKey(
            NodeId nodeId, int tunnelId,
            BwSharedGroupContainer bwSharedGroup, boolean isBiDirect, boolean isSimulate) {
        return new TunnelUnifyKey(nodeId, tunnelId, false, false, true, bwSharedGroup != null, isBiDirect)
                .setSimulateFlag(isSimulate);
    }

    private static TeArgumentBean setTeArgInfo(TunnelHsbsData dbData) {
        TeArgumentBean teAttr = new TeArgumentBean(dbData.getTeArgCommonData());
        teAttr.setExcludedNodes(dbData.getExcludingNode());
        teAttr.setExcludedPorts(dbData.getExcludingPort());
        teAttr.setNextAddress(dbData.getNextAddress());
        teAttr.setTryToAvoidLink(dbData.getTryToAvoidLink());
        teAttr.setContrainedAddress(dbData.getContrainedAddress(), dbData.getTopologyId());
        teAttr.setComputeLspWithBandWidth(dbData.isComputeSlaveLspWithBandwidth());
        teAttr.setAffinityStrategy(dbData.getAffinityStrategy());
        return teAttr;
    }

    private static TeArgumentBeanLsp setSlaveTeArgInfo(TunnelHsbsData dbData) {
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
            NodeId nodeId, int tunnelId,
            BwSharedGroupContainer bwSharedGroup,
            BiDirectContainer biDirect) {
        return new TunnelUnifyKey(nodeId, tunnelId, false, true, true, bwSharedGroup != null,
                                  isBiDirectional(biDirect));
    }

    private static TunnelUnifyKey getSlaveTunnelUnifyKey(
            NodeId nodeId, int tunnelId,
            BwSharedGroupContainer bwSharedGroup,
            BiDirectContainer biDirect) {
        return new TunnelUnifyKey(nodeId, tunnelId, false, false, true, bwSharedGroup != null,
                                  isBiDirectional(biDirect));
    }

    private void setBiDirectRole(DirectRole role) {
        if (biDirect != null) {
            biDirect.setDirectRole(role);
        }
    }

    public Long getTunnelId() {
        return tunnelId;
    }

    /**
     * calcPathWithFailRollBack.
     *
     * @param isFailRollback isFailRollback
     * @return PceResult
     */
    public PceResult calcPathWithFailRollBack(boolean isFailRollback) {
        try {
            return calcPathAsync(isFailRollback, this.teArg, this.teArgSlave, this.bwSharedGroups, null).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("calcPathWithFailRollBack witch exception {}", e);
        }
        return new PceResult();
    }

    public ListenableFuture<PceResult> calcPathWithFailRollBackAsync(boolean isFailRollback) {
        return calcPathAsync(isFailRollback, this.teArg, this.teArgSlave, this.bwSharedGroups, null);
    }

    public PceResult calcPathWithNoFailrollBackAndShareGroup() {
        return calcPath(false, this.teArg, this.teArgSlave, null, null);
    }

    public ListenableFuture<PceResult> calcPathWithNoFailrollBackAndShareGroupAsync() {
        return calcPathAsync(false, this.teArg, this.teArgSlave, null, null);
    }

    public boolean isMatched(NodeId headNodeId, int tunnelId) {
        return (this.headNodeId.equals(headNodeId)) && (this.tunnelId == tunnelId);
    }

    @Override
    public void printSummaryInfo() {
        ComUtility.debugInfoLog("TunnelId:" + tunnelId);
        super.printSummaryInfo();
    }

    @Override
    public void printDetailInfo() {
        ComUtility.debugInfoLog("TunnelId:" + tunnelId);
        super.printDetailInfo();
    }

    @Override
    public boolean isSlaveIndependent() {
        return false;
    }

    @Override
    public PriorityAvoidLinks generatePriorityAvoidLinks(TeArgumentBean teArgBean) {
        List<Link> avoidMasterPath = ComUtility.getTryToAvoidLinkForHsb(masterPath);
        List<Link> manualAvoidLinks = teArgBean.getTryToAvoidLinkInLinks();

        PriorityAvoidLinks priorityAvoidLinks = new PriorityAvoidLinks();
        if (isSrlgEnabled) {
            List<Link> srlgAvoidLinks = PceUtil.getSrlgAvoidLinks(
                    masterPath,
                    TopoServiceAdapter.getInstance().getPceTopoProvider()
                            .getTopoGraph(ComUtility.getSimulateFlag(getTunnelUnifyKey()), topoId));
            priorityAvoidLinks.addAvoidLinks(new AovidLinks(srlgAvoidLinks));
        }
        priorityAvoidLinks.addAvoidLinks(new AovidLinks(manualAvoidLinks));
        priorityAvoidLinks.addAvoidLinks(new AovidLinks(avoidMasterPath));
        return priorityAvoidLinks;
    }

    @Override
    public String toString() {
        String str = "";

        str += "tunnelId:" + tunnelId;
        str += super.toString();
        return str;
    }

    @Override
    public void notifyPathChange() {
        TunnelPathUpdate notification = new TunnelPathUpdateBuilder()
                .setHeadNodeId(headNodeId)
                .setTunnelId(tunnelId)
                .setSimulateTunnel(isSimulate())
                .setZeroBandWidth(isChangeToZeroBandWidth())
                .setTunnelPath(new TunnelPathBuilder()
                                       .setPathLink(MplsLinkTools.getMplsLinkPath(masterPath))
                                       .setLspMetric(masterMetric)
                                       .build())
                .setTunnelProtectedPath(new TunnelProtectedPathBuilder()
                                                .setPathLink(MplsLinkTools.getMplsLinkPath(slavePath))
                                                .setLspMetric(slaveMetric)
                                                .build()).build();
        NotificationProvider.getInstance().notify(notification);
        Logs.debug(LOG, "[notifyPathChange]:{}", toString());
    }

    /**
     * update tunnel hsb path.
     *
     * @param input new argument
     * @return {@link PceResult}
     */
    public synchronized PceResult update(UpdateTunnelHsbPathInput input) {
        return super.update(input, input.getSlaveTeArgument(), input.getBwSharedGroupContainer(),
                            input.getMaintenanceTeArgument());
    }

    /**
     * update common tunnel.
     *
     * @param input new argument
     * @return {@link PceResult}
     */
    public synchronized PceResult update(UpdateTunnelPathInput input) {
        return super.update(input, input.getSlaveTeArgument(), input.getBwSharedGroupContainer(),
                            input.getMaintenanceTeArgument());
    }

    public synchronized PceResult updateWithoutRollback(UpdateTunnelPathWithoutRollbackInput input) {
        return super.updateWithoutRollback(input, input.getSlaveTeArgument(), input.getBwSharedGroupContainer());
    }

    public synchronized PceResult adjustBandWidth(PathAdjustRequest input) {
        PceResult pceResult = new PceResult();
        Logs.info(LOG, "updateBandWidth {}", this);
        long oldBw = teArg.getBandWidth();
        long newBw = getNewAdjustBw(input, oldBw);
        if (oldBw == newBw) {
            Logs.info(LOG, "updateBandWidth bandWidth is equal");
            return pceResult;
        }
        PceResult masterResult = updateMasterBandWidth(oldBw, newBw);
        if (CalcFailType.NoEnoughBandwidth.equals(masterResult.getCalcFailType())) {
            Logs.info(LOG, "updateMasterBandWidth bandWidth fail");
            return masterResult;
        }
        masterResult.merge(updateSlaveBandWidth(oldBw, newBw));
        setChangeToZeroBandWidth(false);
        writeDb();
        return masterResult;
    }

    private PceResult updateMasterBandWidth(long oldBw, long newBw) {
        PceResult pceResult = new PceResult();
        if (masterPath == null || masterPath.isEmpty()) {
            Logs.info(LOG, "updateMasterBandWidth master is null");
            return pceResult;
        }
        if (!isPathHasEnoughBw(teArg, newBw, masterPath, isBiDirect())) {
            Logs.info(LOG, "updateMasterBandWidth master has no enough bw ");
            pceResult.setCalcFail(true);
            pceResult.setCalcFailType(CalcFailType.NoEnoughBandwidth);
            return pceResult;
        }
        if (PceUtil.isTunnelBandwidthShrinkToZero(teArg, newBw)) {
            teArg.setForceCalcPathWithBandwidth(true);
        }
        teArg.updateBandWidth(newBw);
        if (newBw < oldBw) {
            Logs.info(LOG, "updateBandWidth master decreasePathBw");
            pceResult = BandWidthMng.getInstance()
                    .decreasePathBw(masterPath, newBw, teArg.getHoldPriority(), getMasterTunnelUnifyKey(),
                                    isBiDirect());
        } else {
            Logs.info(LOG, "updateBandWidth master increasePathBw");
            pceResult = BandWidthMng.getInstance()
                    .increasePathBw(masterPath, newBw, teArg.getHoldPriority(), teArg.getHoldPriority(),
                                    getMasterTunnelUnifyKey(), isBiDirect());
        }
        return pceResult;
    }

    private PceResult updateSlaveBandWidth(long oldBw, long newBw) {
        PceResult pceResult = new PceResult();
        if (!teArg.isComputeLspWithBandWidth()) {
            Logs.info(LOG, "updateMasterBandWidth slave no need do");
            return pceResult;
        }
        if (slavePath == null || slavePath.isEmpty()) {
            Logs.info(LOG, "updateMasterBandWidth slave is null");
            return pceResult;
        }
        if (!isPathHasEnoughBw(teArg, newBw, slavePath, isBiDirect())) {
            Logs.info(LOG, "updateMasterBandWidth slave has no enough bw ");
            pceResult.setCalcFail(true);
            pceResult.setCalcFailType(CalcFailType.NoEnoughBandwidth);
            return pceResult;
        }
        if (PceUtil.isTunnelBandwidthShrinkToZero(teArg, newBw)) {
            teArg.setForceCalcPathWithBandwidth(true);
        }
        teArg.updateBandWidth(newBw);
        if (newBw < oldBw) {
            Logs.info(LOG, "updateBandWidth slave decreasePathBw");
            pceResult = BandWidthMng.getInstance()
                    .decreasePathBw(slavePath, newBw, teArg.getHoldPriority(), getSlaveTunnelUnifyKey(), isBiDirect());
        } else {
            Logs.info(LOG, "updateBandWidth slave increasePathBw");
            pceResult = BandWidthMng.getInstance()
                    .increasePathBw(slavePath, newBw, teArg.getHoldPriority(), teArg.getHoldPriority(),
                                    getSlaveTunnelUnifyKey(), isBiDirect());
        }
        return pceResult;
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
                    PcePathDb.buildtunnelHsbDbPath(getHeadNodeId(), getTunnelId()),
                    tunnelHsbsDataCreate());
        }
    }

    @Override
    public void removeDb() {
        if (!getTunnelUnifyKey().isSimulate()) {
            DataBrokerDelegate.getInstance().delete(
                    LogicalDatastoreType.CONFIGURATION,
                    PcePathDb.buildtunnelHsbDbPath(getHeadNodeId(), getTunnelId()));
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
        TunnelPathUpdate notification = new TunnelPathUpdateBuilder()
                .setHeadNodeId(headNodeId)
                .setTunnelId(tunnelId)
                .setTunnelPathSegments(new TunnelPathSegmentsBuilder()
                                               .setSegment(PceUtil.transformToSegments(masterPathSegments)).build())
                .setTunnelProtectedPathSegments(new TunnelProtectedPathSegmentsBuilder()
                                                        .setSegment(
                                                                PceUtil.transformToSegments(slavePathSegments)).build())
                .build();

        NotificationProvider.getInstance().notify(notification);
    }

    @Override
    public boolean isSimulate() {
        return getTunnelUnifyKey() != null && getTunnelUnifyKey().isSimulate();
    }

    private TunnelHsbsData tunnelHsbsDataCreate() {
        SlaveTeArgument slaveTeArgument = new SlaveTeArgumentBuilder()
                .setTryToAvoidLink(PcePathDb.tryToAvoidLinkConvert(getSlaveTeArg().getTryToAvoidLink()))
                .setNextAddress(PcePathDb.nextAddressConvert(getSlaveTeArg().getNextAddress()))
                .setExcludingNode(PcePathDb.excludeNodeConvert(getSlaveTeArg().getExcludedNodes()))
                .setExcludingPort(PcePathDb.excludePortConvert(getSlaveTeArg().getExcludedPorts()))
                .setAffinityStrategy(getSlaveTeArg().getAffinityStrategy())
                .build();

        return new TunnelHsbsDataBuilder()
                .setKey(new TunnelHsbsDataKey(getHeadNodeId(), getTunnelId()))
                .setTunnelId(getTunnelId())
                .setZeroBandWidth(isChangeToZeroBandWidth())
                .setComputeSlaveLspWithBandwidth(this.teArg == null || this.teArg.isComputeLspWithBandWidth())
                .setTryToAvoidLink(PcePathDb.tryToAvoidLinkConvert(getTeArgumentBean().getTryToAvoidLink()))
                .setTeArgCommonData(getTeArgumentBean().getArgComm())
                .setHeadNodeId(getHeadNodeId())
                .setTailNodeId(getTailNodeId())
                .setHsbMasterPaths(new HsbMasterPathsBuilder()
                                           .setPathLink(PcePathDb.pathLinkToLinkConvert(getMasterLsp()))
                                           .setLspMetric(masterMetric).setLspDelay(masterDelay).setSrlgs(
                                transSrlgAttr(masterSrlgAttr))
                                           .build()).setHsbSlavePaths(
                        new HsbSlavePathsBuilder().setPathLink(PcePathDb.pathLinkToLinkConvert(getSlaveLsp()))
                                .setLspMetric(slaveMetric).setLspDelay(slaveDelay)
                                .setSrlgs(transSrlgAttr(slaveSrlgAttr)).build())
                .setExcludingNode(PcePathDb.excludeNodeConvert(getTeArgumentBean().getExcludedNodes()))
                .setExcludingPort(PcePathDb.excludePortConvert(getTeArgumentBean().getExcludedPorts()))
                .setNextAddress(PcePathDb.nextAddressConvert(getTeArgumentBean().getNextAddress()))
                .setTopologyId(getTopoId())
                .setSlaveTeArgument(slaveTeArgument)
                .setBiDirectContainer(biDirect == null ? null : biDirect.getBiDirectContainer())
                .setBwSharedGroupContainer(bwSharedGroups)
                .setCalculateStrategyContainer(calculateStrategy)
                .setRecalcWithoutDelay(recalcWithoutDelay)
                .setSrlgEnabled(isSrlgEnabled)
                .setAffinityStrategy(teArg.getAffinityStrategy())
                .setSrArgument(srArgument)
                .setMasterPathSegments(new MasterPathSegmentsBuilder()
                                               .setSegment(PceUtil.transformToSegments(masterPathSegments)).build())
                .setSlavePathSegments(new SlavePathSegmentsBuilder()
                                              .setSegment(PceUtil.transformToSegments(slavePathSegments)).build())
                .build();
    }

    public class TunnelHsbBiDirect extends BiDirect {
        public TunnelHsbBiDirect(BiDirectArgument arg, Long tunnelId) {
            super(arg, tunnelId, isSimulate());
        }

        public TunnelHsbBiDirect(BiDirectArgument arg) {
            super(arg, isSimulate());
        }

        public TunnelHsbBiDirect(BiDirect source) {
            super(source.getBiDirectContainer(), isSimulate());
        }

        @Override
        public List<Link> getPositivePathById(TunnelUnifyKey positiveTunnel) {
            TunnelHsbPathInstance instance = PcePathProvider.getInstance()
                    .getTunnelHsbInstanceByKey(positiveTunnel.getHeadNode(), positiveTunnel.getTunnelId(),
                                               positiveTunnel.isSimulate());
            if (instance == null) {
                Logs.error(LOG, "positive instance can't be found! headNodeId:{} tunnelId:{}",
                           positiveTunnel.getHeadNode(), positiveTunnel.getTunnelId());
                return new LinkedList<>();
            }

            if (positiveTunnel.isMaster()) {
                return instance.getMasterLspLinkedList();
            } else {
                return instance.getSlaveLspLinkedList();
            }
        }

        @Override
        public BiDirect getBiDirectById(NodeId tail, long reveseId) {
            TunnelHsbPathInstance instance = PcePathProvider.getInstance()
                    .getTunnelHsbInstanceByKey(tail, (int) reveseId, getTunnelUnifyKey().isSimulate());
            if (instance == null) {
                Logs.error(LOG, "positive instance can't be found! headNodeId:{} tunnelId:{}", tail, reveseId);
                return null;
            }
            return instance.getBiDirect();
        }

        @Override
        public void notifyReverseChangeByKey(NodeId tail, long reverseId, boolean isSimulate, boolean isNeedNotify) {
            TunnelHsbPathInstance reverseInst =
                    PcePathProvider.getInstance().getTunnelHsbInstanceByKey(tail, (int) reverseId, isSimulate);
            if (reverseInst == null) {
                Logs.error(LOG, "reverse instance can't be found! headNodeId:{} tunnelId:{} isSimulate:{}",
                           tail, reverseId, isSimulate);
                return;
            }
            if (reverseInst.getTunnelUnifyKey().isSimulate()) {
                return;
            }
            reverseInst.setBwSharedGroup(bwSharedGroups);
            reverseInst.setBandwidth(teArg.getBandWidth());
            reverseInst.setChangeToZeroBandWidth(isChangeToZeroBandWidth());
            reverseInst.calcPath();
            reverseInst.writeDb();
            /*reverse tunnel need to notify tunnel by 2016.9.1*/
            if (isNeedNotify) {
                reverseInst.notifyPathChange();
            }
        }
    }
}
