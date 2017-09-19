/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TeArgument;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TeArgumentLsp;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.CalculateStrategyContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.multiple.paths.param.grouping.MultiplePathsParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroupMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.DiffServBw;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.PathCompator;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.PathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelUnifyRecordKey;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelsRecordPerPort;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.srlg.PriorityAvoidLinks;
import com.zte.mw.sdn.components.path.calculator.borrowed.topology.TunnelsRecordPerTopology;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelhsbpath.TunnelHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.CommonTunnel;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.segmentrouting.PathSegment;
import com.zte.ngip.ipsdn.pce.path.api.srlg.SrlgAttribute;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Conditions;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBeanLsp;
import com.zte.ngip.ipsdn.pce.path.core.strategy.ICalcStrategy;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformerFactory;
import com.zte.ngip.ipsdn.pce.path.core.transformer.MetricTransformer;

public abstract class HsbPathInstance extends CommonTunnel {
    public HsbPathInstance() {
    }

    public HsbPathInstance(NodeId headNodeId, String serviceName) {
        super(headNodeId, serviceName);
    }

    /**
     * Constructor.
     *
     * @param headNodeId           headNodeId
     * @param tailNodeId           tailNodeId
     * @param topoId               topoId
     * @param masterTunnelUnifyKey masterTunnelUnifyKey
     * @param slaveTunnelUnifyKey  slaveTunnelUnifyKey
     * @param teArg                teArg
     */
    public HsbPathInstance(
            NodeId headNodeId, NodeId tailNodeId,
            TopologyId topoId, TunnelUnifyKey masterTunnelUnifyKey,
            TunnelUnifyKey slaveTunnelUnifyKey, TeArgumentBean teArg) {
        super(masterTunnelUnifyKey.getHeadNode(), masterTunnelUnifyKey.getId());
        this.headNodeId = headNodeId;
        this.tailNodeId = tailNodeId;
        this.topoId = topoId;
        this.masterTunnelUnifyKey = masterTunnelUnifyKey;
        this.slaveTunnelUnifyKey = slaveTunnelUnifyKey;
        this.teArg = teArg;
        this.recordPerTopology(this.masterTunnelUnifyKey);
        this.recordPerTopology(this.slaveTunnelUnifyKey);
    }

    private static final Logger LOG = LoggerFactory.getLogger(HsbPathInstance.class);
    protected NodeId headNodeId;
    protected NodeId tailNodeId;
    protected List<Link> masterPath;
    protected List<Link> slavePath;
    protected List<PathSegment> masterPathSegments = Collections.emptyList();
    protected List<PathSegment> slavePathSegments = Collections.emptyList();
    protected long masterMetric;
    protected long slaveMetric;
    protected long masterDelay;
    protected long slaveDelay;
    protected SrlgAttribute masterSrlgAttr;
    protected SrlgAttribute slaveSrlgAttr;
    protected TopologyId topoId;
    protected TunnelUnifyKey masterTunnelUnifyKey;
    protected TunnelUnifyKey slaveTunnelUnifyKey;
    protected TeArgumentBean teArg;
    protected TeArgumentBeanLsp teArgSlave;
    protected BiDirect biDirect;
    protected List<Link> overlapPath = new LinkedList<>();
    protected boolean isSrlgEnabled = true;
    protected MultiplePathsParam multiplePathsParam;

    protected void recordPerTopology(TunnelUnifyKey tunnelKey) {
        TunnelUnifyKey tunnelKeyRecord = null;
        if (tunnelKey.getTgId() != 0) {
            tunnelKeyRecord = tunnelKey;
        }
        if (tunnelKey.getTunnelId() != 0) {
            tunnelKeyRecord = new TunnelUnifyKey(tunnelKey.getHeadNode(), tunnelKey.getTunnelId(), tunnelKey.isTg(),
                                                 tunnelKey.isMaster(), false);
        }
        TunnelsRecordPerTopology.getInstance().add(topoId, tunnelKeyRecord);
    }

    public void printSummaryInfo() {
        HsbPrintUtils.printSummaryInfo(this);
    }

    public SrlgAttribute getSrlgAttr(HsbLspType type) {
        if (type == HsbLspType.MASTER) {
            return masterSrlgAttr;
        } else {
            return slaveSrlgAttr;
        }
    }

    public void printDetailInfo() {
        HsbPrintUtils.printDetailInfo(this);
    }

    public PceResult calcPath(
            boolean isFailRollback, TeArgumentBean newTeArg, TeArgumentBeanLsp newSlaveTeArg,
            BwSharedGroupContainer bwSharedGroups, BwSharedGroupContainer deletedBwSharedGroups) {
        PceResult singlePceResult;

        singlePceResult = calcSinglePath(masterPath, true, newTeArg, isFailRollback,
                                         bwSharedGroups, deletedBwSharedGroups);
        if (singlePceResult.isCalcFail()) {
            if (needMaintenanceSlavePath(isFailRollback)) {
                maintenanceSlavePath();
            }
            return singlePceResult;
        }

        TeArgumentBean slaveTeArg = PceUtil.generatorSlaveTeArg(newTeArg, newSlaveTeArg);

        singlePceResult.merge(calcSinglePath(slavePath, false, slaveTeArg, false,
                                             bwSharedGroups, deletedBwSharedGroups));
        return singlePceResult;
    }

    @SuppressWarnings("unchecked")
    private PceResult calcSinglePath(
            List<Link> oldPath, boolean isMaster, TeArgumentBean teArgBean,
            boolean isFailRollback, BwSharedGroupContainer bwSharedGroups,
            BwSharedGroupContainer deletedBwSharedGroups) {
        PceResult pceResult;
        TunnelUnifyKey tunnelKey = isMaster ? masterTunnelUnifyKey : slaveTunnelUnifyKey;

        PathProvider<MetricTransformer> pathProvider =
                new PathProvider(headNodeId, tunnelKey, tailNodeId, topoId, generateCalculateStrategy(),
                                 generateTransformerFactory());
        pathProvider.setTeArgWithBuildNew(teArgBean);
        pathProvider.setOldPath(oldPath);
        pathProvider.setBiDirect(biDirect);
        pathProvider.setRecalc(recalcWithoutDelay);

        pathProvider.setBwSharedGroups(bwSharedGroups, deletedBwSharedGroups);
        if (!isSlaveIndependent()) {
            pathProvider.setOverlapPath(overlapPath);
        }
        if (!isMaster) {
            if (getTunnelUnifyKey().isServiceInstance()) {
                pceResult = calcServiceSlaveLsp(pathProvider);
            } else {
                pceResult = calcSlaveLsp(oldPath, teArgBean, pathProvider);
                setSlaveLspInfo(pathProvider);
            }
        } else {
            pceResult = calcMasterLsp(isFailRollback, pathProvider);
            if (HsbTunnelUtil.needRollback(isFailRollback, pceResult)) {
                return pceResult;
            }
            setMasterLspInfo(pathProvider);
        }
        calcOverlapPath();
        return pceResult;
    }

    private boolean needMaintenanceSlavePath(boolean isFailRollback) {
        return !isFailRollback && !isSlaveIndependent();
    }

    private void maintenanceSlavePath() {
        if (slavePath == null) {
            slavePath = new LinkedList<>();
            return;
        }
        if (slavePath.isEmpty()) {
            return;
        }

        if (!TopoServiceAdapter.getInstance().getPceTopoProvider()
                .pathIsOk(topoId, ComUtility.getSimulateFlag(getTunnelUnifyKey()), slavePath) || !HsbBwMngUtils
                .isPathHasEnoughBw(this, slavePath, slaveTunnelUnifyKey) || !HsbTunnelUtil
                .isPathDelayEligible(topoId, isSimulate(), teArg.getMaxDelay(), slavePath)) {
            LOG.info("maintenanceSlavePath: destroy!");
            clearAllPathInfo();
            slavePath = new LinkedList<>();
            slaveMetric = 0;
            slaveDelay = 0;
            slavePathSegments = Collections.emptyList();
        }
    }

    protected ICalcStrategy<NodeId, Link> generateCalculateStrategy() {
        return PceUtil.createCalcStrategy(calculateStrategy, biDirect != null, topoId);
    }

    protected ITransformerFactory generateTransformerFactory() {
        return PceUtil.createTransformerFactory(calculateStrategy);
    }

    public abstract boolean isSlaveIndependent();

    private PceResult calcServiceSlaveLsp(PathProvider<MetricTransformer> pathProvider) {
        PceResult pceResult = new PceResult();
        try {
            pceResult = calcSlaveServicePath(pathProvider).get();
        } catch (InterruptedException | ExecutionException e) {
            Logs.info(LOG, "calcSinglePath face error {}", e);
            pceResult.setCalcFail(true);
        }
        return pceResult;
    }

    private PceResult calcSlaveLsp(
            List<Link> oldPath, TeArgumentBean teArgBean,
            PathProvider<MetricTransformer> pathProvider) {
        pathProvider.clearTryToAvoidLinks();
        PriorityAvoidLinks priorityAvoidLinks = generatePriorityAvoidLinks(teArgBean);
        if (isDelayStrategy() && !CollectionUtils.isNullOrEmpty(masterPath)) {
            pathProvider.getTeArg().setMaxDelay(ComUtility.INVALID_DELAY);
            pathProvider.setTeArgWithBuildNew(pathProvider.getTeArg());
        }
        return priorityAvoidLinks.calcPathByAvoidPriority(pathProvider, slaveTunnelUnifyKey, oldPath);
    }

    private void setSlaveLspInfo(PathProvider<MetricTransformer> pathProvider) {
        slavePath = pathProvider.getPath();
        slaveMetric = pathProvider.getLspMetric();
        slaveDelay = pathProvider.getLspDelay();
        slaveSrlgAttr = pathProvider.getSrlgAttr();
        slavePathSegments =
                PcePathProvider.getInstance().calcSegments(isSrTunnel(), slavePath, topoId, slaveTunnelUnifyKey);
    }

    private PceResult calcMasterLsp(
            boolean isFailRollback,
            PathProvider<MetricTransformer> pathProvider) {
        PceResult result;
        pathProvider.setFailRollback(isFailRollback);
        try {
            result = pathProvider.calcPathAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.info("calcSinglePath exception {}", e);
            result = PceResult.nullPceResult;
            result.setCalcFail(true);
        }
        return result;
    }

    private void setMasterLspInfo(PathProvider<MetricTransformer> pathProvider) {
        masterPath = pathProvider.getPath();
        masterMetric = pathProvider.getLspMetric();
        masterDelay = pathProvider.getLspDelay();
        masterSrlgAttr = pathProvider.getSrlgAttr();
        masterPathSegments =
                PcePathProvider.getInstance().calcSegments(isSrTunnel(), masterPath, topoId, masterTunnelUnifyKey);
    }

    public void calcOverlapPath() {
        overlapPath.clear();
        overlapPath.addAll(HsbTunnelUtil.calcOverlapPath(masterPath, slavePath));
    }

    public void clearAllPathInfo() {
        TunnelsRecordPerTopology.getInstance().remove(topoId, masterTunnelUnifyKey);
        TunnelsRecordPerTopology.getInstance().remove(topoId, slaveTunnelUnifyKey);

        TunnelsRecordPerPort.getInstance().update(new TunnelUnifyRecordKey(masterTunnelUnifyKey), masterPath, null);
        TunnelsRecordPerPort.getInstance().update(new TunnelUnifyRecordKey(slaveTunnelUnifyKey), slavePath, null);

        if (BiDirect.isBiDirectReverse(biDirect)) {
            return;
        }

        HsbBwMngUtils.releaseHsbBandWidth(this);

        TopoServiceAdapter.getInstance().getPceTopoProvider().removeTunnelInstance(masterTunnelUnifyKey);
        TopoServiceAdapter.getInstance().getPceTopoProvider().removeTunnelInstance(slaveTunnelUnifyKey);
    }

    private ListenableFuture<PceResult> calcSlaveServicePath(PathProvider<MetricTransformer> pathProvider) {

        return Futures.transform(pathProvider.calcPathAsync(), (AsyncFunction<PceResult, PceResult>) calResult -> {
            setSlaveLspInfo(pathProvider);
            if (CollectionUtils.isNullOrEmpty(slavePath)) {
                clearAllPathInfo();
                calResult.setCalcFail(true);
                calResult.setCalcFailType(CalcFailType.NoPath);
            } else {
                calcOverlapPath();
            }
            return Futures.immediateFuture(calResult);
        });
    }

    public abstract PriorityAvoidLinks generatePriorityAvoidLinks(TeArgumentBean teArgBean);

    public PceResult calcSlavePath() {
        TeArgumentBean slaveTeArg = PceUtil.generatorSlaveTeArg(this.teArg, this.teArgSlave);
        return calcSinglePath(null, false, slaveTeArg, false, bwSharedGroups, null);
    }

    public PceResult calcUnestablishPath(boolean isNeedSrlg) {
        PceResult singlePceResult = new PceResult();

        if (isCalcUnsuccess(isNeedSrlg)) {
            LOG.info("calcUnestablishPath {}", getTunnelUnifyKey());
            try {
                singlePceResult = calcSinglePathAsync(masterPath, true, teArg, false, bwSharedGroups, null).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("calcUnestablishPath master faced error {}", e);
            }
            TeArgumentBean slaveTeArg = PceUtil.generatorSlaveTeArg(teArg, teArgSlave);
            singlePceResult.merge(calcSinglePath(slavePath, false, slaveTeArg, false, bwSharedGroups, null));
            logRefreshedPath("refresh unestablished  path");
        } else if (CollectionUtils.isNullOrEmpty(slavePath)) {
            TeArgumentBean slaveTeArg = PceUtil.generatorSlaveTeArg(teArg, teArgSlave);
            try {
                singlePceResult
                        .merge(calcSinglePathAsync(slavePath, false, slaveTeArg, false, bwSharedGroups, null).get());
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("calcUnestablishPath slave faced error {}", e);
            }
            logRefreshedPath("refresh unestablished path");
        }

        return singlePceResult;
    }

    private boolean isCalcUnsuccess(boolean isNeedSrlg) {
        if (CollectionUtils.isNullOrEmpty(masterPath)) {
            return true;
        }
        return !overlapPath.isEmpty() || isChangeToZeroBandWidth() || (isNeedSrlg && isSrlgOverlap());
    }

    public List<Link> getMasterLsp() {
        return masterPath;
    }

    public List<Link> getSlaveLsp() {
        return slavePath;
    }

    public List<Link> getMasterLspLinkedList() {
        return masterPath;
    }

    public List<Link> getSlaveLspLinkedList() {
        return slavePath;
    }

    public PceResult calcUnestablishPathEx(boolean isNeedSrlg) {
        PceResult singlePceResult;

        if (isChangeToZeroBandWidth()) {
            LOG.info("calcUnestablishPathEx {}", getTunnelUnifyKey());
            singlePceResult = PceResult.create();
            try {
                singlePceResult = calcPathAsync(true, teArg, teArgSlave, bwSharedGroups, null).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.info("calcUnestablishPathEx calcPathAsync face exception {}", e);
            }
            if (singlePceResult.isCalcFail()) {
                return singlePceResult;
            }
            setChangeToZeroBandWidth(false);
            logRefreshedPath("calcUnestablishPathEx refersh  path");
        } else {
            singlePceResult = calcUnestablishPath(isNeedSrlg);
        }
        return singlePceResult;
    }

    protected synchronized PceResult calcWithZeroBandWidth() {
        TeArgumentBean newArg = generatorZeroBandWidthTeArg(teArg);
        if (newArg == null) {
            return PceResult.create();
        }
        PceResult pceResult = PceResult.create();
        Logs.info(LOG, "Hsb {} change bandwidth to 0 to computer", getId());
        try {
            pceResult = calcPathAsync(false, newArg, teArgSlave, null, bwSharedGroups).get();
        } catch (InterruptedException | ExecutionException e) {
            Logs.info(LOG, "tunnel hsb path calcWithZeroBandWidth {}", e);
        }
        return pceResult;
    }

    private PceResult calcRefreshPath() {
        PceResult pceResult = calcPath();
        if (pceResult.isCalcFail() && CollectionUtils.isNullOrEmpty(masterPath) && CollectionUtils
                .isNullOrEmpty(slavePath)) {
            pceResult = calcWithZeroBandWidth();
            if (!pceResult.isCalcFail() && !CollectionUtils.isNullOrEmpty(masterPath)) {
                Logs.info(LOG, "TunnelHsb change to zero bandWith");
                setChangeToZeroBandWidth(true);
            } else {
                setChangeToZeroBandWidth(false);
            }
        } else {
            setChangeToZeroBandWidth(false);
        }
        return pceResult;
    }

    private PceResult refreshUnestablishedOrSrlgPath(boolean needSrlg) {
        if (BiDirect.isBiDirectReverse(biDirect)) {
            return PceResult.nullPceResult;
        }
        final List<Link> oldMasterPath = masterPath;
        final List<Link> oldSlavePath = slavePath;
        final boolean oldZeroBandWidthFlag = isChangeToZeroBandWidth();
        final List<PathSegment> oldMasterSegments = masterPathSegments;
        final List<PathSegment> oldSlaveSegments = slavePathSegments;

        final PceResult pceResult = calcUnestablishPathEx(needSrlg);
        writeDbAndNotifyPathChange(oldMasterPath, oldSlavePath, oldZeroBandWidthFlag != isChangeToZeroBandWidth());
        notifySegmentsChange(oldMasterSegments, oldSlaveSegments);
        return pceResult;
    }

    private void writeDbAndNotifyPathChange(
            List<Link> oldMasterPath, List<Link> oldSlavePath,
            boolean isZeroBandWidthChange) {
        writeDb();

        boolean isMasterChange = !PathCompator.isPathEqual(oldMasterPath, masterPath);
        boolean isSlaveChange = !PathCompator.isPathEqual(oldSlavePath, slavePath);

        if (getTunnelState() == CommonTunnel.ACTIVE_WAIT_STATE) {
            Logs.info(LOG, "{} state is ACTIVE_WAIT_STATE,need set TOPO_CHANGE_STATE", getTunnelUnifyKey());
            comAndSetTunnelState(CommonTunnel.ACTIVE_WAIT_STATE, CommonTunnel.TOPO_CHANGE_STATE);
        }

        if ((isMasterChange || isSlaveChange || isZeroBandWidthChange)
                && CommonTunnel.NORMAL_STATE == getTunnelState()) {
            notifyPathChange();
            notifyReverseTunnel(true);
        }
    }

    /**
     * update hsb path.
     *
     * @param newTeArg          newTeArg
     * @param argSlave          argSlave
     * @param newBwSharedGroups newBwSharedGroups
     * @param maintenanceTeArg  maintenanceTeArg
     * @return {@link PceResult}
     */
    public PceResult update(
            TeArgument newTeArg, TeArgumentLsp argSlave, BwSharedGroupContainer newBwSharedGroups,
            TeArgument maintenanceTeArg) {

        final HsbNewTeArgBuilder hsbNewTeArgBuilder = new HsbNewTeArgBuilder(teArg, topoId);
        TeArgumentBean newArgBean = hsbNewTeArgBuilder.buildNewTeArg(newTeArg, newTeArg, maintenanceTeArg);
        TeArgumentBeanLsp newArgSlave = hsbNewTeArgBuilder.buildNewSlaveTeArg(argSlave, maintenanceTeArg);
        if (PceUtil.isTunnelBandwidthShrinkToZero(teArg, newTeArg.getBandwidth())) {
            newArgBean.setForceCalcPathWithBandwidth(true);
        }

        BwSharedGroupContainer deletedBwGroups = BwSharedGroupMng.getDeletedGroups(newBwSharedGroups, bwSharedGroups);
        PceResult pceResult = new PceResult();
        try {
            pceResult = calcPathAsync(true, newArgBean, newArgSlave, newBwSharedGroups, deletedBwGroups).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.info("TunnelHsbInstance update {}", e);
        }
        newArgBean.setForceCalcPathWithBandwidth(false);
        if (pceResult.isCalcFail()) {
            return pceResult;
        }
        if (!PceUtil.isMaintenance(maintenanceTeArg)) {
            teArg = newArgBean;
            teArgSlave = newArgSlave;
            this.bwSharedGroups = newBwSharedGroups;
            setChangeToZeroBandWidth(false);
        }

        notifyReverseTunnel(false);
        return pceResult;
    }

    public PceResult updateWithoutRollback(
            TeArgument newTeArg, TeArgumentLsp argSlave,
            BwSharedGroupContainer newBwShareGroup) {

        if (PceUtil.isTunnelBandwidthShrinkToZero(teArg, newTeArg.getBandwidth())) {
            teArg.setForceCalcPathWithBandwidth(true);
        }

        this.teArg.update(newTeArg, topoId);
        if (this.teArgSlave == null) {
            this.teArgSlave = new TeArgumentBeanLsp();
        }
        this.teArgSlave.update(argSlave, topoId);
        BwSharedGroupContainer deletedBwGroups = BwSharedGroupMng.getDeletedGroups(newBwShareGroup, bwSharedGroups);
        this.bwSharedGroups = newBwShareGroup;
        //priority can not update
        final PceResult pceResult = calcSinglePath(masterPath, true, teArg, false,
                                                   bwSharedGroups, deletedBwGroups);
        TeArgumentBean slaveTeArg = PceUtil.generatorSlaveTeArg(teArg, teArgSlave);
        pceResult.merge(calcSinglePath(slavePath, false, slaveTeArg, false,
                                       bwSharedGroups, deletedBwGroups));
        teArg.setForceCalcPathWithBandwidth(false);
        setChangeToZeroBandWidth(false);
        notifyReverseTunnel(false);
        return pceResult;
    }

    @Override
    public void destroy() {
        super.destroy();
        clearAllPathInfo();
    }

    @Override
    public PceResult reCalcPath(TunnelUnifyKey path) {
        PceResult result = calcPath();

        if (BiDirect.isBiDirectBinding(biDirect)) {
            TunnelHsbPathInstance reversePathInstance = PcePathProvider.getInstance()
                    .getTunnelHsbInstance(tailNodeId, (int) biDirect.getReverseId(), isSimulate());
            PceResult reverseResult = reversePathInstance.calcPath();
            result.merge(reverseResult);
        }

        return result;
    }

    @Override
    public PceResult refreshPath(TunnelUnifyKey path) {
        return calcPath();
    }

    @Override
    public boolean isSrlgOverlap() {
        return PceUtil.isHsbSrlgOverlap(masterSrlgAttr, slaveSrlgAttr);
    }

    @Override
    protected void notifyReverseTunnel(boolean isNeedNotify) {
        if (BiDirect.isBiDirectPositive(biDirect)) {
            biDirect.notifyReverseChange(tailNodeId, getTunnelUnifyKey(), isNeedNotify);
        }
    }

    @Override
    public String toString() {
        return HsbPrintUtils.toString(this);
    }

    public PceResult calcPath() {
        try {
            return calcPathAsync(false, teArg, teArgSlave, bwSharedGroups, null).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("calcPathWithFailRollBack witch exception {}", e);
        }
        return PceResult.nullPceResult;
    }

    public ListenableFuture<PceResult> calcPathAsync(
            boolean isFailRollback, TeArgumentBean newTeArg,
            TeArgumentBeanLsp newSlaveTeArg, BwSharedGroupContainer bwSharedGroups,
            BwSharedGroupContainer deletedBwSharedGroups) {
        return Futures.transform(
                calcSinglePathAsync(masterPath, true, newTeArg, isFailRollback, bwSharedGroups, deletedBwSharedGroups),
                new CreateTunnelHsbPathAsyncFunction(isFailRollback, newTeArg, newSlaveTeArg, bwSharedGroups,
                                                     deletedBwSharedGroups));
    }

    @SuppressWarnings("unchecked")
    private ListenableFuture<PceResult> calcSinglePathAsync(
            List<Link> oldPath, boolean isMaster,
            TeArgumentBean teArgBean, boolean isFailRollback, BwSharedGroupContainer bwSharedGroups,
            BwSharedGroupContainer deletedBwSharedGroups) {
        TunnelUnifyKey tunnelKey = isMaster ? masterTunnelUnifyKey : slaveTunnelUnifyKey;

        PathProvider<MetricTransformer> pathProvider =
                new PathProvider(headNodeId, tunnelKey, tailNodeId, topoId,
                                 generateCalculateStrategy(), generateTransformerFactory());
        pathProvider.setTeArgWithBuildNew(teArgBean);
        pathProvider.setOldPath(oldPath);
        pathProvider.setBiDirect(biDirect);
        pathProvider.setRecalc(recalcWithoutDelay);
        pathProvider.setBwSharedGroups(bwSharedGroups, deletedBwSharedGroups);
        pathProvider.setMultiplePathsParam(this.multiplePathsParam);

        if (!isSlaveIndependent()) {
            pathProvider.setOverlapPath(overlapPath);
        }
        if (!isMaster) {
            if (getTunnelUnifyKey().isServiceInstance()) {
                return calcSlaveServicePath(pathProvider);
            }
            pathProvider.clearTryToAvoidLinks();
            PriorityAvoidLinks priorityAvoidLinks = generatePriorityAvoidLinks(teArgBean);
            if (!CollectionUtils.isNullOrEmpty(masterPath)) {
                pathProvider.setRecalc(true);
            }
            return calcSlavePathAsync(oldPath, pathProvider, priorityAvoidLinks);
        } else {
            pathProvider.setFailRollback(isFailRollback);
            return calcMasterPathAsync(isFailRollback, pathProvider);
        }
    }

    private ListenableFuture<PceResult> calcSlavePathAsync(
            List<Link> oldPath,
            PathProvider<MetricTransformer> pathProvider, PriorityAvoidLinks priorityAvoidLinks) {
        return Futures.transform(
                priorityAvoidLinks.calcPathByAvoidPriorityAsync(pathProvider, slaveTunnelUnifyKey, oldPath),
                (AsyncFunction<PceResult, PceResult>) calResult -> {
                    setSlaveLspInfo(pathProvider);
                    calcOverlapPath();
                    return Futures.immediateFuture(calResult);
                });
    }

    private ListenableFuture<PceResult> calcMasterPathAsync(
            boolean isFailRollback,
            PathProvider<MetricTransformer> pathProvider) {
        return Futures.transform(pathProvider.calcPathAsync(), (AsyncFunction<PceResult, PceResult>) calResult -> {
            if (HsbTunnelUtil.needRollback(isFailRollback, calResult)) {
                return Futures.immediateFuture(calResult);
            }
            setMasterLspInfo(pathProvider);
            calcOverlapPath();
            calResult.setCalcFail(CollectionUtils.isNullOrEmpty(masterPath));
            return Futures.immediateFuture(calResult);
        });
    }

    public void updateTopoId(TopologyId topoId) {
        this.topoId = topoId;
        calcPath();
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
        //to do
        return teArg.getArgComm().getBandwidth();
    }

    @Override
    public byte getHoldPriority() {
        return Optional.ofNullable(teArg).map(TeArgumentBean::getHoldPriority).orElse(DiffServBw.LOWEST_PRIORITY);
    }

    @Override
    public TunnelUnifyKey getTunnelUnifyKey() {
        return this.masterTunnelUnifyKey;
    }

    @Override
    public List<Link> getMasterPath() {
        return masterPath;
    }

    @Override
    public List<Link> getSlavePath() {
        return slavePath;
    }

    @Override
    public synchronized PceResult refreshPath() {
        if (BiDirect.isBiDirectReverse(biDirect)) {
            return PceResult.create();
        }
        LOG.info("refreshPath {}", getTunnelUnifyKey());
        final List<PathSegment> oldMasterSegments = masterPathSegments;
        final List<PathSegment> oldSlaveSegments = slavePathSegments;
        final List<Link> oldMasterPath = masterPath;
        final List<Link> oldSlavePath = slavePath;
        final boolean oldZeroBandWidthFlag = isChangeToZeroBandWidth();
        final PceResult pceResult = calcRefreshPath();
        writeDbAndNotifyPathChange(oldMasterPath, oldSlavePath, oldZeroBandWidthFlag != isChangeToZeroBandWidth());
        logRefreshedPath("refresh path");
        notifySegmentsChange(oldMasterSegments, oldSlaveSegments);
        return pceResult;
    }

    @Override
    public synchronized PceResult refreshUnestablishPath() {
        return refreshUnestablishedOrSrlgPath(false);
    }

    @Override
    public synchronized PceResult refreshUnestablishAndSrlgPath() {
        return refreshUnestablishedOrSrlgPath(true);
    }

    @Override
    public void refreshSegments() {
        if (!isSrTunnel()) {
            return;
        }
        Logs.info(LOG, "refreshSegments tunnelId={} headNodeId={}", masterTunnelUnifyKey.getId(), headNodeId);
        final List<PathSegment> oldMasterPathSegments = masterPathSegments;
        final List<PathSegment> oldSlavePathSegments = slavePathSegments;
        masterPathSegments = PcePathProvider.getInstance().calcSegments(true, masterPath, topoId, masterTunnelUnifyKey);
        slavePathSegments = PcePathProvider.getInstance().calcSegments(true, slavePath, topoId, slaveTunnelUnifyKey);
        writeDb();
        notifySegmentsChange(oldMasterPathSegments, oldSlavePathSegments);
    }

    @Override
    public void decreaseBandwidth(long newBandwidth, BwSharedGroupContainer bwContainer) {
        if (BiDirect.isBiDirectReverse(biDirect)) {
            return;
        }

        if (teArg == null || teArg.getBandWidth() == 0) {
            LOG.error("oldBandwidth is null!", masterTunnelUnifyKey.toString());
            return;
        }

        final long oldBandwidth = teArg.getBandWidth();
        boolean isNewBwLarger = newBandwidth > oldBandwidth;
        boolean isEqual = newBandwidth == oldBandwidth;
        Conditions.ifTrue(
                isNewBwLarger,
                () -> Logs.error(LOG, "newBandwidth > oldBandwidth!", masterTunnelUnifyKey.toString()));
        Conditions.ifTrue(
                isEqual,
                () -> Logs.error(LOG, "newBandwidth = oldBandwidth!", masterTunnelUnifyKey.toString()));

        if (Conditions.or(isNewBwLarger, isEqual)) {
            return;
        }

        teArg.setBandWidth(newBandwidth);

        BwSharedGroupContainer newGroups = HsbBwMngUtils.decreaseBw(this, newBandwidth, bwContainer);
        setBwSharedGroup(newGroups);
        setChangeToZeroBandWidth(false);
        //else do not update bwContainer, waiting for update
        writeDb();
    }

    @Override
    public void notifyPathChange() {
        //to do
    }

    @Override
    public BiDirect getBiDirect() {
        return biDirect;
    }

    @Override
    public TeArgumentBean getTeArgumentBean() {
        return this.teArg;
    }

    @Override
    public boolean isDelayEligible() {
        return !isDelayRestricted() || masterDelay <= teArg.getMaxDelay();
    }

    @Override
    public boolean isUnestablished() {
        return CollectionUtils.isNullOrEmpty(masterPath);
    }

    @Override
    public boolean isPathOverlap() {
        return !overlapPath.isEmpty();
    }

    private void notifySegmentsChange(List<PathSegment> oldMasterSegments, List<PathSegment> oldSlaveSegments) {
        if (isSimulate() || !isSrTunnel()) {
            return;
        }
        final boolean isMasterSegmentsChange = !Objects.equals(oldMasterSegments, masterPathSegments);
        final boolean isSlaveSegmentsChange = !Objects.equals(oldSlaveSegments, slavePathSegments);
        if ((isMasterSegmentsChange || isSlaveSegmentsChange) && CommonTunnel.NORMAL_STATE == getTunnelState()) {
            Logs.debug(LOG,
                       "NotifySegmentsChange: head={} tunnelId={} oldMaster={} oldSlave={} newMaster={} newSlave={}",
                       getHeadNodeId(), getId(), oldMasterSegments, oldSlaveSegments, masterPathSegments,
                       slavePathSegments);
            notifySegmentsChange();
        }
    }

    public NodeId getHeadNodeId() {
        return this.headNodeId;
    }

    public NodeId getTailNodeId() {
        return this.tailNodeId;
    }

    public TunnelUnifyKey getMasterTunnelUnifyKey() {
        return this.masterTunnelUnifyKey;
    }

    public TunnelUnifyKey getSlaveTunnelUnifyKey() {
        return this.slaveTunnelUnifyKey;
    }

    public TeArgumentBeanLsp getSlaveTeArg() {
        return teArgSlave;
    }

    public long getMasterMetric() {
        return masterMetric;
    }

    public long getMasterDelay() {
        return masterDelay;
    }

    public long getSlaveDelay() {
        return slaveDelay;
    }

    public long getSlaveMetric() {
        return slaveMetric;
    }

    public List<PathSegment> getMasterPathSegments() {
        return masterPathSegments;
    }

    public List<PathSegment> getSlavePathSegments() {
        return slavePathSegments;
    }

    protected void setBandwidth(long bandwidth) {
        this.teArg.updateBandWidth(bandwidth);
    }

    protected boolean isBiDirect() {
        return biDirect != null;
    }

    protected void setBiDirect(BiDirect biDirect) {
        this.biDirect = biDirect;
    }

    public List<Link> getOverlapPath() {
        return overlapPath;
    }

    public CalculateStrategyContainer getCalculateStrategy() {
        return calculateStrategy;
    }

    public boolean isSrlgEnabled() {
        return isSrlgEnabled;
    }

    public void logRefreshedPath(String description) {
        Logs.debug(LOG, "Hsb {} {}\n master path:\n{}\nslave path:\n{}\n", this.getTunnelUnifyKey(), description,
                   ComUtility.pathToString(masterPath), ComUtility.pathToString(slavePath));
    }

    public void recoverHsbPathBw() {
        HsbBwMngUtils.recoverHsbPathBw(this);
    }

    private class CreateTunnelHsbPathAsyncFunction implements AsyncFunction<PceResult, PceResult> {
        CreateTunnelHsbPathAsyncFunction(
                boolean isFailRollback, TeArgumentBean newTeArg,
                TeArgumentBeanLsp newSlaveTeArg, BwSharedGroupContainer bwGroups,
                BwSharedGroupContainer deletedBwSharedGroups) {
            this.isFailRollback = isFailRollback;
            this.masterTeArg = newTeArg;
            this.slaveTeArgBeanLsp = newSlaveTeArg;
            this.delBwGroups = deletedBwSharedGroups;
            this.bwGroups = bwGroups;
        }

        TeArgumentBean masterTeArg;
        TeArgumentBeanLsp slaveTeArgBeanLsp;
        boolean isFailRollback;
        BwSharedGroupContainer bwGroups;
        BwSharedGroupContainer delBwGroups;

        @Override
        public ListenableFuture<PceResult> apply(PceResult calcMasterResult) throws Exception {
            if (!isSlaveIndependent() && calcMasterResult.isCalcFail()) {
                if (!isFailRollback) {
                    maintenanceSlavePath();
                }
                return Futures.immediateFuture(calcMasterResult);
            }
            TeArgumentBean newSlaveTeArg = PceUtil.generatorSlaveTeArg(masterTeArg, slaveTeArgBeanLsp);
            return Futures.transform(
                    calcSinglePathAsync(slavePath, false, newSlaveTeArg, false, bwGroups, delBwGroups),
                    (AsyncFunction<PceResult, PceResult>) calcSlaveResult -> {
                        calcMasterResult.merge(calcSlaveResult);
                        return Futures.immediateFuture(calcMasterResult);
                    });
        }
    }
}
