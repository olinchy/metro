/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.pathcore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.multiple.paths.param.grouping.MultiplePathsParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandwidthAllocException;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroupMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.AbstractMultiplePathsProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.ContrainedMultiplePathsProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input.ConstrainedOptimalPathInput;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input.ConstrainedOptimalPathInputBuilder;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser.PathChooser;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser.PathChooserFactory;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.CommonTunnel;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.ITunnel;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.srlg.AovidLinks;
import com.zte.ngip.ipsdn.pce.path.api.srlg.SrlgAttribute;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.PceComputeResult;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.LspGetPath;
import com.zte.ngip.ipsdn.pce.path.core.OptimalPath;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.strategy.ICalcStrategy;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformer;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformerFactory;

public class PathProvider<T extends ITransformer<Link>> {
    /**
     * constructor.
     *
     * @param headNodeId   headNodeId
     * @param tunnelPathId tunnelPathId
     * @param tailNodeId   tailNodeId
     * @param topoId       topoId
     * @param strategy     strategy
     * @param factory      factory
     * @param <F>          F
     */
    public <F extends ITransformerFactory<T>> PathProvider(
            NodeId headNodeId, TunnelUnifyKey tunnelPathId,
            NodeId tailNodeId, TopologyId topoId, ICalcStrategy<NodeId, Link> strategy, F factory) {
        this.headNodeId = headNodeId;
        this.tunnelUnifyKey = tunnelPathId;
        this.tailNodeId = tailNodeId;
        this.topoId = topoId;
        this.strategy = strategy;
        this.factory = factory;
        this.isNeedBandScaled = false;
    }

    private static final Logger LOG = LoggerFactory.getLogger(PathProvider.class);
    private static final long REQ_MAX_DELAY = ComUtility.INVALID_DELAY;
    private NodeId headNodeId;
    private NodeId tailNodeId;
    private TopologyId topoId;
    private TeArgumentBean teArg;
    private List<Link> path;
    private List<Link> oldPath;
    private List<AovidLinks> tryToAvoidLinks = Lists.newArrayList();
    private List<Link> excludePath = Lists.newArrayList();
    private TunnelUnifyKey tunnelUnifyKey;
    private BiDirect biDirect;
    private LspAttributes lspAttributes = new LspAttributes();
    private boolean isRealTime = false;
    private ICalcStrategy<NodeId, Link> strategy;
    private ITransformerFactory factory;
    private boolean failRollback = false;
    private boolean recalc;
    private BwSharedGroupContainer bwSharedGroups;
    private BwSharedGroupContainer deletedBwSharedGroups;
    private long reqMaxDelay = REQ_MAX_DELAY;
    private List<Link> overlapPath = null;
    private boolean isNeedBandScaled;
    private MultiplePathsParam multiplePathsParam = null;
    private List<PathResult> pathResultList = new ArrayList<>();
    private PathChooserFactory.PathChooserName pathChooserName;

    private static boolean canScaleBandwidth() {
        return PceUtil.isCanBandWidthScaled() && PcePathProvider.getInstance().pceGetBandScaleGlobalFlag();
    }

    private static TeArgumentBean buildNewTeArg(TeArgumentBean teArg) {
        TeArgumentBean newTeArg = new TeArgumentBean(teArg);
        newTeArg.setMaxDelay(REQ_MAX_DELAY);
        return newTeArg;
    }

    public List<Link> getPath() {
        return this.path;
    }

    public List<PathResult> getPathResultList() {
        return pathResultList;
    }

    public long getLspMetric() {
        return lspAttributes.getLspMetric();
    }

    public long getLspDelay() {
        return lspAttributes.getLspDelay();
    }

    public SrlgAttribute getSrlgAttr() {
        return lspAttributes.getSrlgAttr();
    }

    public TeArgumentBean getTeArg() {
        return this.teArg;
    }

    public void setTeArg(TeArgumentBean teArgBean) {
        this.teArg = teArgBean;
    }

    /**
     * build a new teArg which has max delay.
     *
     * @param teArg old teArg
     */
    public void setTeArgWithBuildNew(TeArgumentBean teArg) {
        this.reqMaxDelay = teArg.getMaxDelay();
        this.teArg = buildNewTeArg(teArg);
        if (teArg.getTryToAvoidLink() != null) {
            setAvoidPath(teArg.getTryToAvoidLinkInLinks());
        }
        LOG.debug("start calc path on {}: reqMaxDelay={} {}", topoId, reqMaxDelay,
                  ComUtility.getTunnelUnifyKeyStringIfPresent(tunnelUnifyKey));
    }

    private void setAvoidPath(List<Link> tryToAvoidLink) {

        if (tryToAvoidLink != null) {
            List<Link> avoidLinks;
            avoidLinks = tryToAvoidLink.stream().map(tryAvoidLink -> new LinkBuilder(tryAvoidLink).build())
                    .collect(Collectors.toList());
            tryToAvoidLinks = Lists.newArrayList(new AovidLinks(avoidLinks));
        }
    }

    public void clearTryToAvoidLinks() {
        tryToAvoidLinks.clear();
    }

    public void setOldPath(List<Link> oldPath) {
        this.oldPath = oldPath;
    }

    public void setOverlapPath(List<Link> overlapPath) {
        this.overlapPath = overlapPath;
    }

    /**
     * addAvoidPath.
     *
     * @param avoidPath avoidPath
     */
    public void addAvoidPath(List<AovidLinks> avoidPath) {
        if (avoidPath != null) {
            tryToAvoidLinks.addAll(avoidPath);
        }
    }

    /**
     * setExcludePath.
     *
     * @param excludePath excludePath
     */
    public void setExcludePath(List<Link> excludePath) {
        if (excludePath == null) {
            this.excludePath.clear();
            return;
        }
        this.excludePath.addAll(excludePath);
    }

    public void setBiDirect(BiDirect biDirect) {
        this.biDirect = biDirect;
    }

    /**
     * setBwSharedGroups.
     *
     * @param groups        groups
     * @param deletedGroups deletedGroups
     */
    public void setBwSharedGroups(BwSharedGroupContainer groups, BwSharedGroupContainer deletedGroups) {
        if ((groups == null) || (groups.getBwSharedGroupMember() == null) || (groups.getBwSharedGroupMember()
                .isEmpty())) {
            this.bwSharedGroups = null;
        } else {
            this.bwSharedGroups = groups;
        }
        this.deletedBwSharedGroups = deletedGroups;
    }

    public void setIsRealTimePath(boolean isRealTimePath) {
        this.isRealTime = isRealTimePath;
    }

    public void setFailRollback(boolean failRollback) {
        this.failRollback = failRollback;
    }

    private void doRecordPerPort(PceResult result) {
        if (isRealTime) {
            return;
        }
        if (failRollback && ((path == null) || (path.isEmpty()))) {
            result.setCalcFail(true);
            return;
        }
        recordPerPort();
    }

    /**
     * calcPath.
     *
     * @param result result of calcPath
     */
    public void calcPath(PceResult result) {

        if (biDirect != null && biDirect.isReverse()) {
            path = biDirect.reverseGetPath(tunnelUnifyKey, tailNodeId, topoId);
            doRecordPerPort(result);
            return;
        }

        calcPathProcess(result);
    }

    private boolean isNeedComputeWithOutBandWidth() {
        if ((teArg.getBandWidth() == 0L) && (!teArg.isForceCalcPathWithBandwidth())) {
            return true;
        }
        if (tunnelUnifyKey != null && !tunnelUnifyKey.isMaster() && !teArg.isComputeLspWithBandWidth()) {
            return true;
        }
        return false;
    }

    public void setPathChooserName(PathChooserFactory.PathChooserName pathChooserName) {
        this.pathChooserName = pathChooserName;
    }

    /**
     * calcPathAsync.
     *
     * @return PceResult
     */
    public ListenableFuture<PceResult> calcPathAsync() {
        PceResult result = new PceResult();
        calcPath(result);
        if (path == null || path.isEmpty()) {
            LOG.info("calcPathAsync failed  {}: {}", tunnelUnifyKey, result.getCalcFailType());
        } else {
            LOG.info("calcPathAsync success {}", tunnelUnifyKey);
        }
        return Futures.immediateFuture(result);
    }

    private void calcPathProcess(PceResult result) {
        if (teArg == null) {
            if (excludePath.isEmpty()) {
                calcShortestPath();
            } else {
                calcContrainedPath(result);
            }
            callTopoCalcPathSuccess(result);
            doRecordPerPort(result);
        } else if (isNeedComputeWithOutBandWidth()) {
            calcContrainedPath(result);
            callTopoCalcPathSuccess(result);
            doRecordPerPort(result);
        } else {
            calcPathWithBw(result);
        }
    }

    private void calcPathWithBw(PceResult result) {
        try {
            PceResult resultFuture;
            if (tunnelUnifyKey != null && tunnelUnifyKey.isSimulate()) {
                resultFuture = BandWidthMng.getInstance().submitSimuExcute(new CalcPathWithBwTask(result)).get();
            } else {
                resultFuture = BandWidthMng.getInstance().submit(new CalcPathWithBwTask(result)).get();
            }
            if (!isRealTime) {
                result.merge(resultFuture);
                result.deleteSamePreemptedTunnels();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("calcPathWithBw face error ", e);
            path = null;
        }
    }

    private void recordPerPort() {
        if (this.tunnelUnifyKey == null) {
            return;
        }
        if (tunnelUnifyKey.isNormalTunnel()) {
            TunnelUnifyKey tunnelUnifyRecordKey = new TunnelUnifyRecordKey(this.tunnelUnifyKey);
            TunnelsRecordPerPort.getInstance().update(tunnelUnifyRecordKey, oldPath, path);
        } else if (tunnelUnifyKey.isHsbFlag()) {
            TunnelUnifyKey tunnelUnifyRecordKey = new TunnelUnifyRecordKey(this.tunnelUnifyKey);
            TunnelsRecordPerPort.getInstance().update(tunnelUnifyRecordKey, oldPath, path);
        } else if (tunnelUnifyKey.isTg()) {
            TunnelsRecordPerPort.getInstance().update(tunnelUnifyKey, oldPath, path);
        }
    }

    private void calcShortestPath() {
        Map<NodeId, List<Link>> incomingMap = calcIncomingMap();
        if (!incomingMap.containsKey(tailNodeId)) {
            return;
        }

        path = LspGetPath.getPath(incomingMap, headNodeId, tailNodeId);
        calcLspAttributes();
    }

    private Map<NodeId, List<Link>> calcIncomingMap() {
        List<NodeId> destNodeList = new ArrayList<>();
        destNodeList.add(tailNodeId);
        Logs.debug(LOG, "calcIncomingMap {}", topoId);
        OptimalPath<NodeId, Link> sp = new OptimalPath<>(headNodeId,
                                                         TopoServiceAdapter.getInstance().getPceTopoProvider()
                                                                 .getTopoGraph(
                                                                         ComUtility.getSimulateFlag(tunnelUnifyKey),
                                                                         topoId), strategy);
        sp.setDestNodeList(destNodeList);
        sp.setEdgeMeasure(getMetricTransform());

        sp.calcSpt();
        return sp.getIncomingEdgeMap();
    }

    private void calcContrainedPath(PceResult result) {

        ComUtility.checkTopo(tunnelUnifyKey, headNodeId, tailNodeId,
                             TopoServiceAdapter.getInstance().getPceTopoProvider()
                                     .getTopoGraph(ComUtility.getSimulateFlag(tunnelUnifyKey), topoId));

        LOG.debug("calcContrainedPath: topoId={} maxDelay={} {}", topoId, teArg.getMaxDelay(),
                  ComUtility.getTunnelUnifyKeyStringIfPresent(tunnelUnifyKey));

        calcContrainedPathByKsp(result);
    }

    /**
     * calcContrainedPathByKsp.
     *
     * @param result result
     */
    private void calcContrainedPathByKsp(PceResult result) {

        ConstrainedOptimalPathInput<NodeId, Link> initialInput = new ConstrainedOptimalPathInputBuilder()
                .setHeadNodeId(headNodeId).setTailNodeId(tailNodeId)
                .setTopologyId(topoId).setCalcStrategy(strategy).setTransformer(getMetricTransform())
                .setTunnelUnifyKey(tunnelUnifyKey).setTeArgumentBean(teArg).setBiDirect(biDirect)
                .setExcludedLinks(excludePath).setBwSharedGroups(bwSharedGroups)
                .setDeletedBwSharedGroups(deletedBwSharedGroups).setIsNeedScaleBandwith(isNeedBandScaled)
                .setOldPath(oldPath).setReqMaxDelay(reqMaxDelay).build();

        Optional<MultiplePathsParam> multiplePathsParamOptional = Optional.ofNullable(multiplePathsParam);
        final int maxK = AbstractMultiplePathsProvider.MAX_K;
        final int chooseNum = multiplePathsParamOptional.isPresent()
                ? multiplePathsParamOptional.map(MultiplePathsParam::getChooseNum)
                .orElse(AbstractMultiplePathsProvider.MAX_K) : 1;

        final PathChooser<PathResult> chooser = PathChooserFactory.create(chooseNum, initialInput, recalc,
                                                                          tryToAvoidLinks.stream().flatMap(
                                                                                  listLinks -> listLinks.getLinks().stream()).distinct()
                                                                                  .collect(Collectors.toList()),
                                                                          pathChooserName);

        AbstractMultiplePathsProvider multiplePathsProvider = new ContrainedMultiplePathsProvider(initialInput);
        if (null == multiplePathsParam) {
            PathResult pathResult = multiplePathsProvider.calcMultiplePathsAndChooseOne(maxK, chooser);
            path = pathResult.getPath();
            calcLspAttributes();
            result.merge(pathResult.getPceResult());
            result.setBandWidthScaleList(pathResult.getPceResult().getBandWidthScaleList());
            result.setFailReason(pathResult.getPceResult().getFailReason());
            result.setCalcFail(pathResult.getPceResult().isCalcFail());
            reCalcForScalingBandwidth(result);
        } else {
            List<PathResult> pathResults = multiplePathsProvider.calcMultiplePaths(maxK, chooser);
            pathResultList.addAll(pathResults);
        }
    }

    private void reCalcForScalingBandwidth(PceResult result) {
        if (isCalcPathFailed(result) && canScaleBandwidth() && !isNeedBandScaled) {
            LOG.info("recalc path with scale bandWidth {}", tunnelUnifyKey);
            isNeedBandScaled = true;
            calcContrainedPath(result);/*recal path when has vte link,because vtelink can band scaled*/
        }
    }

    private boolean isCalcPathFailed(PceResult result) {
        return CollectionUtils.isNullOrEmpty(path) && result.getCalcFailType() == CalcFailType.NoPath;
    }

    @SuppressWarnings("unchecked")
    private ITransformer<Link> getMetricTransform() {
        return (T) factory.create(tryToAvoidLinks, tunnelUnifyKey);
    }

    public void setRecalc(boolean recalc) {
        this.recalc = recalc;
    }

    public void setMultiplePathsParam(MultiplePathsParam multiplePathsParam) {
        this.multiplePathsParam = multiplePathsParam;
    }

    private boolean isFailAndRollback(PceResult result) {
        if (failRollback && ((path == null) || (path.isEmpty()))) {
            result.setCalcFail(true);
            return true;
        }
        return false;
    }

    private boolean isBwEnough(Link incomingEdge, long bandwidth, byte preemptPriority, byte holdPriority) {
        if (bwSharedGroups != null) {
            return BwSharedGroupMng.getInstance()
                    .hasEnoughBw(incomingEdge, preemptPriority, holdPriority, bwSharedGroups, deletedBwSharedGroups,
                                 biDirect != null, tunnelUnifyKey);
        }
        if (deletedBwSharedGroups != null) {
            return BwSharedGroupMng.getInstance()
                    .hasEnoughBw(incomingEdge, preemptPriority, holdPriority, bandwidth, deletedBwSharedGroups,
                                 biDirect != null, tunnelUnifyKey);
        }

        return BandWidthMng.getInstance()
                .hasEnoughBw(incomingEdge, preemptPriority, holdPriority, bandwidth, tunnelUnifyKey, biDirect != null);
    }

    private void setPceResultFail(PceResult result, CalcFailType calcFailType) {
        result.reset();
        result.setFailReason(calcFailType);
        result.setCalcFail(true);
        path.clear();
    }

    private boolean callTopoCalcPathSuccess(PceResult result) {
        if (isRealTime || tunnelUnifyKey.isSimulate()) {
            return true;
        }
        Logs.info(LOG, "callTopoCalcPathSuccess tunnelUnifyKey={}", tunnelUnifyKey);
        PceComputeResult pceComputeResult = PceUtil.generatorPceComputeResult(topoId, path, tunnelUnifyKey, result);
        ITunnel tunnel = PcePathProvider.getInstance().getTunnelInstance(tunnelUnifyKey);
        if (tunnel != null) {
            tunnel.setTunnelState(CommonTunnel.ACTIVE_WAIT_STATE);
        }
        if (!TopoServiceAdapter.getInstance().getPceTopoProvider().onCalcPathSuccess(pceComputeResult)) {
            setPceResultFail(result, CalcFailType.BandWidthSacledFail);
        }
        if (tunnel != null) {
            tunnel.comAndSetTunnelState(CommonTunnel.ACTIVE_WAIT_STATE, CommonTunnel.NORMAL_STATE);
        }
        return !isFailAndRollback(result);
    }

    public PathProvider setNeedBandScaled(boolean needBandScaled) {
        isNeedBandScaled = needBandScaled;
        return this;
    }

    private void calcLspAttributes() {
        lspAttributes = PceUtil.calcLspAttributes(path);
    }

    private class CalcPathWithBwTask implements Callable<PceResult> {
        CalcPathWithBwTask(PceResult result) {

            this.pceResult = result;
        }

        private PceResult pceResult;

        @Override
        public PceResult call() throws BandwidthAllocException {
            calcContrainedPath(pceResult);
            if (isRealTime) {
                return null;
            }
            if (isFailAndRollback(pceResult)) {
                return pceResult;
            }
            if (!callTopoCalcPathSuccess(pceResult)) {
                return pceResult;
            }
            BandWidthMng.getInstance().dealBandWidth(this::handleBandWidth);
            return pceResult;
        }

        private void handleBandWidth() throws BandwidthAllocException {
            if (teArg.getBandWidth() != 0) {
                path.stream().filter(link -> !isBwEnough(link, teArg.getBandWidth(), teArg.getPreemptPriority(),
                                                         teArg.getHoldPriority())).findFirst().ifPresent(linkFilter -> {
                    Logs.info(LOG, "{} has no enough bandwidth {}", linkFilter, teArg.getBandWidth());
                    setPceResultFail(pceResult, CalcFailType.NoEnoughBandwidth);
                });
            }
            if (pceResult.isCalcFail()) {
                path.clear();
                if (failRollback) {
                    return;
                }
            }

            freeDeletedBwSharedGroups(pceResult);
            allocBw(pceResult);
            freeOldPathBw(pceResult);
            doRecordPerPort(pceResult);
        }

        private void freeOldPathBw(PceResult result) {
            if (oldPath == null || oldPath.isEmpty()) {
                return;
            }

            if (path == null || path.isEmpty()) {
                freeOldBw(result);
                return;
            }

            for (Link linkOld : oldPath) {
                if (!findLinkInPath(linkOld)) {
                    freeLinkBw(linkOld, result);
                }
            }
        }

        private boolean findLinkInPath(Link linkOld) {
            for (Link linkNew : path) {
                if (linkEqual(linkOld, linkNew)) {
                    return true;
                }
            }

            return (overlapPath != null) && (pathContainsLink(overlapPath, linkOld)) && teArg
                    .isComputeLspWithBandWidth();
        }

        private void freeDeletedBwSharedGroups(PceResult pceResult) {
            BwSharedGroupMng.getInstance()
                    .delTunnelAndFreeBw(oldPath, tunnelUnifyKey, deletedBwSharedGroups, biDirect != null, pceResult);
        }

        private void freeOldBw(PceResult result) {
            Predicate<Link> notNeedFreeBandWidth =
                    link -> (overlapPath != null) && (pathContainsLink(overlapPath, link)) && (teArg
                            .isComputeLspWithBandWidth());
            oldPath.stream().filter(notNeedFreeBandWidth.negate()).forEach(link -> freeLinkBw(link, result));
        }

        private boolean pathContainsLink(List<Link> path, Link link) {
            if (path == null || path.isEmpty()) {
                return false;
            }

            for (Link pathLink : path) {
                if (linkEqual(pathLink, link)) {
                    return true;
                }
            }

            return false;
        }

        private void freeLinkBw(Link link, PceResult result) {
            if (bwSharedGroups == null) {
                BandWidthMng.getInstance()
                        .free(link, teArg.getHoldPriority(), tunnelUnifyKey, result, biDirect != null);
            } else {
                BwSharedGroupMng.getInstance().freeBw(link, tunnelUnifyKey, bwSharedGroups, result, biDirect != null);
            }
        }

        private boolean linkEqual(Link link1, Link link2) {
            return link1.getSource().equals(link2.getSource()) && (link1.getDestination()
                    .equals(link2.getDestination()));
        }

        private void allocBw(PceResult result) throws BandwidthAllocException {
            if (path == null) {
                return;
            }

            if (bwSharedGroups == null) {
                result.merge(BandWidthMng.getInstance()
                                     .alloc(path, teArg.getPreemptPriority(), teArg.getHoldPriority(),
                                            teArg.getBandWidth(),
                                            tunnelUnifyKey, biDirect != null));
                return;
            }

            result.merge(BwSharedGroupMng.getInstance()
                                 .allocBw(path, teArg.getPreemptPriority(), teArg.getHoldPriority(), tunnelUnifyKey,
                                          bwSharedGroups,
                                          biDirect != null));
        }
    }
}
