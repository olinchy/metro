/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.pathcore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.uci.ics.jung.graph.Graph;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.te.argument.lsp.NextAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.algorithm.ConstrainedOptimalPathAlgorithm;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input.ConstrainedOptimalPathInput;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.topology.MaintenanceTopologyMng;

import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Conditions;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.PortKey;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.api.vtelink.BandWidthScale;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.ISpt;
import com.zte.ngip.ipsdn.pce.path.core.LspGetPath;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.result.Path;
import com.zte.ngip.ipsdn.pce.path.core.strategy.ICalcStrategy;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformer;

public class ContrainedOptimalPath implements ISpt<NodeId, Link>, ConstrainedOptimalPathAlgorithm<NodeId, Link> {
    /**
     * Constructor.
     *
     * @param srcNodeId      srcNodeId
     * @param destNodeId     destNodeId
     * @param topoGraph      topoGraph
     * @param tunnelUnifyKey tunnelUnifyKey
     * @param strategy       strategy
     */
    public ContrainedOptimalPath(
            NodeId srcNodeId, NodeId destNodeId,
            Graph<NodeId, Link> topoGraph, TunnelUnifyKey tunnelUnifyKey,
            ICalcStrategy<NodeId, Link> strategy) {
        this.srcNodeId = srcNodeId;
        this.destNodeId = destNodeId;
        this.topoGraph = topoGraph;
        this.tunnelUnifyKey = tunnelUnifyKey;
        this.strategy = strategy;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ContrainedOptimalPath.class);
    Set<PortKey> crankBackExcludedPorts = new HashSet<>();
    byte crankBackTime = 0;
    Iterator<NextAddress> nextIterator;
    boolean conditionTriggeredVteLinkFlag = false;
    private NodeId srcNodeId;
    private NodeId destNodeId;
    private Graph<NodeId, Link> topoGraph;
    private Boolean descendFlag = false;
    private ITransformer<Link> edgeMetric;
    private TeArgumentBean teArg;
    private TunnelUnifyKey tunnelUnifyKey;
    private LinkedList<Link> path = new LinkedList<>();
    private BiDirect biDirect;
    private List<Link> excludePath = Lists.newArrayList();
    private ICalcStrategy<NodeId, Link> strategy;
    private Map<NodeId, List<Link>> incomingEdgeMap = new HashMap<>();
    private BwSharedGroupContainer bwSharedGroups;
    private BwSharedGroupContainer deletedBwSharedGroups;
    private List<BandWidthScale> bandWidthScaleList = new ArrayList<>();
    private boolean isNeedBandScaled;
    private CalcFailType failReason = CalcFailType.Other;

    private static NextAddress getNextLooseNode(
            Iterator<NextAddress> nextIterator,
            LinkedList<NextAddress> strictNext) {
        if (nextIterator == null) {
            return null;
        }

        while (nextIterator.hasNext()) {
            NextAddress nextAddress = nextIterator.next();
            if (isLooseNode(nextAddress)) {
                if ((nextAddress.getDestination() != null) && (nextAddress.getDestination().getDestNode() != null)) {
                    return nextAddress;
                }
            } else {
                strictNext.add(nextAddress);
            }
        }

        return null;
    }

    private static CalcFailType updateFailReason(
            final ContrainedOptimalPathWithoutLooseNext cspf,
            final NodeId tmpDestNodeId, final CalcFailType historyReason) {
        CalcFailType newReason = cspf.getFailReason(tmpDestNodeId);
        if (historyReason == CalcFailType.NoEnoughBandwidth) {
            return newReason == CalcFailType.NoPath ? newReason : CalcFailType.NoEnoughBandwidth;
        } else {
            return newReason;
        }
    }

    private static LinkedList<Link> getLooseNodeOldPath(NodeId looseNode, LinkedList<Link> oldPathRest) {
        boolean isFound = false;
        LinkedList<Link> looseNodeOldPath = new LinkedList<>();

        if (oldPathRest == null || oldPathRest.isEmpty()) {
            return new LinkedList<>();
        }

        Iterator<Link> iterator = oldPathRest.iterator();

        while (iterator.hasNext()) {
            Link link = iterator.next();
            looseNodeOldPath.addLast(link);
            if (looseNode.equals(link.getDestination().getDestNode())) {
                isFound = true;
                iterator.remove();
                break;
            }
            iterator.remove();
        }

        if (isFound) {
            return looseNodeOldPath;
        } else {
            looseNodeOldPath.clear();
            oldPathRest.clear();
            return new LinkedList<>();
        }
    }

    private static long getTmpDelay(Long tmpDelay, Long pathTempDelay) {
        return tmpDelay == ComUtility.INVALID_DELAY ? ComUtility.INVALID_DELAY : tmpDelay - pathTempDelay;
    }

    private static boolean isLooseNode(NextAddress nextAddress) {
        return (null == nextAddress.isStrict()) || (null != nextAddress.isStrict() && (!nextAddress.isStrict()));
    }

    private static boolean hasDestNode(NextAddress nextAddress) {
        if ((nextAddress.getDestination() == null) || (nextAddress.getDestination().getDestNode()
                == null)) {
            return false;
        }
        return true;
    }

    public void setEdgeMetric(ITransformer<Link> edgeMetric) {
        this.edgeMetric = edgeMetric;
    }

    /**
     * setTeArgument.
     *
     * @param teArg teArg
     */
    public void setTeArgument(TeArgumentBean teArg) {
        this.teArg = teArg;
        this.nextIterator = (this.teArg != null && this.teArg.getNextAddress() != null)
                ? this.teArg.getNextAddress().iterator() : null;
    }

    public void setBiDirect(BiDirect biDirect) {
        this.biDirect = biDirect;
    }

    public void setExcludePath(List<Link> excludePath) {
        this.excludePath = excludePath;
    }

    public void setBwSharedGroups(BwSharedGroupContainer groups, BwSharedGroupContainer deletedGroups) {
        this.bwSharedGroups = groups;
        this.deletedBwSharedGroups = deletedGroups;
    }

    @Override
    public Path<Link> calcConstrainedOptimalPath(ConstrainedOptimalPathInput<NodeId, Link> constrainedInput) {
        PathResult pathResult = new PathResult();
        pathResult.setPath(calcCspt(constrainedInput.getOldPath()));
        pathResult.getPceResult().setBandWidthScaleList(bandWidthScaleList);
        pathResult.getPceResult().setFailReason(getFailReason());
        return pathResult;
    }

    private List<Link> calcCspt(List<Link> oldPath) {
        NodeId tmpSrcNodeId = srcNodeId;
        Long tmpDelay = teArg.getMaxDelay();
        Long tmpReverseDelay = teArg.getMaxDelay();
        conditionTriggeredVteLinkFlag = teArg.getConditionTriggeredVteLinkFlag(topoGraph);

        //As current loose node is calculated,the rest loose nodes and dest node should be excluded.
        Set<NodeId> restExcludedNodes = new HashSet<>();

        getAllLooseNode(restExcludedNodes);
        restExcludedNodes.add(destNodeId);

        LinkedList<Link> oldPathRest = new LinkedList<>();
        Conditions.ifTrue(!CollectionUtils.isNullOrEmpty(oldPath), () -> oldPathRest.addAll(oldPath));

        //split path by loosenodes, and calculate the splited paths lonely
        while (true) {
            Long pathTempDelay = 0L;
            Long reversePathTmpDelay = 0L;
            LinkedList<NextAddress> strictNext = Lists.newLinkedList();
            NextAddress nextLooseNode = getNextLooseNode(nextIterator, strictNext);
            TpId looseNodeInport = Optional.ofNullable(nextLooseNode)
                    .map(nextLoose -> nextLoose.getDestination().getDestTp()).orElse(null);
            NodeId tmpDestNodeId = Optional.ofNullable(nextLooseNode)
                    .map(nextLoose -> nextLoose.getDestination().getDestNode()).orElse(destNodeId);

            restExcludedNodes.remove(tmpDestNodeId);

            ContrainedOptimalPathWithoutLooseNext cspf = calcPath(tmpSrcNodeId, tmpDestNodeId, strictNext,
                                                                  restExcludedNodes, looseNodeInport, tmpDelay,
                                                                  tmpReverseDelay);

            failReason = updateFailReason(cspf, tmpDestNodeId, failReason);

            Map<NodeId, List<Link>> incomingMap = cspf.getIncomingEdgeMap();
            if (!incomingMap.containsKey(tmpDestNodeId)) {
                tmpSrcNodeId = crankBack(nextLooseNode, tmpDestNodeId, restExcludedNodes);
                if (tmpSrcNodeId == null) {
                    bandWidthScaleList.clear();
                    return new LinkedList<>();
                }
            } else {

                descendFlag = cspf.getDescendFlag(tmpDestNodeId);
                LinkedList<Link> looseNodeOldPath = getLooseNodeOldPath(tmpDestNodeId, oldPathRest);
                List<Link> pathTemp = LspGetPath.getPath(incomingMap, tmpSrcNodeId, tmpDestNodeId, looseNodeOldPath);
                if (CollectionUtils.isNullOrEmpty(pathTemp)) {
                    path.clear();
                    bandWidthScaleList.clear();
                    return new LinkedList<>();
                }
                for (Link link : pathTemp) {
                    pathTempDelay += TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkDelay(link);
                    reversePathTmpDelay += cspf.getReverseLinkDelay(link);
                }

                path.addAll(pathTemp);
                bandWidthScaleList.addAll(cspf.getBandWidthScaleList());

                if (tmpDestNodeId.equals(destNodeId)) {
                    break;
                }

                tmpSrcNodeId = tmpDestNodeId;
                tmpDelay = getTmpDelay(tmpDelay, pathTempDelay);
                tmpReverseDelay = getTmpReverseDelay(tmpReverseDelay, reversePathTmpDelay);
            }
        }
        bandWidthScaleList =
                bandWidthScaleList.stream().filter(bandWidthScale -> path.contains(bandWidthScale.getLink()))
                        .collect(Collectors.toList());
        return path;
    }

    public CalcFailType getFailReason() {
        return failReason;
    }

    private void getAllLooseNode(Set<NodeId> allLooseNodes) {
        if (allLooseNodes == null) {
            return;
        }
        if (teArg != null && teArg.getNextAddress() != null) {
            for (NextAddress nextAddress : teArg.getNextAddress()) {
                if (isLooseNode(nextAddress) && hasDestNode(nextAddress)) {
                    allLooseNodes.add(nextAddress.getDestination().getDestNode());
                }
            }
        }
    }

    private ContrainedOptimalPathWithoutLooseNext calcPath(
            NodeId srcNodeId, NodeId destNodeId,
            List<NextAddress> strictNext,
            Set<NodeId> restExcludedNodes,
            TpId looseNodeInport, Long delay, Long reverseDelay) {
        List<NodeId> destNodeList = Lists.newArrayList();
        destNodeList.add(destNodeId);

        ContrainedOptimalPathWithoutLooseNext cspf =
                new ContrainedOptimalPathWithoutLooseNext(tunnelUnifyKey, srcNodeId, topoGraph, strategy);

        cspf.setDescendFlagMap(srcNodeId, descendFlag);

        cspf.setDestNodeList(destNodeList);
        cspf.setEdgeMeasure(edgeMetric);
        cspf.setBiDirect(biDirect);
        cspf.setBwSharedGroups(bwSharedGroups, deletedBwSharedGroups);
        cspf.setCanAddConditionTriggeredVteLink(conditionTriggeredVteLinkFlag);
        cspf.setNeedBandScaled(isNeedBandScaled);
        if (looseNodeInport != null) {
            cspf.setDestNodeInport(new PortKey(destNodeId, looseNodeInport));
        }
        addExcludePath2Cspf(cspf);

        if (teArg != null) {
            if (restExcludedNodes != null) {
                cspf.addExcludedNodes(restExcludedNodes);
            }

            for (Link link : path) {
                cspf.addExcludedNode(link.getSource().getSourceNode());
            }

            crankBackExcludedPortsAdd2cspf(cspf, destNodeId);

            cspf.addExcludedNodes(teArg.getExcludedNodes());
            cspf.addExcludedPorts(teArg.getExcludedPorts());
            /*add the global valid excludingAddresses */
            cspf.addExcludedNodes(MaintenanceTopologyMng.getInstance().getExcludingNodes(
                    Optional.ofNullable(tunnelUnifyKey).map(TunnelUnifyKey::isSimulate).orElse(false)));
            cspf.addExcludedPorts(MaintenanceTopologyMng.getInstance().getExcludingPorts(
                    Optional.ofNullable(tunnelUnifyKey).map(TunnelUnifyKey::isSimulate).orElse(false)));
            cspf.setBandwidth(teArg.getBandWidth(), teArg.getPreemptPriority(), teArg.getHoldPriority());
            cspf.setMaxDelay(delay);
            cspf.setReverseMaxDelay(reverseDelay);
            cspf.setStrictNextAddresses(strictNext);
            cspf.setAffinityStrategy(teArg.getAffinityStrategy());
        }
        cspf.calcSpt();

        return cspf;
    }

    private NodeId crankBack(
            NextAddress currentLooseNode, NodeId tmpDestNodeId,
            Set<NodeId> restExcludedNodes) {
        NodeId crankBackSrcNodeId;

        if (!isNeedCrankBack()) {
            return null;
        }

        PortKey inPort = new PortKey(
                path.getLast().getDestination().getDestNode(),
                path.getLast().getDestination().getDestTp());

        crankBackExcludedPorts.add(inPort);

        NextAddress lastLooseNode = crankBackLooseNode(currentLooseNode);
        if (lastLooseNode == null) {
            crankBackSrcNodeId = srcNodeId;
            nextIterator = resetNextIterator();
        } else {
            if (lastLooseNode.getDestination() == null) {
                return null;
            }
            crankBackSrcNodeId = lastLooseNode.getDestination().getDestNode();
            if (crankBackSrcNodeId == null) {
                Logs.error(LOG, "crankBack lastLooseNode error! srcNodeId {} TunnelId {}", srcNodeId,
                           tunnelUnifyKey.getTunnelId());
            } else {
                nextIterator = setNextIterator(lastLooseNode);
            }
        }

        crackBackPath(crankBackSrcNodeId);

        restExcludedNodes.add(tmpDestNodeId);

        crankBackTime++;
        if (crankBackTime == 100) {
            Logs.error(LOG, "crankBackTime too more! srcNodeId {} TunnelId {} tmpDestNodeId {}",
                       srcNodeId, tunnelUnifyKey.getTunnelId(), tmpDestNodeId);
            return null;
        }
        return crankBackSrcNodeId;
    }

    private Long getTmpReverseDelay(Long tmpReverseDelay, Long reversePathTmpDelay) {
        if (biDirect != null) {
            return getTmpDelay(tmpReverseDelay, reversePathTmpDelay);
        }
        return tmpReverseDelay;
    }

    private void addExcludePath2Cspf(ContrainedOptimalPathWithoutLooseNext cspf) {
        if (!excludePath.isEmpty()) {
            Set<NodeId> excludedNodes = new HashSet<>();
            Set<PortKey> excludedPorts = new HashSet<>();
            NodeId destNode;

            for (Link link : excludePath) {
                destNode = link.getDestination().getDestNode();
                if (link.getSource() != null) {
                    excludedPorts.add(new PortKey(link.getSource().getSourceNode(), link.getSource().getSourceTp()));
                    excludedPorts.add(new PortKey(destNode, link.getDestination().getDestTp()));
                } else {
                    excludedNodes.add(destNode);
                }
            }

            if (!excludedNodes.isEmpty()) {
                cspf.addExcludedNodes(excludedNodes);
            }
            cspf.addExcludedPorts(excludedPorts);
        }
    }

    private void crankBackExcludedPortsAdd2cspf(ContrainedOptimalPathWithoutLooseNext cspf, NodeId destNodeId) {
        if (!crankBackExcludedPorts.isEmpty()) {
            Iterator<PortKey> crankBacknextIterator = crankBackExcludedPorts.iterator();
            while (crankBacknextIterator.hasNext()) {
                PortKey port = crankBacknextIterator.next();
                if (port.getNode().equals(destNodeId)) {
                    cspf.addExcludedPort(port);
                }
            }
        }
    }

    private boolean isNeedCrankBack() {
        if ((path == null) || path.isEmpty()) {
            return false;
        }

        if ((teArg == null) || (teArg.getNextAddress() == null)
                || (teArg.getNextAddress().isEmpty())) {
            Logs.error(LOG, "isNeedCrankBack teArg error! srcNodeId {} TunnelId {}", srcNodeId,
                       tunnelUnifyKey.getTunnelId());
            return false;
        }
        return true;
    }

    private NextAddress crankBackLooseNode(NextAddress currentLooseNode) {
        NextAddress previousLooseNode;

        if ((teArg == null)
                || (teArg.getNextAddress() == null)
                || (teArg.getNextAddress().isEmpty())) {
            return null;
        }

        if (currentLooseNode != null) {
            previousLooseNode = getPreviousLooseNode(currentLooseNode);
        } else {
            //the last one
            previousLooseNode = getLastLooseNode();
        }

        return getPreviousLooseNode(previousLooseNode);
    }

    private Iterator<NextAddress> resetNextIterator() {
        return teArg.getNextAddress().listIterator(0);
    }

    private Iterator<NextAddress> setNextIterator(NextAddress looseNode) {
        int index = teArg.getNextAddress().indexOf(looseNode);
        if (index >= teArg.getNextAddress().size() - 1) {
            return null;
        }
        return teArg.getNextAddress().listIterator(index + 1);
    }

    private void crackBackPath(NodeId srcNode) {
        int pathLinkIndex = path.size();
        while (pathLinkIndex > 0) {
            pathLinkIndex--;
            if (srcNode.equals(path.get(pathLinkIndex).getDestination().getDestNode())) {
                return;
            }
            path.remove(pathLinkIndex);
        }
    }

    private NextAddress getPreviousLooseNode(NextAddress currentLooseNode) {

        int index = teArg.getNextAddress().indexOf(currentLooseNode);

        while (true) {
            index--;
            if (index < 0) {
                return null;
            }
            NextAddress nextAddress = teArg.getNextAddress().get(index);
            if ((null != nextAddress.isStrict()) && (!nextAddress.isStrict())) {
                return nextAddress;
            }
        }
    }

    private NextAddress getLastLooseNode() {
        if ((teArg == null) || (teArg.getNextAddress() == null)
                || (teArg.getNextAddress().isEmpty())) {
            return null;
        }

        return teArg.getNextAddress().get(teArg.getNextAddress().size() - 1);
    }

    @Override
    public Map<NodeId, List<Link>> getIncomingEdgeMap() {
        return incomingEdgeMap;
    }

    @Override
    public Map<NodeId, Number> getDistanceMap() {
        return null;
    }

    @Override
    public List<NodeId> getDistanceOrderList() {
        return new LinkedList<>();
    }

    public List<BandWidthScale> getBandWidthScaleList() {
        return bandWidthScaleList;
    }

    public ContrainedOptimalPath setNeedBandScaled(boolean needBandScaled) {
        isNeedBandScaled = needBandScaled;
        return this;
    }
}
