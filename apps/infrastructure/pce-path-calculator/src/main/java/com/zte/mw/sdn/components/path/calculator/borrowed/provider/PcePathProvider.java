/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import edu.uci.ics.jung.graph.Graph;
import org.apache.mina.util.ConcurrentHashSet;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.AdjustStrategy;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.AdjustTunnelBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcCalendarTunnelInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateSlaveTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateSlaveTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelGroupPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelGroupPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelHsbPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelHsbPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllTunnelThroughLinkInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllTunnelThroughLinkOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllTunnelThroughLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllTunnelThroughPortInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllTunnelThroughPortOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllTunnelThroughPortOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllUnreservedBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllUnreservedBandwidthOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllUnreservedBandwidthOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetMaxAvailableBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetMaxAvailableBandwidthOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetMaxAvailableBandwidthOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GlobalOptimizationInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.MigrateTopologyIdInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.PcePathService;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.QueryFailReasonInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.QueryFailReasonOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.QueryFailReasonOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.QueryTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.QueryTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.QueryTunnelPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.RefreshAllBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.RefreshAllBandwidthOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.RefreshAllBandwidthOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.RemoveTunnelGroupPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.RemoveTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.SetMaintenanceNodesInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelGroupPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelGroupPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbWithoutRollbackInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbWithoutRollbackOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathWithoutRollbackInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathWithoutRollbackOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelTopoidInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelTopoidOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelTopoidOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.adjust.tunnel.bandwidth.input.PathAdjustRequest;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.tunnel.through.link.output.TunnelGroupIdList;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.tunnel.through.link.output.TunnelGroupIdListBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.tunnel.through.link.output.TunnelIdList;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.tunnel.through.link.output.TunnelIdListBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.tunnel.through.port.input.PortKeyInfo;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.tunnel.through.port.output.ResultTunnelInfo;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.tunnel.through.port.output.ResultTunnelInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.tunnel.through.port.output.result.tunnel.info.TunnelGroupId;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.tunnel.through.port.output.result.tunnel.info.TunnelGroupIdBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.tunnel.through.port.output.result.tunnel.info.TunnelId;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.tunnel.through.port.output.result.tunnel.info.TunnelIdBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.unreserved.bandwidth.output.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.all.unreserved.bandwidth.output.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.max.available.bandwidth.output.MaxAvailableBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.query.tunnel.path.output.MasterLspPathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.query.tunnel.path.output.SlaveLspPathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.refresh.all.bandwidth.input.RefreshTunnel;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.set.maintenance.nodes.input.MaintenanceNode;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.DiffServBw;
import com.zte.mw.sdn.components.path.calculator.borrowed.calendartunnel.CalendarTunnelMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.globaloptimization.GlobalOptimizer;
import com.zte.mw.sdn.components.path.calculator.borrowed.maxbandwidthpath.MaxBandwidthPath;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.PathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelsRecordPerPort;
import com.zte.mw.sdn.components.path.calculator.borrowed.realtimepath.RealtimeDispatcher;
import com.zte.mw.sdn.components.path.calculator.borrowed.servicepath.ServiceHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.servicepath.ServicePathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.topology.MaintenanceTopologyMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.topology.TunnelsRecordPerTopology;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.TunnelGroupPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.TunnelGroupPathKey;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelhsbpath.TunnelHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.ITunnel;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathKey;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.RpcInputChecker;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.RpcReturnUtils;

import com.zte.ngip.ipsdn.pce.path.api.RefreshTarget;
import com.zte.ngip.ipsdn.pce.path.api.graph.GraphCommonUtils;
import com.zte.ngip.ipsdn.pce.path.api.segmentrouting.LabelEncodingService;
import com.zte.ngip.ipsdn.pce.path.api.segmentrouting.PathSegment;
import com.zte.ngip.ipsdn.pce.path.api.segmentrouting.SrResult;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Conditions;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.PortKey;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.strategy.ICalcStrategy;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformerFactory;
import com.zte.ngip.ipsdn.pce.path.core.transformer.MetricTransformer;

public class PcePathProvider implements PcePathService {
    private PcePathProvider() {
        pathHolder = PcePathHolder.getInstance();
        refreshHandler = new PcePathRefreshHandler(pathHolder);
        tunnelPathHandler = new TunnelPathHandler(pathHolder);
        tunnelHsbPathHandler = new TunnelHsbPathHandler(pathHolder, refreshHandler);
        tunnelGroupPathHandler = new TunnelGroupPathHandler(pathHolder);
        pceGlobalProvider = new PceGlobalProvider();
    }

    static final String CREATE_PATH_UNSUCCESSFULLY = "The path hasnot been created!";
    private static final Logger LOG = LoggerFactory.getLogger(PcePathProvider.class);
    private static final String ILLEGAL_ARGUMENT = "Illegal argument";
    private static PcePathProvider instance = new PcePathProvider();
    private static boolean isTunnelDispatchBandwidth = true;
    private PcePathHolder pathHolder;
    private PcePathRefreshHandler refreshHandler;
    private TunnelPathHandler tunnelPathHandler;
    private TunnelHsbPathHandler tunnelHsbPathHandler;
    private TunnelGroupPathHandler tunnelGroupPathHandler;
    private LabelEncodingService labelEncodingService;
    private PceGlobalProvider pceGlobalProvider;

    public static boolean isTunnelDispatchBandwidth() {
        return PcePathProvider.isTunnelDispatchBandwidth;
    }

    public static void setTunnelDispatchBandwidth(boolean isTunnelDispatchBandwidth) {
        PcePathProvider.isTunnelDispatchBandwidth = isTunnelDispatchBandwidth;
    }

    public static PcePathProvider getInstance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    private static List<UnreservedBandwidth> getUnreservedBandwidthList(String topoid, boolean isSimulate) {
        List<UnreservedBandwidth> unreservedBandwidthList = new ArrayList<>();
        if (!topoid.equals(ComUtility.DEFAULT_TOPO_ID_STRING)) {
            return unreservedBandwidthList;
        }

        TopologyId topologyId = new TopologyId(topoid);
        Graph<NodeId, Link> graph =
                TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoGraph(isSimulate, topologyId);
        if (graph != null) {
            GraphCommonUtils.forEachEdge(graph, link -> {
                long reservedBw = BandWidthMng.getInstance().queryLinkReservedBw(isSimulate, link);

                UnreservedBandwidth pathLinkBw = new UnreservedBandwidthBuilder()
                        .setDstNodeId(link.getDestination().getDestNode())
                        .setDstTpId(link.getDestination().getDestTp())
                        .setSrcNodeId(link.getSource().getSourceNode())
                        .setSrcTpId(link.getSource().getSourceTp())
                        .setBandwidth(BigInteger.valueOf(reservedBw))
                        .build();
                unreservedBandwidthList.add(pathLinkBw);
            });
        }

        return unreservedBandwidthList;
    }

    /**
     * recoveryDb.
     */
    public void recoveryDb() {
        pceGlobalProvider.pceGlobalDbRecovery();
        tnnlPathDbRecovery();
        tnnlGroupDbRecovery();
        tnnlHsbDbRecovery();
        pathHolder.serviceRecovery();
        pceGlobalProvider.registerDataTreeChangeListener();
    }

    /**
     * tnnlPathDbRecovery.
     */
    public void tnnlPathDbRecovery() {
        try {
            pathHolder.tnnlPathDbRecovery();
        } catch (InterruptedException | ExecutionException e) {
            Logs.debug(LOG, "tnnlPathDbRecovery read failed " + e);
        }
    }

    /**
     * tnnlGroupDbRecovery.
     */
    public void tnnlGroupDbRecovery() {
        try {
            pathHolder.tnnlGroupDbRecovery();
        } catch (InterruptedException | ExecutionException e) {
            Logs.debug(LOG, "tnnlGroupDbRecovery read db failed " + e);
        }
    }

    /**
     * tnnlHsbDbRecovery.
     */
    public void tnnlHsbDbRecovery() {
        try {
            pathHolder.tnnlHsbDbRecovery();
        } catch (InterruptedException | ExecutionException e) {
            Logs.debug(LOG, "tnnlHsbDbRecovery read db failed " + e);
        }
    }

    public void destroy() {
        pathHolder.destroy();
        pceGlobalProvider.pceSetBandScaleGlobalFlag(true);
    }

    public boolean getZeroBandWidthFlag() {
        return pceGlobalProvider.isCanZeroBandWidth();
    }

    public void setZeroBandWidthFlag(boolean flag) {
        pceGlobalProvider.setCanZeroBandWidth(flag);
    }

    /**
     * printTunnelInfo.
     *
     * @param headNodeId headNodeId
     * @param tunnelId   tunnelId
     */
    public void printTunnelInfo(NodeId headNodeId, int tunnelId) {
        pathHolder.printTunnelInfo(headNodeId, tunnelId);
    }

    /**
     * printAllTunnelInfo.
     */
    public void printAllTunnelInfo() {
        pathHolder.printAllTunnelInfo();
    }

    /**
     * printTunnelGroupInfo.
     *
     * @param headNodeId headNodeId
     * @param tunnelId   tunnelId
     */
    public void printTunnelGroupInfo(NodeId headNodeId, int tunnelId) {
        pathHolder.printTunnelGroupInfo(headNodeId, tunnelId);
    }

    /**
     * printAllTunnelGroupInfo.
     */
    public void printAllTunnelGroupInfo() {
        pathHolder.printAllTunnelGroupInfo();
    }

    /**
     * printTunnelHsbInfo.
     *
     * @param headNodeId headNodeId
     * @param tunnelId   tunnelId
     */
    public void printTunnelHsbInfo(NodeId headNodeId, int tunnelId) {
        pathHolder.printTunnelHsbInfo(headNodeId, tunnelId);
    }

    /**
     * printAllTunnelHsbInfo.
     */
    public void printAllTunnelHsbInfo() {
        pathHolder.printAllTunnelHsbInfo();
    }

    /**
     * update normal tunnel.
     *
     * @param tunnel tunnel path instance
     */
    public void updateTunnel(TunnelPathInstance tunnel) {
        pathHolder.updateTunnel(tunnel);
    }

    public void updateTunnel(ServiceHsbPathInstance service) {
        pathHolder.updateTunnel(service);
    }

    public void updateTunnel(ServicePathInstance service) {
        pathHolder.updateTunnel(service);
    }

    /**
     * update hsb tunnel instance.
     *
     * @param tunnel hsb tunnel
     */
    public void updateTunnel(TunnelHsbPathInstance tunnel) {
        pathHolder.updateTunnel(tunnel);
    }

    public void updateTunnel(TunnelGroupPathInstance tunnel) {
        pathHolder.putTunnelGroupPath(
                new TunnelGroupPathKey(tunnel.getHeadNodeId(), tunnel.getTunnelGroupId().intValue()), tunnel);
    }

    /**
     * getTunnelPathInstance.
     *
     * @param headNode   headNode
     * @param tunnelId   tunnelId
     * @param isSimulate isSimulate
     * @return TunnelPathInstance
     */
    public TunnelPathInstance getTunnelPathInstance(NodeId headNode, int tunnelId, Boolean isSimulate) {
        return pathHolder.getTunnelPathInstance(headNode, tunnelId, isSimulate);
    }

    public void setLabelEncodingService(LabelEncodingService service) {
        this.labelEncodingService = service;
    }

    /**
     * calcSegments.
     *
     * @param isSrTunnel     isSrTunnel
     * @param srp            srp
     * @param topologyId     topologyId
     * @param tunnelUnifyKey tunnelUnifyKey
     * @return segments
     */
    public List<PathSegment> calcSegments(
            boolean isSrTunnel, List<Link> srp, TopologyId topologyId,
            TunnelUnifyKey tunnelUnifyKey) {
        if (!isSrTunnel) {
            return Collections.emptyList();
        }
        if (labelEncodingService == null) {
            Logs.error(LOG, "labelEncodingService is unavailable");
            return Collections.emptyList();
        }
        Logs.info(LOG, "calcSegments begin tunnelUnifyKey={} path={}", tunnelUnifyKey, ComUtility.pathToString(srp));
        SrResult srResult =
                labelEncodingService.calcSegments2(srp, topologyId, ComUtility.getSimulateFlag(tunnelUnifyKey));
        pathHolder.updateTunnelSegmentsAffectedByLinkCache(srResult.getUsedLinks(), tunnelUnifyKey);
        Logs.info(LOG, "calcSegments end segments={} tunnelUnifyKey={}", srResult.getPathSegments(), tunnelUnifyKey);
        return srResult.getPathSegments();
    }

    public void refreshTunnelPath(TunnelUnifyKey tunnelPathKey, TopologyId topoId) {
        refreshHandler.refreshTunnelPath(tunnelPathKey, topoId);
    }

    public void refreshAllTunnels(TopologyId topoId) {
        refreshHandler.refreshAllTunnels(false, topoId);
    }

    public void refreshUnestablishAndSrlgTunnels(TopologyId topoId, TunnelUnifyKey sourceTunnel, byte sourcePriority) {
        refreshHandler.refreshUnestablishAndSrlgTunnels(false, topoId, sourceTunnel, sourcePriority);
    }

    public void refreshTunnels(
            boolean isSimulate, List<TunnelUnifyKey> migrateTunnels,
            Set<RefreshTarget> refreshTargets, TopologyId topoId) {
        refreshHandler.refreshTunnels(isSimulate, migrateTunnels, refreshTargets, topoId);
    }

    public void refreshTunnelsOnLink(boolean isSimulate, Link link, Graph<NodeId, Link> graph) {
        refreshHandler.refreshTunnelsOnLink(isSimulate, link, graph, null);
    }

    public void refreshTunnelsOnLink(
            boolean isSimulate, Link link, Graph<NodeId, Link> graph,
            Set<RefreshTarget> states) {
        refreshHandler.refreshTunnelsOnLink(isSimulate, link, graph, states);
    }

    private ResultTunnelInfo getResultTunnelInfo(PortKeyInfo portKeyInfo) {
        if (portKeyInfo == null) {
            return null;
        }
        Set<TunnelUnifyKey> tunnelSet;
        if (portKeyInfo.getTpId() != null) {
            tunnelSet = getPositiveTunnelsRecord(
                    new PortKey(portKeyInfo.getNodeId(), portKeyInfo.getTpId()),
                    portKeyInfo.isSimulateTunnel());
        } else {
            tunnelSet = new ConcurrentHashSet<>();
            List<Link> outLinks = TopoServiceAdapter.getInstance().getPceTopoProvider()
                    .getLinksViaNode(portKeyInfo.getNodeId(), null);
            tunnelSet.addAll(outLinks.stream().map(PortKey::new)
                                     .flatMap(portKey -> getPositiveTunnelsRecord(
                                             portKey,
                                             portKeyInfo.isSimulateTunnel()).stream())
                                     .collect(Collectors.toSet()));
        }
        List<TunnelId> tunnelList = new ArrayList<>();
        List<TunnelGroupId> tgList = new ArrayList<>();
        Map<Boolean, List<TunnelUnifyKey>> partition =
                tunnelSet.stream().collect(Collectors.partitioningBy(TunnelUnifyKey::isTg));
        tgList.addAll(partition.get(true).stream()
                              .map(key -> new TunnelGroupIdBuilder().setHeadNodeId(key.getHeadNode())
                                      .setTunnelGroupId((long) key.getTgId()).setIsMaster(key.isMaster()).build())
                              .collect(Collectors.toList()));
        tunnelList.addAll(partition.get(false).stream()
                                  .map(key -> new TunnelIdBuilder().setHeadNodeId(key.getHeadNode()).setTunnelId(
                                          (long) key.getTunnelId())
                                          .setIsMaster(key.isMaster()).setIsReverse(key.isReverse()).build())
                                  .collect(Collectors.toList()));
        return new ResultTunnelInfoBuilder(portKeyInfo).setTunnelId(tunnelList).setTunnelGroupId(tgList).build();
    }

    /**
     * always return the positive tunnel id and head node.
     * when a tunnel has two records( master and slave), this method only return one record.
     *
     * @param portKey    portKey
     * @param isSimulate isSimulate
     * @return a set of TunnelUnifyKey
     */
    private Set<TunnelUnifyKey> getPositiveTunnelsRecord(PortKey portKey, Boolean isSimulate) {
        Set<TunnelUnifyKey> tunnelsWithReverse =
                TunnelsRecordPerPort.getInstance().getTunnelsRecord(portKey, isSimulate);
        if (tunnelsWithReverse == null) {
            return Collections.emptySet();
        }
        Set<TunnelUnifyKey> positiveTunnelRecords = new ConcurrentHashSet<>();
        for (TunnelUnifyKey tunnelKey : tunnelsWithReverse) {

            java.util.Optional<ITunnel> bidirectTunnel = java.util.Optional.ofNullable(getTunnelInstance(tunnelKey));
            boolean isBidirectBindingReverse =
                    bidirectTunnel.map(ITunnel::getBiDirect).map(BiDirect::isReverse).orElse(false);

            if (isBidirectBindingReverse) {
                long positiveId = bidirectTunnel.map(ITunnel::getBiDirect).map(BiDirect::getReverseId).orElse(-1L);
                bidirectTunnel.map(ITunnel::getTailNode)
                        .map(tailNode -> new TunnelUnifyKey(tunnelKey, tailNode, positiveId, true, false))
                        .ifPresent(positiveTunnelKey -> {
                            if (getTunnelInstance(positiveTunnelKey) != null) {
                                positiveTunnelRecords.add(positiveTunnelKey);
                            } else {
                                LOG.error("get positive tunnel[Id={}, headNode={}] null! tunnel Id={} ", positiveId,
                                          positiveTunnelKey.getHeadNode(), tunnelKey.getId());
                            }
                        });
            } else {
                positiveTunnelRecords.add(new TunnelUnifyKey(tunnelKey, true, false));
            }
        }
        return positiveTunnelRecords;
    }

    /**
     * get tunnel instance br tunnel key.
     *
     * @param tunnelPathKey tunnelPathKey
     * @return ITunnel
     */
    public ITunnel getTunnelInstance(TunnelUnifyKey tunnelPathKey) {
        return pathHolder.getTunnelInstance(tunnelPathKey);
    }

    public TunnelGroupPathInstance getTunnelGroupInstance(NodeId headNode, int tgId) {
        return pathHolder.getTunnelGroupInstance(headNode, tgId);
    }

    public TunnelHsbPathInstance getTunnelHsbInstance(NodeId headNode, int tunnelId, boolean isSimulate) {
        return pathHolder.getTunnelHsbInstance(headNode, tunnelId, isSimulate);
    }

    /**
     * getTunnelHsbInstanceByKey.
     *
     * @param headNode   tunnel head id
     * @param tunnelId   tunnel id
     * @param isSimulate is simulate
     * @return TunnelHsbPathInstance
     */
    public TunnelHsbPathInstance getTunnelHsbInstanceByKey(NodeId headNode, int tunnelId, Boolean isSimulate) {
        return pathHolder.getTunnelHsbInstanceByKey(headNode, tunnelId, isSimulate);
    }

    @Override
    public Future<RpcResult<UpdateTunnelHsbWithoutRollbackOutput>> updateTunnelHsbWithoutRollback(
            UpdateTunnelHsbWithoutRollbackInput input) {

        Logs.debug(LOG, input.toString());

        return tunnelHsbPathHandler.updateWithoutRollback(input);
    }

    @Override
    public Future<RpcResult<Void>> removeTunnelPath(RemoveTunnelPathInput input) {
        if (Conditions.anyOneIsNull(input.getHeadNodeId(), input.getTunnelId())) {
            Logs.error(LOG, "removeTunnelPath failed! headNode {} tunnelId {}", input.getHeadNodeId(),
                       input.getTunnelId());
            return RpcReturnUtils.returnErr(ILLEGAL_ARGUMENT);
        }
        Logs.debug(LOG, input.toString());

        TunnelPathInstance tunnelPath = pathHolder
                .getTunnelPathInstance(input.getHeadNodeId(), input.getTunnelId().intValue(), input.isSimulateTunnel());
        TunnelHsbPathInstance tunnelHsbPathInstance = pathHolder
                .getTunnelHsbInstanceByKey(input.getHeadNodeId(), input.getTunnelId().intValue(),
                                           input.isSimulateTunnel());

        if (tunnelPath != null) {
            pathHolder.deleteReverseTunnel(tunnelPath);
            tunnelPath.destroy();
            pathHolder.removeTunnelPathInstance(tunnelPath);
        } else if (tunnelHsbPathInstance != null) {
            pathHolder.deleteReverseTunnel(tunnelHsbPathInstance);
            tunnelHsbPathInstance.destroy();
            pathHolder.removeTunnelHsbPathInstance(tunnelHsbPathInstance);

            Logs.debug(LOG, BandWidthMng.getInstance().getBandWidthString());
        }

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<Void>> removeTunnelGroupPath(RemoveTunnelGroupPathInput input) {
        if (input.getHeadNodeId() != null && input.getTunnelGroupId() != null) {
            LOG.debug(input.toString());
            TunnelGroupPathKey key = new TunnelGroupPathKey(input.getHeadNodeId(), input.getTunnelGroupId().intValue());
            TunnelGroupPathInstance tg = pathHolder.getTunnelGroupInstance(key);
            if (tg != null) {
                tg.destroy();
                pathHolder.removeTunnelGroupPath(key);
                tg.removeDb();
            }
        }

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<Void>> adjustTunnelBandwidth(AdjustTunnelBandwidthInput input) {
        if (!RpcInputChecker.check(input)) {
            Logs.error(LOG, "adjustTunnelBandwidth failed! {}", RpcReturnUtils.ILLEGAL_ARGUMENT);
            return RpcReturnUtils.returnErr(RpcReturnUtils.ILLEGAL_ARGUMENT);
        }
        Logs.debug(LOG, TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());

        boolean isFail =
                input.getPathAdjustRequest().stream().map(this::adjustTunnelBandwidth).anyMatch(result -> !result);
        Logs.debug(LOG, TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());
        return isFail
                ? RpcReturnUtils.returnErr("At least one tunnel fail") :
                Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<GetAllUnreservedBandwidthOutput>> getAllUnreservedBandwidth(
            GetAllUnreservedBandwidthInput input) {
        String topoid = java.util.Optional.ofNullable(input).map(GetAllUnreservedBandwidthInput::getTopoid)
                .orElse(ComUtility.DEFAULT_TOPO_ID_STRING);
        boolean isSimulate = ComUtility.isSimulateTunnel(input.isSimulateTunnel());
        List<UnreservedBandwidth> unreservedBandwidthList = getUnreservedBandwidthList(topoid, isSimulate);
        GetAllUnreservedBandwidthOutput output = new GetAllUnreservedBandwidthOutputBuilder()
                .setUnreservedBandwidth(unreservedBandwidthList)
                .build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<UpdateTunnelHsbPathOutput>> updateTunnelHsbPath(
            UpdateTunnelHsbPathInput input) {
        if (!RpcInputChecker.check(input)) {
            Logs.error(LOG, "updateTunnelHsbPath failed! {}", ILLEGAL_ARGUMENT);
            return RpcReturnUtils.returnErr(ILLEGAL_ARGUMENT);
        }
        LOG.debug(input.toString());
        LOG.debug(TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());

        return tunnelHsbPathHandler.update(input);
    }

    @Override
    public Future<RpcResult<UpdateTunnelGroupPathOutput>> updateTunnelGroupPath(UpdateTunnelGroupPathInput input) {
        Logs.debug(LOG, input.toString());

        return tunnelGroupPathHandler.update(input);
    }

    @Override
    public Future<RpcResult<UpdateTunnelPathWithoutRollbackOutput>> updateTunnelPathWithoutRollback(
            UpdateTunnelPathWithoutRollbackInput input) {

        Logs.debug(LOG, input.toString());

        return tunnelPathHandler.updateWithoutRollback(input);
    }

    @Override
    public Future<RpcResult<Void>> setMaintenanceNodes(SetMaintenanceNodesInput input) {
        if (input == null) {
            return RpcReturnUtils.returnErr("SetMaintenanceNodesInput is null");
        }
        LOG.debug(input.toString());
        Set<NodeId> excludingNodes = new HashSet<>();
        Set<PortKey> excludingPorts = new HashSet<>();
        List<MaintenanceNode> maintenanceNodes = input.getMaintenanceNode();
        if (maintenanceNodes == null) {
            maintenanceNodes = Collections.emptyList();
        }
        for (MaintenanceNode node : maintenanceNodes) {
            if (node.getTpId() == null) {
                java.util.Optional.ofNullable(node.getNodeId()).ifPresent(excludingNodes::add);
            } else {
                if (node.getNodeId() == null) {
                    LOG.error("setMaintenanceNodes error! nodeId is null, but tpId {} is not null", node.getTpId());
                    continue;
                }
                excludingPorts.add(new PortKey(node.getNodeId(), node.getTpId()));
            }
        }
        boolean isSimulate = java.util.Optional.ofNullable(input.isSimulateTunnel()).orElse(false);
        MaintenanceTopologyMng.getInstance().setAllExcludingAddresses(excludingNodes, excludingPorts, isSimulate);
        LOG.debug(
                "current maintenance nodes:{}\n ports:{}",
                MaintenanceTopologyMng.getInstance().getExcludingNodes(isSimulate).toString(),
                MaintenanceTopologyMng.getInstance().getExcludingPorts(isSimulate));
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<CreateTunnelPathOutput>> createTunnelPath(CreateTunnelPathInput input) {

        if (!RpcInputChecker.check(input)) {
            Logs.error(LOG, "createTunnelPath failed! {}", ILLEGAL_ARGUMENT);
            return RpcReturnUtils.returnErr(ILLEGAL_ARGUMENT);
        }

        Logs.debug(LOG, input.toString());
        Logs.debug(LOG, TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());
        PceUtil.logTopoBandWidthInfo(input.isSimulateTunnel());

        return tunnelPathHandler.create(input);
    }

    @Override
    public Future<RpcResult<CreateTunnelGroupPathOutput>> createTunnelGroupPath(CreateTunnelGroupPathInput input) {
        if (input.getHeadNodeId() == null || input.getTailNodeId() == null) {
            return RpcReturnUtils.returnErr(ILLEGAL_ARGUMENT);
        }
        Logs.debug(LOG, input.toString());

        return tunnelGroupPathHandler.create(input);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Future<RpcResult<QueryFailReasonOutput>> queryFailReason(QueryFailReasonInput input) {

        final NodeId headNodeId = input.getHeadNodeId();

        final TunnelUnifyKey tunnelKey = new TunnelUnifyKey(headNodeId, 0);
        tunnelKey.setQueryFailReason(true);
        tunnelKey.setSimulateFlag(input.isSimulateTunnel() != null ? input.isSimulateTunnel() : false);

        final NodeId tailNodeId = input.getTailNodeId();
        final TopologyId topoId = input.getTopologyId() != null ? input.getTopologyId() :
                TopologyId.getDefaultInstance(ComUtility.DEFAULT_TOPO_ID_STRING);
        final ICalcStrategy<NodeId, Link> strategy =
                PceUtil.createCalcStrategy(input.getCalculateStrategyContainer(), input.getBiDirectContainer() != null,
                                           topoId);
        final ITransformerFactory factory = PceUtil.createTransformerFactory(strategy);

        PathProvider<MetricTransformer> pathProvider =
                new PathProvider<>(headNodeId, tunnelKey, tailNodeId, topoId, strategy, factory);
        pathProvider.setTeArg(new TeArgumentBean(input, topoId));
        pathProvider.setIsRealTimePath(true);

        PceResult result = new PceResult();
        pathProvider.calcPath(result);

        QueryFailReasonOutput output = new QueryFailReasonOutputBuilder()
                .setFailReason(result.getFailReason())
                .build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<UpdateTunnelPathOutput>> updateTunnelPath(
            UpdateTunnelPathInput input) {
        if (!RpcInputChecker.check(input)) {
            Logs.error(LOG, "updateTunnelPath failed! {}", ILLEGAL_ARGUMENT);
            return RpcReturnUtils.returnErr(ILLEGAL_ARGUMENT);
        }
        Logs.debug(LOG, input.toString());
        Logs.debug(LOG, TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());
        PceUtil.logTopoBandWidthInfo(input.isSimulateTunnel());

        return tunnelPathHandler.update(input);
    }

    @Override
    public Future<RpcResult<Void>> migrateTopologyId(
            MigrateTopologyIdInput input) {
        if (input.getFromTopologyId() == null
                || input.getToTopologyId() == null) {
            return RpcReturnUtils.returnErr(ILLEGAL_ARGUMENT);
        }

        TunnelsRecordPerTopology.getInstance().migrateTopologyId(
                input.getFromTopologyId(),
                input.getToTopologyId());

        return RpcReturnUtils.returnOk();
    }

    @Override
    public Future<RpcResult<GetMaxAvailableBandwidthOutput>> getMaxAvailableBandwidth(
            GetMaxAvailableBandwidthInput input) {
        if (!RpcInputChecker.check(input)) {
            return RpcReturnUtils.returnErr(ILLEGAL_ARGUMENT);
        }

        MaxBandwidthPath path = new MaxBandwidthPath(input);
        path.calcPath();

        GetMaxAvailableBandwidthOutput output = new GetMaxAvailableBandwidthOutputBuilder()
                .setMaxAvailableBandwidth(new MaxAvailableBandwidthBuilder()
                                                  .setBandwidth(new BigInteger(
                                                          Long.toString(path.getMaxBandwidthToCreateTunnel())))
                                                  .setPathLink(PceUtil.transform2PathLink(path.getLsp()))
                                                  .build())
                .build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<Void>> calcCalendarTunnel(CalcCalendarTunnelInput input) {
        LOG.info("calcCalendarTunnel input {}", input);
        if (input.isOnOffFlag() == null) {
            return RpcReturnUtils.returnErr("Should set on off flag!");
        }
        if (input.isOnOffFlag()) {
            CalendarTunnelMng.getInstance().mirrorAllResource();
        } else {
            CalendarTunnelMng.getInstance().clearAllMirrorResource();
        }
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<CreateTunnelHsbPathOutput>> createTunnelHsbPath(
            CreateTunnelHsbPathInput input) {
        if (!RpcInputChecker.check(input)) {
            Logs.error(LOG, "createTunnelHsbPath failed! {}", ILLEGAL_ARGUMENT);
            return RpcReturnUtils.returnErr(ILLEGAL_ARGUMENT);
        }
        LOG.debug(input.toString());
        LOG.debug(TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());
        PceUtil.logTopoBandWidthInfo(input.isSimulateTunnel());

        return tunnelHsbPathHandler.create(input);
    }

    @Override
    public synchronized Future<RpcResult<RefreshAllBandwidthOutput>> refreshAllBandwidth(
            RefreshAllBandwidthInput input) {
        Logs.debug(LOG, input.toString());

        List<RefreshTunnel> tunnels = input.getRefreshTunnel();
        if (CollectionUtils.isNullOrEmpty(tunnels)) {
            return RpcReturnUtils.returnErr("No tunnels need refresh!");
        }

        List<TopologyId> topos = new LinkedList<>();
        tunnels.forEach(tunnelInfo -> {
            if (Conditions.anyOneIsNull(tunnelInfo.getTunnelId(), tunnelInfo.getHeadNodeId(),
                                        tunnelInfo.getBandwidth())) {
                Logs.error(LOG, "tunnelInfo error! head:{} tunnelid:{} bw:{}", tunnelInfo.getHeadNodeId(),
                           tunnelInfo.getTunnelId(), tunnelInfo.getBandwidth());
            } else {
                ITunnel tunnel = tryToGetTunnel(tunnelInfo.getHeadNodeId(), tunnelInfo.getTunnelId().intValue());

                if (tunnel == null) {
                    Logs.warn(LOG, "tunnel null head:{} tunnelid:{}", tunnelInfo.getHeadNodeId(),
                              tunnelInfo.getTunnelId());
                } else {
                    tunnel.decreaseBandwidth(tunnelInfo.getBandwidth(), tunnelInfo.getBwSharedGroupContainer());
                    Conditions.ifTrue(!topos.contains(tunnel.getTopoId()), () -> topos.add(tunnel.getTopoId()));
                }
            }
        });

        topos.forEach(topo -> refreshUnestablishTunnels(false, topo, null, DiffServBw.HIGHEST_PRIORITY));
        RefreshAllBandwidthOutput output = new RefreshAllBandwidthOutputBuilder().build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<UpdateTunnelTopoidOutput>> updateTunnelTopoid(
            UpdateTunnelTopoidInput input) {
        LOG.debug(input.toString());

        TunnelPathKey key = new TunnelPathKey(input.getHeadNodeId(), input.getTunnelId().intValue());
        TunnelPathInstance path = pathHolder.getTunnelPathInstance(key);
        if (path == null) {
            return RpcReturnUtils.returnErr(CREATE_PATH_UNSUCCESSFULLY);
        }

        TopologyId oldTopoId = path.getTopoId();
        PceResult pceResult = path.updateTopoId(input.getTopologyId());
        path.writeDb();
        if (pceResult.isNeedRefreshUnestablishTunnels()) {
            PcePathProvider.getInstance()
                    .refreshUnestablishTunnels(false, oldTopoId, path.getTunnelUnifyKey(), path.getHoldPriority());
        }

        UpdateTunnelTopoidOutput output = new UpdateTunnelTopoidOutputBuilder()
                .setTunnelPath(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update.tunnel
                        .topoid.output.TunnelPathBuilder()
                                       .setPathLink(PceUtil.transform2PathLink(path.getLsp()))
                                       .setLspMetric(path.getLspMetric())
                                       .build())
                .build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<CreateSlaveTunnelPathOutput>> createSlaveTunnelPath(CreateSlaveTunnelPathInput input) {
        if (!RpcInputChecker.check(input)) {
            return RpcReturnUtils.returnErr(ILLEGAL_ARGUMENT);
        }
        Logs.debug(LOG, input.toString());

        return tunnelHsbPathHandler.createSlave(input);
    }

    @Override
    public Future<RpcResult<QueryTunnelPathOutput>> queryTunnelPath(QueryTunnelPathInput input) {
        TunnelPathKey key = new TunnelPathKey(input.getHeadNodeId(), input.getTunnelId().intValue());
        TunnelPathInstance tunnel = pathHolder.getTunnelPaths().get(key);
        TunnelHsbPathInstance msTunnelPathInstance = pathHolder.getTunnelHsbPaths().get(key);
        QueryTunnelPathOutputBuilder outputBuilder = new QueryTunnelPathOutputBuilder();
        if (null != tunnel) {
            outputBuilder.setMasterLspPath(
                    new MasterLspPathBuilder().setPathLink(PceUtil.transform2PathLink(tunnel.getLsp()))
                            .setLspMetric(tunnel.getLspMetric()).build());
        }
        if (null != msTunnelPathInstance) {
            outputBuilder.setMasterLspPath(new MasterLspPathBuilder()
                                                   .setPathLink(PceUtil.transform2PathLink(
                                                           msTunnelPathInstance.getMasterLsp()))
                                                   .setLspMetric(msTunnelPathInstance.getMasterMetric()).build());
            outputBuilder.setSlaveLspPath(new SlaveLspPathBuilder()
                                                  .setPathLink(PceUtil.transform2PathLink(
                                                          msTunnelPathInstance.getSlaveLsp()))
                                                  .setLspMetric(msTunnelPathInstance.getSlaveMetric()).build());
        }
        return Futures.immediateFuture(RpcResultBuilder.success(outputBuilder.build()).build());
    }

    @Override
    public Future<RpcResult<java.lang.Void>> globalOptimization(GlobalOptimizationInput input) {
        Logs.info(LOG, "globalOptimization input {}", input);
        Optional<AdjustStrategy> strategy =
                Optional.ofNullable(input).map(GlobalOptimizationInput::getLspAdjustStrategy);
        if (!strategy.isPresent()) {
            return RpcReturnUtils.returnErr("Adjust strategy should not be null!");
        }
        new GlobalOptimizer(this).optimizeAsync(strategy.get());
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<GetAllTunnelThroughLinkOutput>> getAllTunnelThroughLink(
            GetAllTunnelThroughLinkInput input) {
        if ((input == null) || (input.getSrcNodeId() == null || input.getDestNodeId() == null)) {
            return RpcReturnUtils.returnErr(ILLEGAL_ARGUMENT);
        }
        NodeId srcNode = input.getSrcNodeId();
        NodeId destNode = input.getDestNodeId();
        List<TunnelIdList> tunnelList = new ArrayList<>();
        List<TunnelGroupIdList> tgList = new ArrayList<>();
        Set<TunnelUnifyKey> tunnelSet =
                getTunnelsBetweenDirectConnectNodes(srcNode, destNode, input.isSimulateTunnel());
        if (!tunnelSet.isEmpty()) {
            for (TunnelUnifyKey tunnelUnifyKey : tunnelSet) {
                if (tunnelUnifyKey.isTg()) {
                    TunnelGroupIdList tgId = new TunnelGroupIdListBuilder()
                            .setHeadNodeId(tunnelUnifyKey.getHeadNode())
                            .setTunnelGroupId((long) tunnelUnifyKey.getTgId())
                            .setIsMaster(true)
                            .build();
                    tgList.add(tgId);
                } else {
                    TunnelIdList tunnelId = new TunnelIdListBuilder()
                            .setTunnelId((long) tunnelUnifyKey.getTunnelId())
                            .setHeadNodeId(tunnelUnifyKey.getHeadNode())
                            .setIsMaster(true)
                            .setIsReverse(false)
                            .build();
                    tunnelList.add(tunnelId);
                }
            }
        }

        GetAllTunnelThroughLinkOutput output = new GetAllTunnelThroughLinkOutputBuilder()
                .setTunnelIdList(tunnelList)
                .setTunnelGroupIdList(tgList)
                .build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<GetRealtimePathOutput>> getRealtimePath(
            GetRealtimePathInput input) {
        if (!RpcInputChecker.check(input)) {
            return RpcReturnUtils.returnErr(ILLEGAL_ARGUMENT);
        }

        LOG.debug(input.toString());
        LOG.debug(TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());
        PceUtil.logTopoBandWidthInfo(false);

        return RealtimeDispatcher.dispatch(input, pathHolder);
    }

    @Override
    public Future<RpcResult<GetAllTunnelThroughPortOutput>> getAllTunnelThroughPort(
            GetAllTunnelThroughPortInput input) {
        if (input == null || input.getPortKeyInfo() == null || input.getPortKeyInfo().isEmpty()) {
            return RpcReturnUtils.returnErr(ILLEGAL_ARGUMENT);
        }
        List<ResultTunnelInfo> lists =
                input.getPortKeyInfo().stream().map(this::getResultTunnelInfo).collect(Collectors.toList());

        GetAllTunnelThroughPortOutput output =
                new GetAllTunnelThroughPortOutputBuilder().setResultTunnelInfo(lists).build();
        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    private Set<TunnelUnifyKey> getTunnelsBetweenDirectConnectNodes(
            NodeId srcNode, NodeId destNode,
            Boolean isSimulate) {

        if ((null == srcNode) || (null == destNode)) {
            return Collections.emptySet();
        }

        List<Link> links = TopoServiceAdapter.getInstance().getPceTopoProvider().getLinksViaNode(srcNode, destNode);
        if (null == links) {
            return Collections.emptySet();
        }

        return links.stream().flatMap(link -> getPositiveTunnelsRecord(
                new PortKey(link.getSource().getSourceNode(), link.getSource().getSourceTp()), isSimulate).stream())
                .collect(Collectors.toSet());
    }

    public void refreshUnestablishTunnels(
            boolean isSimulate, TopologyId topoId, TunnelUnifyKey sourceTunnel,
            byte sourcePriority) {
        refreshHandler.refreshUnestablishTunnels(isSimulate, topoId, sourceTunnel, sourcePriority, null);
    }

    private ITunnel tryToGetTunnel(NodeId headNodeId, int tunnelId) {
        ITunnel tunnel = getTunnelInstance(headNodeId, tunnelId);
        return Optional.ofNullable(tunnel).orElseGet(() -> getTunnelHsbInstance(headNodeId, tunnelId));
    }

    public TunnelPathInstance getTunnelInstance(NodeId headNode, int tunnelId) {
        return getTunnelInstance(headNode, tunnelId, false);
    }

    public TunnelHsbPathInstance getTunnelHsbInstance(NodeId headNode, int tunnelId) {
        return pathHolder.getTunnelHsbInstance(headNode, tunnelId);
    }

    /**
     * getTunnelInstance.
     *
     * @param headNode   headNode
     * @param tunnelId   tunnelId
     * @param isSimulate isSimulate
     * @return TunnelPathInstance
     */
    public TunnelPathInstance getTunnelInstance(NodeId headNode, int tunnelId, boolean isSimulate) {
        return pathHolder.getTunnelInstance(headNode, tunnelId, isSimulate);
    }

    public void tunnelNumPrint() {
        pathHolder.tunnelNumPrint();
    }

    public Map<TunnelPathKey, TunnelPathInstance> getTunnelPaths() {
        return pathHolder.getTunnelPaths();
    }

    public Map<TunnelPathKey, TunnelHsbPathInstance> getTunnelHsbPaths() {
        return pathHolder.getTunnelHsbPaths();
    }

    public boolean pceGetBandScaleGlobalFlag() {
        return pceGlobalProvider.getGlobalBandScaleFlag();
    }

    private boolean adjustTunnelBandwidth(PathAdjustRequest pathAdjustRequest) {
        if (!RpcInputChecker.check(pathAdjustRequest)) {
            Logs.error(LOG, "adjustTunnelBandwidth failed! {}", RpcReturnUtils.ILLEGAL_ARGUMENT);
            return false;
        }
        Logs.info(LOG, "adjustTunnelBandwidth input {}", pathAdjustRequest);

        TunnelPathInstance path = pathHolder
                .getTunnelPathInstance(pathAdjustRequest.getHeadNodeId(), pathAdjustRequest.getTunnelId().intValue(),
                                       false);
        TunnelHsbPathInstance tunnelHsbPathInstance = pathHolder
                .getTunnelHsbInstanceByKey(pathAdjustRequest.getHeadNodeId(),
                                           pathAdjustRequest.getTunnelId().intValue(), false);
        PceResult pceResult = PceResult.nullPceResult;
        TopologyId topologyId = null;
        TunnelUnifyKey tunnelUnifyKey = null;
        byte holdPriority = 0;
        if (path != null) {
            pceResult = path.adjustBandWidth(pathAdjustRequest);
            topologyId = path.getTopoId();
            tunnelUnifyKey = path.getTunnelUnifyKey();
            holdPriority = path.getHoldPriority();
        }
        if (tunnelHsbPathInstance != null) {
            pceResult = tunnelHsbPathInstance.adjustBandWidth(pathAdjustRequest);
            topologyId = tunnelHsbPathInstance.getTopoId();
            tunnelUnifyKey = tunnelHsbPathInstance.getTunnelUnifyKey();
            holdPriority = tunnelHsbPathInstance.getHoldPriority();
        }
        if (pceResult.isNeedRefreshUnestablishTunnels()) {
            PcePathProvider.getInstance()
                    .refreshUnestablishTunnels(false, topologyId, tunnelUnifyKey, holdPriority);
        }
        if (CalcFailType.NoEnoughBandwidth.equals(pceResult.getCalcFailType())) {
            Logs.error(LOG, "adjustTunnelBandwidth failed! han no enough BW");
            return false;
        }
        pceResult.preemptedTunnelsProcess(true);
        Logs.debug(LOG, TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());
        return true;
    }
}




