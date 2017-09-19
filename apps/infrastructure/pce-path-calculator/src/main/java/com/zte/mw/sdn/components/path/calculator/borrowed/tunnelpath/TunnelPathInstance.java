/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelpathinstancedata.TunnelPathsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelpathinstancedata.TunnelPathsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.BiDirectArgument;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.DirectRole;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TunnelPathUpdate;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TunnelPathUpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathWithoutRollbackInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.adjust.tunnel.bandwidth.input.PathAdjustRequest;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bi.direct.argument.BiDirectContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bi.direct.argument.BiDirectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bi.direct.argument.bi.direct.container.bidirect.type.BidirectBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bi.direct.argument.bi.direct.container.bidirect.type.Bidirectional;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.CalculateStrategyContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.multiple.paths.param.grouping.MultiplePathsParam;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.tunnel.path.update.TunnelPathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.tunnel.path.update.TunnelPathSegmentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandwidthAllocException;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroupMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.PathCompator;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.PathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelUnifyRecordKey;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelsRecordPerPort;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.NotificationProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathDb;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.topology.TunnelsRecordPerTopology;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.DataBrokerDelegate;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.MplsLinkTools;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.segmentrouting.PathSegment;
import com.zte.ngip.ipsdn.pce.path.api.srlg.AovidLinks;
import com.zte.ngip.ipsdn.pce.path.api.srlg.SrlgAttribute;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;
import com.zte.ngip.ipsdn.pce.path.core.transformer.MetricTransformer;

@SuppressWarnings("unchecked")
public class TunnelPathInstance extends AbstractTunnelPathInstance {
    /**
     * TunnelPathInstance.
     *
     * @param dbData dbData
     */
    public TunnelPathInstance(TunnelPathsData dbData) {
        super(dbData.getHeadNodeId(), dbData.getTunnelId());
        this.headNodeId = dbData.getHeadNodeId();
        this.tailNodeId = dbData.getTailNodeId();
        this.topoId = dbData.getTopologyId();
        this.tunnelUnifyKey =
                getTunnelUnifyKey(dbData.getHeadNodeId(), dbData.getTunnelId(), dbData.getBiDirectContainer(), false);
        this.teArg = setTeArgInfo(dbData);
        this.path = PcePathDb.getInstance().pathLinks2Links(topoId, false, dbData.getPathLink());
        this.segments = PceUtil.transformFromSegment(dbData.getSegment(), false, topoId);
        setLspAttributes(dbData.getLspMetric(), dbData.getLspDelay(),
                         Optional.ofNullable(dbData.getSrlgs()).map(ArrayList::new).map(SrlgAttribute::new)
                                 .orElse(new SrlgAttribute()));
        this.biDirect = dbData.getBiDirectContainer() == null ? null : new TunnelBiDirect(dbData);
        setBwSharedGroup(dbData.getBwSharedGroupContainer());
        this.calculateStrategy = dbData.getCalculateStrategyContainer();
        this.recalcWithoutDelay = dbData.isRecalcWithoutDelay();
        this.isChangeToZeroBandWidth = dbData.isZeroBandWidth();
        this.srArgument = dbData.getSrArgument();
    }

    /**
     * TunnelPathInstance.
     *
     * @param input input
     */
    public TunnelPathInstance(CreateTunnelPathInput input) {
        super(input.getHeadNodeId(), input.getTunnelId().intValue());
        generatorTunnelPathParameter(input.getHeadNodeId().getValue(), input.getTailNodeId().getValue(),
                                     input.getTopologyId());
        this.teArg = new TeArgumentBean(input, this.topoId);
        setBwSharedGroup(input.getBwSharedGroupContainer());
        setCalculateStrategy(input.getCalculateStrategyContainer());
        setRecalcWithoutDelay(input.isRecalcWithoutDelay());

        this.tunnelUnifyKey =
                getTunnelUnifyKey(headNodeId, input.getTunnelId().intValue(), input.getBiDirectContainer(),
                                  ComUtility.isSimulateTunnel(input.isSimulateTunnel()));
        setBiDirect(input.getBiDirectContainer() == null ? null : new TunnelBiDirect(input, input.getTunnelId()));
        setBiDirectRole(DirectRole.Positive);
        TunnelsRecordPerTopology.getInstance().add(topoId, tunnelUnifyKey);

        this.multiplePathsParam = input.getMultiplePathsParam();
        this.srArgument = input.getSrArgument();
    }

    /**
     * TunnelPathInstance.
     *
     * @param tunnel     tunnel
     * @param isSimulate isSimulate
     */
    public TunnelPathInstance(TunnelPathInstance tunnel, boolean isSimulate) {
        super(NodeId.getDefaultInstance(tunnel.getHeadNodeId().getValue()), tunnel.getTunnelId());
        generatorTunnelPathParameter(tunnel.getHeadNodeId().getValue(), tunnel.getTailNodeId().getValue(),
                                     tunnel.topoId);
        this.tunnelUnifyKey = new TunnelUnifyKey(this.headNodeId, tunnel.getTunnelId(), null,
                                                 tunnel.getTunnelUnifyKey().isBiDirectional(), isSimulate);
        this.path = tunnel.getPath();
        this.segments = tunnel.getSegments();
        this.lspAttributes.setLspMetric(tunnel.getLspMetric());
        this.teArg = new TeArgumentBean(tunnel.teArg);
        this.bwSharedGroups = tunnel.bwSharedGroups;
        this.calculateStrategy = tunnel.calculateStrategy;
        this.recalcWithoutDelay = tunnel.recalcWithoutDelay;
        if (tunnel.getBiDirect() != null) {
            this.biDirect = new TunnelBiDirect(tunnel.biDirect);
            if (tunnel.getBiDirect().isReverse()) {
                this.biDirect.setPositiveInstanceReverseId(tailNodeId, getTunnelId());
            }
        }
        this.multiplePathsParam = tunnel.multiplePathsParam;
        this.srArgument = tunnel.srArgument;
    }

    /**
     * TunnelPathInstance.
     *
     * @param positiveTunnel positiveTunnel
     */
    public TunnelPathInstance(TunnelPathInstance positiveTunnel) {
        super(positiveTunnel.getTailNodeId(), (int) positiveTunnel.getBiDirect().getReverseId());
        this.headNodeId = positiveTunnel.getTailNodeId();
        this.tailNodeId = positiveTunnel.getHeadNode();
        this.topoId = positiveTunnel.getTopoId();
        this.teArg = new TeArgumentBean();
        this.tunnelUnifyKey = getTunnelUnifyKey(positiveTunnel.tailNodeId, getTunnelId(), null,
                                                positiveTunnel.isSimulate());
        this.bwSharedGroups = positiveTunnel.bwSharedGroups;
        this.calculateStrategy = positiveTunnel.calculateStrategy;
        this.recalcWithoutDelay = positiveTunnel.recalcWithoutDelay;
        BiDirectContainer biDirectContainer = new BiDirectContainerBuilder()
                .setBidirectType(new BidirectBindingBuilder()
                                         .setDirectRole(DirectRole.Reverse)
                                         .setReverseId((long) positiveTunnel.getTunnelId())
                                         .build())
                .build();

        setBiDirect(new TunnelBiDirect(
                new CreateTunnelPathInputBuilder().setBiDirectContainer(biDirectContainer).build()));
        this.biDirect.setPositiveInstanceReverseId(tailNodeId, getTunnelId());
        this.multiplePathsParam = positiveTunnel.multiplePathsParam;
        this.srArgument = positiveTunnel.srArgument;
    }

    private static final Logger LOG = LoggerFactory.getLogger(TunnelPathInstance.class);
    private MsCheck msFlag;
    private MultiplePathsParam multiplePathsParam;
    private List<PathSegment> segments = Collections.emptyList();

    private static TunnelUnifyKey getTunnelUnifyKey(
            NodeId nodeId, int tunnelId, BiDirectContainer biDirect,
            boolean isSimulate) {
        return new TunnelUnifyKey(nodeId, tunnelId, null, isBiDirectional(biDirect), isSimulate);
    }

    private static TeArgumentBean setTeArgInfo(TunnelPathsData dbData) {
        TeArgumentBean teAttr = new TeArgumentBean(dbData.getTeArgCommonData());
        teAttr.setExcludedNodes(dbData.getExcludingNode());
        teAttr.setExcludedPorts(dbData.getExcludingPort());
        teAttr.setNextAddress(dbData.getNextAddress());
        teAttr.setTryToAvoidLink(dbData.getTryToAvoidLink());
        teAttr.setContrainedAddress(dbData.getContrainedAddress(), dbData.getTopologyId());
        teAttr.setAffinityStrategy(dbData.getAffinityStrategy());
        return teAttr;
    }

    private static boolean isBiDirectional(BiDirectContainer biDirect) {
        return Optional.ofNullable(biDirect).map(BiDirectContainer::getBidirectType)
                .map(bidirectType -> bidirectType instanceof Bidirectional).orElse(false);
    }

    private void generatorTunnelPathParameter(String headId, String tailId, TopologyId topoId) {
        this.headNodeId = NodeId.getDefaultInstance(headId);
        this.tailNodeId = NodeId.getDefaultInstance(tailId);
        this.topoId =
                Optional.ofNullable(topoId).orElse(TopologyId.getDefaultInstance(ComUtility.DEFAULT_TOPO_ID_STRING));
    }

    private void setBiDirectRole(DirectRole role) {
        if (biDirect != null) {
            biDirect.setDirectRole(role);
        }
    }

    public NodeId getHeadNodeId() {
        return this.headNodeId;
    }

    public int getTunnelId() {
        return getId();
    }

    public NodeId getTailNodeId() {
        return this.tailNodeId;
    }

    public List<Link> getPath() {
        return this.path;
    }

    public TunnelPathInstance setPath(List<Link> path) {
        this.path = path;
        return this;
    }

    public List<PathSegment> getSegments() {
        return segments;
    }

    public void printSummaryInfo() {
        ComUtility.debugInfoLog("tunnelId:" + getTunnelId());
        ComUtility.debugInfoLog("headNodeId:" + headNodeId + ", tailNodeId:" + tailNodeId);
        ComUtility.debugInfoLog("topoId:" + topoId + ", bandwidth:" + teArg.getBandWidth());
        ComUtility.debugInfoLog("Path:");
        ComUtility.debugInfoLog(ComUtility.pathToString(path));
    }

    public void printDetailInfo() {
        ComUtility.debugInfoLog("headNodeId:" + headNodeId);
        ComUtility.debugInfoLog("tailNodeId:" + tailNodeId);
        ComUtility.debugInfoLog("tunnelId:" + getTunnelId());
        ComUtility.debugInfoLog("topoId:" + topoId);
        Optional.ofNullable(teArg).ifPresent(arg -> ComUtility.debugInfoLog(arg.toString()));

        ComUtility.debugInfoLog("Path:");
        ComUtility.debugInfoLog(ComUtility.pathToString(path));
        ComUtility.debugInfoLog("lspMetric:" + lspAttributes.getLspMetric());
    }

    public boolean isMatched(NodeId headNodeId, int tunnelId) {
        return this.headNodeId.equals(headNodeId) && (getTunnelId() == tunnelId);
    }

    protected void setBandwidth(long bandwidth) {
        this.teArg.updateBandWidth(bandwidth);
    }

    public PceResult calcPath(boolean failRollback, TeArgumentBean newArg) {
        try {
            return calcPathAsync(failRollback, newArg, null, null, bwSharedGroups, null).get();
        } catch (InterruptedException | ExecutionException error) {
            LOG.error("tunnelKey-" + tunnelUnifyKey.toString());
            LOG.error("calc tunnel face exception", error);
        }
        return new PceResult();
    }

    public PceResult calcPath(boolean failRollback) {
        return calcPath(failRollback, teArg, null, null, bwSharedGroups, null);
    }

    public PceResult calcPath(
            boolean failRollback, TeArgumentBean newArg, List<Link> masterPath,
            List<Link> overlapPath, BwSharedGroupContainer newBwSharedGroups,
            BwSharedGroupContainer deletedBwSharedGroups) {
        PceResult result = new PceResult();
        if (!newArg.isValid()) {
            result.setCalcFail(true);
            return result;
        }

        PathProvider<MetricTransformer> pathProvider = new PathProvider(headNodeId, tunnelUnifyKey, tailNodeId, topoId,
                                                                        generateCalculateStrategy(),
                                                                        generateTransformerFactory());

        pathProvider.setTeArgWithBuildNew(newArg);
        pathProvider.setOldPath(path);
        pathProvider.setFailRollback(failRollback);
        pathProvider.setOverlapPath(overlapPath);
        pathProvider.setBwSharedGroups(newBwSharedGroups, deletedBwSharedGroups);
        pathProvider.setBiDirect(biDirect);
        pathProvider.setRecalc(recalcWithoutDelay);
        if (!CollectionUtils.isNullOrEmpty(masterPath)) {
            pathProvider.addAvoidPath(Lists.newArrayList(new AovidLinks(masterPath)));
        }
        pathProvider.calcPath(result);
        if (failRollback && (result.isCalcFail())) {
            return result;
        }
        setPathInfo(pathProvider);
        return result;
    }

    public PceResult calcPath(List<Link> avoidPath, List<Link> overlapPath) {
        return calcPath(false, teArg, avoidPath, overlapPath);
    }

    public PceResult calcPath(
            boolean failRollback, TeArgumentBean newArg, List<Link> masterPath,
            List<Link> overlapPath) {
        PceResult result = new PceResult();
        if (!newArg.isValid()) {
            result.setCalcFail(true);
            return result;
        }

        PathProvider<MetricTransformer> pathProvider = new PathProvider(headNodeId, tunnelUnifyKey, tailNodeId, topoId,
                                                                        generateCalculateStrategy(),
                                                                        generateTransformerFactory());

        pathProvider.setTeArgWithBuildNew(newArg);
        pathProvider.setOldPath(path);
        pathProvider.setFailRollback(failRollback);
        pathProvider.setOverlapPath(overlapPath);
        pathProvider.setBiDirect(biDirect);
        pathProvider.setRecalc(recalcWithoutDelay);
        if (!CollectionUtils.isNullOrEmpty(masterPath)) {
            pathProvider.addAvoidPath(Lists.newArrayList(new AovidLinks(masterPath)));
        }
        try {
            result = pathProvider.calcPathAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("calcPath faced error {}", e);
        }
        if (failRollback && result.isCalcFail()) {
            return result;
        }
        setPathInfo(pathProvider);
        return result;
    }

    public PceResult calcPath(List<Link> overlapPath) {
        return calcPath(false, teArg, null, overlapPath);
    }

    public ListenableFuture<PceResult> calcPathAsync() {
        return calcPathAsync(false, teArg, null, null, bwSharedGroups, null);
    }

    public ListenableFuture<PceResult> calcPathAsync(boolean failRollback) {
        return calcPathAsync(failRollback, teArg, null, null, bwSharedGroups, null);
    }

    @Override
    protected PathProvider<MetricTransformer> buildPathProvider(
            boolean failRollback, TeArgumentBean newArg,
            List<Link> masterPath, List<Link> overlapPath, BwSharedGroupContainer newBwSharedGroups,
            BwSharedGroupContainer deletedBwSharedGroups) {

        PathProvider<MetricTransformer> pathProvider = buildBasePathProvider(failRollback, newArg, overlapPath,
                                                                             newBwSharedGroups, deletedBwSharedGroups);

        if (!CollectionUtils.isNullOrEmpty(masterPath)) {
            pathProvider.addAvoidPath(Lists.newArrayList(new AovidLinks(masterPath)));
        }
        pathProvider.setMultiplePathsParam(this.multiplePathsParam);
        return pathProvider;
    }

    @Override
    protected void setPathInfo(PathProvider<MetricTransformer> pathProvider) {
        super.setPathInfo(pathProvider);
        segments = PcePathProvider.getInstance().calcSegments(isSrTunnel(), path, topoId, tunnelUnifyKey);
    }

    private boolean isCalcPathSucc() {
        return null != path && !path.isEmpty() && !isChangeToZeroBandWidth();
    }

    public PceResult updateBandwidth(long newBw) {
        return updateBandwidth(newBw, null, null);
    }

    //fail rollback
    public PceResult updateBandwidth(long newBw, List<Link> masterPath, List<Link> overlapPath) {
        if (PceUtil.isTunnelBandwidthShrinkToZero(teArg, newBw)) {
            teArg.setForceCalcPathWithBandwidth(true);
        }
        Long oldBw = teArg.getBandWidth();
        teArg.updateBandWidth(newBw);

        PceResult pceResult = calcPath(true, teArg, masterPath, overlapPath);
        if (pceResult.isCalcFail()) {
            teArg.updateBandWidth(oldBw);
        }
        teArg.setForceCalcPathWithBandwidth(false);
        return pceResult;
    }

    public synchronized PceResult adjustBandWidth(PathAdjustRequest input) {
        PceResult pceResult = new PceResult();
        Logs.info(LOG, "updateBandWidth {}", this);
        if (path == null || path.isEmpty()) {
            return pceResult;
        }
        long oldBw = teArg.getBandWidth();
        long newBw = getNewAdjustBw(input, oldBw);
        if (oldBw == newBw) {
            Logs.info(LOG, "updateBandWidth bandWidth is equal");
            return pceResult;
        }
        if (!isPathHasEnoughBw(teArg, newBw, path, isBiDirect())) {
            Logs.info(LOG, "updateBandWidth has no enough bw ");
            pceResult.setCalcFail(true);
            pceResult.setCalcFailType(CalcFailType.NoEnoughBandwidth);
            return pceResult;
        }
        Logs.info(LOG, "updateBandWidth bandWidth {}", newBw);

        if (PceUtil.isTunnelBandwidthShrinkToZero(teArg, newBw)) {
            teArg.setForceCalcPathWithBandwidth(true);
        }
        teArg.updateBandWidth(newBw);
        if (newBw < oldBw) {
            Logs.info(LOG, "updateBandWidth decreasePathBw");
            pceResult = BandWidthMng.getInstance()
                    .decreasePathBw(path, newBw, teArg.getHoldPriority(), tunnelUnifyKey, isBiDirect());
        } else {
            Logs.info(LOG, "updateBandWidth increasePathBw");
            pceResult = BandWidthMng.getInstance()
                    .increasePathBw(path, newBw, teArg.getHoldPriority(), teArg.getHoldPriority(), tunnelUnifyKey,
                                    isBiDirect());
        }
        setChangeToZeroBandWidth(false);
        writeDb();
        return pceResult;
    }

    protected boolean isBiDirect() {
        return biDirect != null;
    }

    public PceResult updateTopoId(TopologyId topologyId) {
        this.topoId = topologyId;

        return calcPath();
    }

    public synchronized PceResult updateWithoutRollback(UpdateTunnelPathWithoutRollbackInput input) {
        return updateWithoutRollback(input, null, null);
    }

    public PceResult updateWithoutRollback(
            UpdateTunnelPathWithoutRollbackInput input, List<Link> masterPath,
            List<Link> overlapPath) {
        setTopoId(input.getTopologyId());

        boolean forceCalcPathWithBandwidth = PceUtil.isTunnelBandwidthShrinkToZero(teArg, input.getBandwidth());

        if (isMaster()) {
            teArg = getNewTeArg(input, input);
        } else {
            teArg = getNewTeArg(input, input.getSlaveTeArgument());
        }

        if (forceCalcPathWithBandwidth) {
            teArg.setForceCalcPathWithBandwidth(true);
        }

        BwSharedGroupContainer deletedBwGroups = BwSharedGroupMng.getDeletedGroups(
                input.getBwSharedGroupContainer(),
                bwSharedGroups);
        final PceResult pceResult =
                calcPath(false, teArg, masterPath, overlapPath, input.getBwSharedGroupContainer(), deletedBwGroups);
        teArg.setForceCalcPathWithBandwidth(false);
        setChangeToZeroBandWidth(false);
        notifyReverseTunnel(false);
        return pceResult;
    }

    public boolean isMaster() {
        return !isHsb() || this.msFlag == MsCheck.MASTER;
    }

    public boolean isHsb() {
        return this.msFlag != null;
    }

    public synchronized PceResult update(UpdateTunnelPathInput input) {
        return update(input, null, null, input.getBwSharedGroupContainer());
    }

    public synchronized PceResult update(
            UpdateTunnelPathInput input, List<Link> masterPath, List<Link> overlapPath,
            BwSharedGroupContainer newBwSharedGroups) {
        setTopoId(input.getTopologyId());

        boolean forceCalcPathWithBandwidth = PceUtil.isTunnelBandwidthShrinkToZero(teArg, input.getBandwidth());

        TeArgumentBean newArg;
        if (isMaster()) {
            newArg = getNewTeArg(input, input, input.getMaintenanceTeArgument());
        } else {
            newArg = getNewTeArg(input, input.getSlaveTeArgument());
        }

        if (forceCalcPathWithBandwidth) {
            newArg.setForceCalcPathWithBandwidth(true);
        }

        BwSharedGroupContainer deletedBwGroups = BwSharedGroupMng.getDeletedGroups(newBwSharedGroups, bwSharedGroups);
        PceResult pceResult = new PceResult();
        try {
            pceResult = calcPathAsync(true, newArg, masterPath, overlapPath, newBwSharedGroups, deletedBwGroups).get();
        } catch (InterruptedException | ExecutionException e) {
            Logs.info(LOG, "tunnel path update{}", e);
        }
        if (!pceResult.isCalcFail() && !PceUtil.isMaintenance(input.getMaintenanceTeArgument())) {
            teArg = newArg;
            teArg.setForceCalcPathWithBandwidth(false);
            setBwSharedGroup(newBwSharedGroups);
            setChangeToZeroBandWidth(false);
        }

        notifyReverseTunnel(false);
        return pceResult;
    }

    @Override
    public void destroy() {
        super.destroy();
        TunnelsRecordPerTopology.getInstance().remove(topoId, tunnelUnifyKey);
        TunnelsRecordPerPort.getInstance().update(new TunnelUnifyRecordKey(tunnelUnifyKey), path, null);

        if (isBidirectReverse()) {
            return;
        }

        PceResult result = new PceResult();
        if (teArg != null && teArg.getBandWidth() != 0) {
            BandWidthMng.getInstance().free(path, teArg.getHoldPriority(), tunnelUnifyKey, result, isBiDirect());
        }

        if (bwSharedGroups != null) {
            BwSharedGroupMng.getInstance().delTunnelAndFreeBw(path, tunnelUnifyKey, bwSharedGroups,
                                                              isBiDirect(), result);
        }

        if (result.isNeedRefreshUnestablishTunnels()) {
            PcePathProvider.getInstance()
                    .refreshUnestablishTunnels(tunnelUnifyKey.isSimulate(), topoId, tunnelUnifyKey, getHoldPriority());
        }

        TopoServiceAdapter.getInstance().getPceTopoProvider().removeTunnelInstance(tunnelUnifyKey);
    }

    @Override
    public PceResult reCalcPath(TunnelUnifyKey path) {
        if (!path.equals(tunnelUnifyKey)) {
            return PceResult.nullPceResult;
        }
        PceResult result = calcPath();

        if (biDirect != null && biDirect.isBiDirectBinding()) {
            TunnelPathInstance reversePathInstance = PcePathProvider.getInstance()
                    .getTunnelInstance(tailNodeId, (int) biDirect.getReverseId(), path.isSimulate());
            PceResult reverseResult = reversePathInstance.calcPath();
            result.merge(reverseResult);
        }

        return result;
    }

    @Override
    public synchronized PceResult refreshPath(TunnelUnifyKey path) {
        if (!path.equals(tunnelUnifyKey)) {
            return PceResult.nullPceResult;
        }
        return refreshPath();
    }

    @Override
    protected void notifyReverseTunnel(boolean isNeedNotify) {
        if (BiDirect.isBiDirectPositive(biDirect)) {
            biDirect.notifyReverseChange(tailNodeId, getTunnelUnifyKey(), isNeedNotify);
        }
    }

    public PceResult calcPath() {
        try {
            return calcPathAsync(false, teArg, null, null, bwSharedGroups, null).get();
        } catch (InterruptedException | ExecutionException error) {
            LOG.error("tunnelKey-" + tunnelUnifyKey.toString());
            LOG.error("calcPathAsync face error ", error);
        }
        return new PceResult();
    }

    private boolean isBidirectReverse() {
        return BiDirect.isBiDirectReverse(biDirect);
    }

    public void destroy2Hotstandby() {
        TunnelsRecordPerTopology.getInstance().remove(topoId, tunnelUnifyKey);
        TunnelsRecordPerPort.getInstance().update(new TunnelUnifyRecordKey(tunnelUnifyKey), path, null);
    }

    @Override
    public NodeId getTailNode() {
        return getTailNodeId();
    }

    @Override
    public TopologyId getTopoId() {
        return topoId;
    }

    @Override
    public long getOccupyBw() {
        return teArg.getBandWidth();
    }

    @Override
    public byte getHoldPriority() {
        return teArg.getHoldPriority();
    }

    @Override
    public TunnelUnifyKey getTunnelUnifyKey() {
        return this.tunnelUnifyKey;
    }

    @Override
    public List<Link> getMasterPath() {
        return path;
    }

    @Override
    public List<Link> getSlavePath() {
        return new LinkedList<>();
    }

    @Override
    public void writeDb() {
        if (getTunnelState() == TUNNEL_INVALID) {
            Logs.info(LOG, "tunnel state is invalid can not write db {}", tunnelUnifyKey);
            return;
        }
        if (!getTunnelUnifyKey().isSimulate()) {
            DataBrokerDelegate.getInstance().put(
                    LogicalDatastoreType.CONFIGURATION,
                    PcePathDb.buildtunnelPathDbPath(getHeadNodeId(), getTunnelId()),
                    tunnelPathsInstanceCreate());
        }
    }

    @Override
    public void removeDb() {
        if (!getTunnelUnifyKey().isSimulate()) {
            DataBrokerDelegate.getInstance().delete(
                    LogicalDatastoreType.CONFIGURATION,
                    PcePathDb.buildtunnelPathDbPath(getHeadNodeId(), getTunnelId()));
        }
    }

    @Override
    public void writeMemory() {
        if (getTunnelState() == TUNNEL_INVALID) {
            Logs.info(LOG, "tunnel state is invalid can not write memory {}", tunnelUnifyKey);
            return;
        }
        PcePathProvider.getInstance().updateTunnel(this);
    }

    @Override
    public synchronized PceResult refreshPath() {
        if (isBidirectReverse()) {
            return PceResult.create();
        }
        LOG.info("refreshPath {}", getTunnelUnifyKey());
        final List<Link> oldPath = path;
        final List<PathSegment> oldSegments = segments;
        final boolean oldZeroBandWidthFlag = isChangeToZeroBandWidth();
        teArg.updateIpv4(topoId);
        final PceResult pceResult = calcRefreshPath(this::calcPath);
        logRefreshedPath("refreshed path", oldPath);
        writeDbAndNotifyPathChange(oldPath, oldZeroBandWidthFlag != isChangeToZeroBandWidth());
        notifySegmentsChange(oldSegments);
        return pceResult;
    }

    @Override
    public synchronized PceResult refreshUnestablishPath() {

        if (isCalcPathSucc()) {
            return PceResult.create();
        }
        LOG.info("refreshed unestablished or srlg path {}", getTunnelUnifyKey());
        teArg.updateIpv4(topoId);
        final List<Link> oldPath = path;
        final List<PathSegment> oldSegments = segments;
        final boolean oldZeroBandWidthFlag = isChangeToZeroBandWidth();
        final PceResult pceResult = calcUnestablishedPath(this::calcPath);
        logRefreshedPath("refreshed unestablished or srlg path", oldPath);
        writeDbAndNotifyPathChange(oldPath, oldZeroBandWidthFlag != isChangeToZeroBandWidth());
        notifySegmentsChange(oldSegments);
        return pceResult;
    }

    @Override
    public synchronized PceResult refreshUnestablishAndSrlgPath() {
        return refreshUnestablishPath();
    }

    @Override
    public void refreshSegments() {
        if (!isSrTunnel()) {
            return;
        }
        Logs.info(LOG, "refreshSegments tunnelId={} headNodeId={}", tunnelUnifyKey.getTunnelId(), headNodeId);
        final List<PathSegment> oldSegments = segments;
        segments = PcePathProvider.getInstance().calcSegments(isSrTunnel(), path, topoId, tunnelUnifyKey);
        writeDb();
        notifySegmentsChange(oldSegments);
    }

    @Override
    public void decreaseBandwidth(long newBandwidth, BwSharedGroupContainer bwContainer) {
        if (teArg == null || teArg.getBandWidth() == 0) {
            LOG.error("oldBandwidth is null!", tunnelUnifyKey.toString());
            return;
        }

        long oldBandwidth = teArg.getBandWidth();
        if (newBandwidth > oldBandwidth) {
            LOG.error("newBandwidth > oldBandwidth!", tunnelUnifyKey.toString());
            return;
        }

        if (newBandwidth == oldBandwidth) {
            LOG.error("newBandwidth = oldBandwidth!", tunnelUnifyKey.toString());
            return;
        }

        teArg.setBandWidth(newBandwidth);
        BwSharedGroupContainer newGroups;
        if (bwContainer == null || CollectionUtils.isNullOrEmpty(bwContainer.getBwSharedGroupMember())) {
            newGroups = null;
        } else {
            newGroups = bwContainer;
        }

        decreaseBw(newBandwidth, newGroups);

        setBwSharedGroup(newGroups);
        setChangeToZeroBandWidth(false);
        writeDb();
    }

    @Override
    public void notifyPathChange() {
        TunnelPathUpdate notification = new TunnelPathUpdateBuilder()
                .setHeadNodeId(headNodeId)
                .setTunnelId((long) getTunnelId())
                .setSimulateTunnel(isSimulate())
                .setZeroBandWidth(isChangeToZeroBandWidth())
                .setTunnelPath(new TunnelPathBuilder()
                                       .setPathLink(MplsLinkTools.getMplsLinkPath(path))
                                       .setLspMetric(lspAttributes.getLspMetric())
                                       .build())
                .build();
        LOG.info("notifyPathChange:" + getTunnelId() + "isSimulate:" + isSimulate() + "-" + path);
        NotificationProvider.getInstance().notify(notification);
    }

    @Override
    public void notifySegmentsChange() {
        TunnelPathUpdate notification = new TunnelPathUpdateBuilder()
                .setHeadNodeId(headNodeId)
                .setTunnelId((long) getTunnelId())
                .setTunnelPathSegments(new TunnelPathSegmentsBuilder()
                                               .setSegment(PceUtil.transformToSegments(segments)).build())
                .build();
        NotificationProvider.getInstance().notify(notification);
    }

    @Override
    public BiDirect getBiDirect() {
        return biDirect;
    }

    protected void setBiDirect(BiDirect biDirect) {
        this.biDirect = biDirect;
    }

    @Override
    public TeArgumentBean getTeArgumentBean() {
        return getTeArg();
    }

    @Override
    public boolean isSimulate() {
        return getTunnelUnifyKey() != null && getTunnelUnifyKey().isSimulate();
    }

    public void setTunnelUnifyKey(TunnelUnifyKey tunnelUnifyKey) {
        this.tunnelUnifyKey = tunnelUnifyKey;
    }

    private void setTopoId(TopologyId topoId) {
        if (topoId != null) {
            this.topoId = topoId;
        }
    }

    private void decreaseBw(long newBandwidth, BwSharedGroupContainer newGroups) {
        try {
            if (bwSharedGroups == null) {
                BandWidthMng.getInstance().decreasePathBw(path, newBandwidth, teArg.getHoldPriority(), tunnelUnifyKey);
            } else {
                BwSharedGroupMng.getInstance().decreaseGroupBw(newGroups, isSimulate());
            }
        } catch (BandwidthAllocException error) {
            LOG.error("", error);
        }
    }

    private TunnelPathsData tunnelPathsInstanceCreate() {
        return new TunnelPathsDataBuilder()
                .setTopologyId(getTopoId())
                .setTeArgCommonData(getTeArg().getArgComm())
                .setNextAddress(PcePathDb.nextAddressConvert(getTeArg().getNextAddress()))
                .setExcludingPort(PcePathDb.excludePortConvert(getTeArg().getExcludedPorts()))
                .setExcludingNode(PcePathDb.excludeNodeConvert(getTeArg().getExcludedNodes()))
                .setIsTg(getTunnelUnifyKey().isTg())
                .setPathLink(PcePathDb.pathLinkToLinkConvert(getLsp()))
                .setIsMaster(getTunnelUnifyKey().isMaster())
                .setHeadNodeId(getHeadNodeId())
                .setTailNodeId(getTailNodeId())
                .setTunnelId(getTunnelId())
                .setLspMetric(getLspMetric())
                .setLspDelay(getLspDelay())
                .setSrlgs(transSrlgAttr(lspAttributes.getSrlgAttr()))
                .setBwSharedGroupContainer(getBwSharedGroups())
                .setCalculateStrategyContainer(getCalculateStrategy())
                .setRecalcWithoutDelay(recalcWithoutDelay)
                .setBiDirectContainer(biDirect == null ? null : biDirect.getBiDirectContainer())
                .setAffinityStrategy(teArg.getAffinityStrategy())
                .setSrArgument(srArgument)
                .setSegment(PceUtil.transformToSegments(segments))
                .setZeroBandWidth(isChangeToZeroBandWidth)
                .build();
    }

    public TeArgumentBean getTeArg() {
        return this.teArg;
    }

    public CalculateStrategyContainer getCalculateStrategy() {
        return calculateStrategy;
    }

    private void notifySegmentsChange(List<PathSegment> oldSegments) {
        if (isSimulate() || !isSrTunnel()) {
            return;
        }
        final boolean isSegmentsChange = !Objects.equals(oldSegments, segments);
        if (isSegmentsChange && CommonTunnel.NORMAL_STATE == getTunnelState()) {
            Logs.debug(LOG, "NotifySegmentsChange:{} old={} new={}", getTunnelId(), oldSegments, segments);
            notifySegmentsChange();
        }
    }

    public void recoverTunnelPathBw() {
        List<Link> lspPath = getLsp();
        TeArgumentBean lspTeArg = getTeArg();
        if (isBidirectReverse()) {
            return;
        }

        if (!CollectionUtils.isNullOrEmpty(lspPath)) {
            if (bwSharedGroups != null) {
                BwSharedGroupMng.getInstance().recoverBw(lspPath, teArg.getPreemptPriority(), teArg.getHoldPriority(),
                                                         tunnelUnifyKey, isBiDirect(), bwSharedGroups);
            } else {
                BandWidthMng.getInstance()
                        .recoverPathBw(lspPath, lspTeArg.getHoldPriority(), lspTeArg.getBandWidth(), tunnelUnifyKey,
                                       isBiDirect());
            }
        }
    }

    private void calcLspAttributesViaPath() {
        if (CollectionUtils.isNullOrEmpty(path)) {
            return;
        }
        long lspMetric = 0;
        long lspDelay = 0;
        for (Link link : path) {
            lspMetric += TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkMetric(link);
            lspDelay += TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkDelay(link);
        }
        lspAttributes.setLspMetric(lspMetric);
        lspAttributes.setLspDelay(lspDelay);
    }

    private void writeDbAndNotifyPathChange(List<Link> oldPath, boolean isZeroBandWidthChange) {
        writeDb();
        if (getTunnelState() == CommonTunnel.ACTIVE_WAIT_STATE) {
            LOG.info("{} state is ACTIVE_WAIT_STATE,need set TOPO_CHANGE_STATE", getTunnelUnifyKey());
            comAndSetTunnelState(CommonTunnel.ACTIVE_WAIT_STATE, CommonTunnel.TOPO_CHANGE_STATE);
        }
        if ((!PathCompator.isPathEqual(oldPath, path) || isZeroBandWidthChange)
                && CommonTunnel.NORMAL_STATE == getTunnelState()) {
            notifyPathChange();
            notifyReverseTunnel(true);
        }
    }

    private void logRefreshedPath(String description, List<Link> oldPath) {
        Logs.info(LOG, "{} {}\n old path:{}\n<->\nnew path:\n{}\n", this.getTunnelUnifyKey(), description, oldPath,
                  path);
    }

    private enum MsCheck {
        MASTER, SLAVE;

        public static MsCheck getMsCheck(boolean msFlag) {
            MsCheck msCheck;

            if (msFlag) {
                msCheck = MASTER;
            } else {
                msCheck = SLAVE;
            }

            return msCheck;
        }
    }

    public class TunnelBiDirect extends BiDirect {
        TunnelBiDirect(BiDirectArgument biDirectArgument, Long tunnelId) {
            super(biDirectArgument, tunnelId, isSimulate());
        }

        TunnelBiDirect(BiDirectArgument biDirectArgument) {
            super(biDirectArgument, isSimulate());
        }

        TunnelBiDirect(BiDirect source) {
            super(source.getBiDirectContainer(), isSimulate());
        }

        @Override
        public List<Link> getPositivePathById(TunnelUnifyKey positiveTunnel) {

            TunnelPathInstance instance = PcePathProvider.getInstance()
                    .getTunnelPathInstance(positiveTunnel.getHeadNode(), positiveTunnel.getTunnelId(),
                                           positiveTunnel.isSimulate());
            if (instance == null) {
                Logs.error(LOG, "positive instance can't be found! headNodeId:{} tunnelId:{}",
                           positiveTunnel.getHeadNode(), positiveTunnel.getTunnelId());
                return new LinkedList<>();
            }
            return instance.getMasterPath();
        }

        @Override
        public BiDirect getBiDirectById(NodeId tail, long reverseId) {
            TunnelPathInstance instance =
                    PcePathProvider.getInstance().getTunnelInstance(tail, (int) reverseId, isSimulate());
            if (instance == null) {
                Logs.error(LOG, "positive instance can't be found! headNodeId:{} tunnelId:{}", tail, reverseId);
                return null;
            }
            return instance.getBiDirect();
        }

        @Override
        public void notifyReverseChangeByKey(NodeId tail, long reverseId, boolean isSimulate, boolean isNeedNotify) {
            TunnelPathInstance reverseInst =
                    PcePathProvider.getInstance().getTunnelPathInstance(tail, (int) reverseId, isSimulate);
            if (reverseInst == null) {
                Logs.error(LOG, "reverse instance can't be found! headNodeId:{} tunnelId:{} isSimulate:{}",
                           tail, reverseId, isSimulate);
                return;
            }
            reverseInst.setBwSharedGroup(bwSharedGroups);
            reverseInst.setBandwidth(teArg.getBandWidth());
            reverseInst.setChangeToZeroBandWidth(isChangeToZeroBandWidth());
            reverseInst.calcPath();
            reverseInst.calcLspAttributesViaPath();
            reverseInst.writeDb();
            /*reverse tunnel need to notify tunnel by 2016.9.1*/
            if (isNeedNotify) {
                reverseInst.notifyPathChange();
            }
        }
    }
}
