/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import edu.uci.ics.jung.graph.Graph;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.common.data.TeArgCommonData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.common.data.TeArgCommonDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TeArgument;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TeArgumentLsp;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bi.direct.argument.BiDirectContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bi.direct.argument.bi.direct.container.bidirect.type.Bidirectional;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.CalculateStrategyContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.calculate.strategy.container.StrategyType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.links.PathLink;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.links.PathLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.segments.Segment;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.segments.SegmentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.LspAttributes;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathDb;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.ITunnel;

import com.zte.ngip.ipsdn.pce.path.api.graph.GraphCommonUtils;
import com.zte.ngip.ipsdn.pce.path.api.segmentrouting.PathSegment;
import com.zte.ngip.ipsdn.pce.path.api.srlg.AvoidSrlg;
import com.zte.ngip.ipsdn.pce.path.api.srlg.Srlg;
import com.zte.ngip.ipsdn.pce.path.api.srlg.SrlgAttribute;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.PceComputeResult;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBeanLsp;
import com.zte.ngip.ipsdn.pce.path.core.strategy.DelayStrategy;
import com.zte.ngip.ipsdn.pce.path.core.strategy.ICalcStrategy;
import com.zte.ngip.ipsdn.pce.path.core.strategy.MetricStrategy;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;
import com.zte.ngip.ipsdn.pce.path.core.transformer.DelayTransformerFactory;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformerFactory;
import com.zte.ngip.ipsdn.pce.path.core.transformer.MetricTransformerFactory;

public class PceUtil {
    private PceUtil() {

    }

    private static final Logger LOG = LoggerFactory.getLogger(PceUtil.class);

    public static boolean isMaintenance(TeArgumentLsp maintenanceLsp) {
        return maintenanceLsp != null;
    }

    public static boolean isTunnelBandwidthShrinkToZero(TeArgumentBean oldArg, Long newBandwidth) {
        if (null == oldArg) {
            return false;
        }

        if (0 == oldArg.getBandWidth()) {
            return false;
        }

        if ((null == newBandwidth) || (0 == newBandwidth)) {
            return true;
        }
        return false;
    }

    public static List<Link> getSrlgAvoidLinks(List<Link> masterPath, Graph<NodeId, Link> topoGraph) {
        AvoidSrlg avoidSrlg = getAvoidSrlgFromLink(masterPath);
        List<Link> retLinks = Lists.newArrayList();

        if (null == topoGraph) {
            return retLinks;
        }

        GraphCommonUtils.forEachEdge(topoGraph, link -> {
            SrlgAttribute srlgAttr = TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkSrlgAttr(link);
            if (avoidSrlg.match(srlgAttr) && !masterPath.contains(link)) {
                retLinks.add(link);
            }
        });
        return retLinks;
    }

    public static AvoidSrlg getAvoidSrlgFromLink(List<Link> path) {
        AvoidSrlg avoidSrlg = new AvoidSrlg();

        if (null == path) {
            return avoidSrlg;
        }

        for (Link link : path) {
            SrlgAttribute srlgAttribute = TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkSrlgAttr(link);
            srlgAttribute.mergeToAvoidSrlg(avoidSrlg);
        }
        return avoidSrlg;
    }

    public static Long getReverseLinkDelay(Graph<NodeId, Link> graph, Link link) {
        List<Link> links = ComUtility
                .getLinkInGraph(graph, link.getDestination().getDestNode(), link.getDestination().getDestTp(),
                                link.getSource().getSourceNode(), link.getSource().getSourceTp());

        if ((links != null) && (!links.isEmpty())) {
            return TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkDelay(links.get(0));
        }

        return getLinkDelayForPseudo(graph, link.getDestination().getDestNode(), link.getDestination().getDestTp(),
                                     link.getSource().getSourceNode(), link.getSource().getSourceTp());
    }

    public static Long getLinkDelayForPseudo(
            Graph<NodeId, Link> graph, NodeId sourceNode, TpId sourceTp,
            NodeId destNode, TpId destTp) {

        if (!graph.containsVertex(sourceNode) || !graph.containsVertex(destNode)) {
            return 0L;
        }

        AtomicLong delay = new AtomicLong(0);
        GraphCommonUtils.forEachOutEdge(graph, sourceNode, linkPs1 -> {
            if (sourceTp.equals(linkPs1.getSource().getSourceTp()) && ComUtility
                    .isDestPseudo(ComUtility.getLinkPseudo(linkPs1))) {
                GraphCommonUtils.forEachOutEdge(graph, destNode, linkPs2 -> {
                    if ((linkPs2.getDestination().getDestNode().equals(linkPs1.getDestination().getDestNode()))
                            && (linkPs2.getSource().getSourceTp().equals(destTp))) {
                        delay.set(TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkDelay(linkPs1));
                    }
                });
            }
        });

        return 0L;
    }

    public static boolean isCanBandWidthScaled() {
        return TopoServiceAdapter.getInstance().getPceTopoProvider().isCanBandWidthScaled();
    }

    public static long getReservedBwToTunnelInLink(TunnelUnifyKey tunnelKey, Link link, byte priority) {
        long curOccupyBwByTunnel = BandWidthMng.getInstance().queryTunnelOccupyBwInLink(tunnelKey, link);
        long reservedBw = BandWidthMng.getInstance().queryReservedBw(link, priority);
        return curOccupyBwByTunnel + reservedBw;
    }

    public static long calcPositiveDelay(List<Link> path) {
        if ((path == null) || (path.isEmpty())) {
            return ComUtility.DEFAULT_DELAY;
        }
        return path.stream().mapToLong(PceUtil::getLinkDelay).sum();
    }

    public static long calcReverseDelay(boolean isSimulate, List<Link> path, TopologyId topologyId) {
        if ((path == null) || (path.isEmpty())) {
            return ComUtility.DEFAULT_DELAY;
        }
        return path.stream().map(link -> ComUtility.getReverseLink4Path(getTopoGraph(isSimulate, topologyId), link))
                .mapToLong(PceUtil::getLinkDelay).sum();
    }

    public static Graph<NodeId, Link> getTopoGraph(boolean isSimulate, TopologyId topologyId) {
        return TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoGraph(isSimulate, topologyId);
    }

    public static PceComputeResult generatorPceComputeResult(
            TopologyId topoId, List<Link> path,
            TunnelUnifyKey tunnelUnifyKey, PceResult result) {
        PceComputeResult pceComputeResult;
        pceComputeResult = new PceComputeResult(topoId, path, tunnelUnifyKey);
        if (!CollectionUtils.isNullOrEmpty(result.getBandWidthScaleList())) {
            pceComputeResult.setBandWidthScaleList(result.getBandWidthScaleList());
        } else {
            pceComputeResult.setBandWidthScaleList(Collections.emptyList());
        }

        return pceComputeResult;
    }

    public static ThreadFactory getThreadFactory(String functionModule) {
        return TopoServiceAdapter.getInstance().getThreadFactory().generateThreadFactor(functionModule);
    }

    public static List<Segment> transformToSegments(List<PathSegment> pathSegments) {
        if (CollectionUtils.isNullOrEmpty(pathSegments)) {
            return Collections.emptyList();
        }
        return pathSegments.stream().map(PceUtil::transformToSegment).collect(Collectors.toList());
    }

    private static Segment transformToSegment(PathSegment pathSegment) {
        List<Link> segmentPath = Optional.ofNullable(pathSegment).map(PathSegment::getPath)
                .orElseGet(Collections::emptyList);
        return new SegmentBuilder().setPathLink(PceUtil.transform2PathLink(segmentPath)).build();
    }

    public static List<PathLink> transform2PathLink(List<Link> lsp) {
        LinkedList<PathLink> path = new LinkedList<>();
        if (lsp == null) {
            return path;
        }

        for (Link link : lsp) {
            path.addLast(new PathLinkBuilder(link).build());
        }

        return path;
    }

    public static List<PathSegment> transformFromSegment(
            List<Segment> segments, boolean isSimulate,
            TopologyId topologyId) {
        if (CollectionUtils.isNullOrEmpty(segments) || topologyId == null) {
            return Collections.emptyList();
        }
        List<PathSegment> pathSegments = segments.stream().filter(Objects::nonNull).map(Segment::getPathLink)
                .filter(pathLink -> !CollectionUtils.isNullOrEmpty(pathLink))
                .map(pathLink -> PcePathDb.getInstance().pathLinks2Links(topologyId, isSimulate, pathLink))
                .map(PathSegment::new).collect(Collectors.toList());
        if (pathSegments.size() < segments.size()) {
            Logs.error(LOG, "Errors occur when transformFromSegment {}", segments);
        }
        return pathSegments;
    }

    /**
     * createCalcStrategy.
     *
     * @param strategyContainer strategyContainer
     * @param isBidirect        isBidirect
     * @param topoId            topoId
     * @param <V>               node type
     * @param <E>               edge type
     * @return ICalcStrategy
     */
    public static <V, E> ICalcStrategy<V, E> createCalcStrategy(
            CalculateStrategyContainer strategyContainer,
            boolean isBidirect, @Nonnull TopologyId topoId) {
        final Optional<StrategyType> strategyType =
                Optional.ofNullable(strategyContainer).map(CalculateStrategyContainer::getStrategyType);
        if (!strategyType.isPresent() || strategyType.get() instanceof org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn
                .pce.path.rev150814.calculate.strategy.calculate.strategy.container.strategy.type.MetricStrategy) {
            return new MetricStrategy<>();
        } else {
            return new DelayStrategy<>(isBidirect, topoId);
        }
    }

    /**
     * createTransformerFactory.
     *
     * @param calcStrategy calcStrategy
     * @param <V>          node type
     * @param <E>          edge type
     * @return ITransformerFactory
     */
    public static <V, E> ITransformerFactory createTransformerFactory(@Nonnull ICalcStrategy<V, E> calcStrategy) {
        if (calcStrategy instanceof MetricStrategy) {
            return new MetricTransformerFactory();
        } else {
            return new DelayTransformerFactory();
        }
    }

    /**
     * createTransformerFactory.
     *
     * @param strategyContainer strategyContainer
     * @return ITransformerFactory
     */
    public static ITransformerFactory createTransformerFactory(
            @Nonnull CalculateStrategyContainer strategyContainer) {
        if (strategyContainer.getStrategyType()
                instanceof org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                .calculate.strategy.calculate.strategy.container.strategy.type.MetricStrategy) {
            return new MetricTransformerFactory();
        } else {
            return new DelayTransformerFactory();
        }
    }

    /* Utils for creating CalcStrategy and ITransformerFactory */

    public static boolean isBiDirectional(BiDirectContainer biDirect) {
        return Optional.ofNullable(biDirect).map(BiDirectContainer::getBidirectType)
                .map(bidirectType -> bidirectType instanceof Bidirectional).orElse(false);
    }

    public static void logTopoBandWidthInfo(Boolean isSimulate) {
        if (ComUtility.isSimulateTunnel(isSimulate)) {
            Logs.debug(LOG, BandWidthMng.getInstance().getSimulateBandWidthString());
        } else {
            Logs.debug(LOG, BandWidthMng.getInstance().getBandWidthString());
        }
    }

    public static void logLspPath(List<Link> lsp) {
        Logs.debug(LOG, "Lsp Path:{}", ComUtility.pathToString(lsp));
    }

    public static TeArgumentBean getNewTeArg(
            TeArgumentBean oldTeArg, TeArgument newTeAgCom,
            TeArgumentLsp teArgumentLsp, TopologyId topoId) {
        //there isn't priority and priority can't change.
        TeArgCommonData teArgComm = new TeArgCommonDataBuilder().setHoldPriority((short) oldTeArg.getHoldPriority())
                .setPreemptPriority((short) oldTeArg.getPreemptPriority()).setBandwidth(newTeAgCom.getBandwidth())
                .setMaxDelay(newTeAgCom.getMaxDelay()).build();

        TeArgumentBeanLsp teArgLsp = new TeArgumentBeanLsp(teArgumentLsp, topoId);

        return new TeArgumentBean(teArgComm, teArgLsp);
    }

    public static TeArgumentBean generatorSlaveTeArg(TeArgumentBean masterTeArg, TeArgumentBeanLsp slaveTeArg) {
        TeArgumentBean teArgumentBean = new TeArgumentBean(masterTeArg.getArgComm(), slaveTeArg);
        teArgumentBean.setForceCalcPathWithBandwidth(masterTeArg.isForceCalcPathWithBandwidth());
        teArgumentBean.setComputeLspWithBandWidth(masterTeArg.isComputeLspWithBandWidth());

        if (!teArgumentBean.isComputeLspWithBandWidth()) {
            teArgumentBean.setBandWidth(0);
        }
        return teArgumentBean;
    }

    public static boolean isHsbSrlgOverlap(SrlgAttribute masterSrlgAttr, SrlgAttribute slaveSrlgAttr) {
        if (masterSrlgAttr == null || masterSrlgAttr.getSrlgs().isEmpty()) {
            return false;
        }
        if (slaveSrlgAttr == null || slaveSrlgAttr.getSrlgs().isEmpty()) {
            return false;
        }
        List<Srlg> masterSrlgs = new LinkedList<>(masterSrlgAttr.getSrlgs());
        List<Srlg> slaveSrlgs = new LinkedList<>(slaveSrlgAttr.getSrlgs());
        masterSrlgs.retainAll(slaveSrlgs);
        return !masterSrlgs.isEmpty();
    }

    public static LspAttributes calcLspAttributes(List<Link> path) {
        final LspAttributes lspAttributes = new LspAttributes();
        if (CollectionUtils.isNullOrEmpty(path)) {
            return lspAttributes;
        }
        long lspMetric = 0;
        long lspDelay = 0;
        SrlgAttribute srlgAttribute = new SrlgAttribute();
        for (Link link : path) {
            lspMetric += PceUtil.getLinkMetric(link);
            lspDelay += PceUtil.getLinkDelay(link);
            srlgAttribute.mergeToAvoidSrlg(PceUtil.getLinkSrlgAttr(link));
        }
        lspAttributes.setLspMetric(lspMetric);
        lspAttributes.setLspDelay(lspDelay);
        lspAttributes.setSrlgAttr(srlgAttribute);
        return lspAttributes;
    }

    public static Double getLinkMetric(Link link) {
        return TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkMetric(link);
    }

    public static Long getLinkDelay(Link link) {
        return TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkDelay(link);
    }

    public static SrlgAttribute getLinkSrlgAttr(Link link) {
        return TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkSrlgAttr(link);
    }

    public static void clearSimulateMap() {
        TopoServiceAdapter.getInstance().getPceTopoProvider().clearSimulateMap();
    }

    public static void printTunnelDebugInfo(String desc, Logger logger, List<ITunnel> tunnelList) {
        for (ITunnel tunnel : tunnelList) {
            Logs.debug(logger, "{}\n {} bandwidth={} delay={} isChangeToZeroBandWidth={} isUnestablished={} "
                               + "isSrlgOverlap={} isPathOverlap={} "
                               + "isDelayRestricted={} isDelayEligible={}\n", desc, tunnel,
                       tunnel.getTeArgumentBean().getBandWidth(), tunnel.getTeArgumentBean().getMaxDelay(),
                       tunnel.isChangeToZeroBandWidth(), tunnel.isUnestablished(), tunnel.isSrlgOverlap(),
                       tunnel.isPathOverlap(), tunnel.isDelayRestricted(), tunnel.isDelayEligible());
        }
    }
}
