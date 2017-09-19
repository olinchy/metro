/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.pathcore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import edu.uci.ics.jung.graph.Graph;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.affinity.AffinityStrategy;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.te.argument.lsp.NextAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroupMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.level.LevelProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.graph.GraphCommonUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Conditions;
import com.zte.ngip.ipsdn.pce.path.api.util.LinkUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.PortKey;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.api.vtelink.BandWidthScale;
import com.zte.ngip.ipsdn.pce.path.api.vtelink.VteLinkScaleOper;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.OptimalPath;
import com.zte.ngip.ipsdn.pce.path.core.strategy.ICalcStrategy;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformer;

public class ContrainedOptimalPathWithoutLooseNext extends OptimalPath<NodeId, Link> {
    /**
     * ContrainedOptimalPathWithoutLooseNext.
     *
     * @param tunnelUnifyKey tunnelUnifyKey
     * @param sourceNode     sourceNode
     * @param graph          graph
     * @param strategy       strategy
     */
    public ContrainedOptimalPathWithoutLooseNext(
            TunnelUnifyKey tunnelUnifyKey, NodeId sourceNode,
            Graph<NodeId, Link> graph, ICalcStrategy<NodeId, Link> strategy) {
        super(sourceNode, graph, strategy);
        setSourceData(new SourceDataExImpl(sourceNode));
        this.tunnelUnifyKey = tunnelUnifyKey;
        this.graph = graph;
        this.levelMap = LevelProvider.getInstance().getTopoLevel();
        this.descendFlagMap.put(sourceNode, false);
    }

    private static final Logger LOG = LoggerFactory.getLogger(ContrainedOptimalPathWithoutLooseNext.class);
    private TunnelUnifyKey tunnelUnifyKey;
    private Set<NodeId> excludedNodes = new HashSet<>();
    private Set<PortKey> excludedPorts = new HashSet<>();
    private List<NextAddress> strictNextAddresses;
    private Iterator<NextAddress> strictNextIterator;
    private NextAddress currentStrictNextAddress;
    private NextAddress nextNodeStrictNextAddress;
    private Iterator<NextAddress> nextNodestrictIterator;
    private long bandwidth;
    private long maxDelay;
    private long reverseMaxDelay;
    private byte preemptPriority;
    private byte holdPriority;
    private BiDirect biDirect;
    private PortKey destNodeInport;
    private Map<NodeId, Long> levelMap;
    private Map<NodeId, Boolean> descendFlagMap = new HashMap<>();
    private Graph<NodeId, Link> graph;
    private List<Link> bestMatchLink = new LinkedList<>();
    private List<Link> lessMatchLink = new LinkedList<>();
    private BwSharedGroupContainer bwSharedGroups;
    private BwSharedGroupContainer deletedBwSharedGroups;
    private boolean isCanAddConditionTriggeredVteLink = false;
    private List<BandWidthScale> bandWidthScaleList = new ArrayList<>();
    private boolean isNeedBandScaled;
    private AffinityStrategy affinityStrategy;

    void setDescendFlagMap(NodeId srcNodeId, Boolean descendFlagMap) {
        this.descendFlagMap.put(srcNodeId, descendFlagMap);
    }

    /**
     * getDescendFlag.
     *
     * @param tmpDestNodeId tmpDestNodeId
     * @return boolean
     */
    boolean getDescendFlag(NodeId tmpDestNodeId) {
        if (CollectionUtils.isNullOrEmpty(descendFlagMap) || !descendFlagMap.containsKey(tmpDestNodeId)) {
            return false;
        }
        return descendFlagMap.get(tmpDestNodeId);
    }

    @Override
    public void setEdgeMeasure(ITransformer<Link> edgeMetric) {
        this.edgeMeasure = edgeMetric;
    }

    public void setBiDirect(BiDirect biDirect) {
        this.biDirect = biDirect;
    }

    public void setDestNodeInport(PortKey destNodeInport) {
        this.destNodeInport = destNodeInport;
    }

    public void setMaxDelay(long maxDelay) {
        this.maxDelay = maxDelay;
    }

    public void setReverseMaxDelay(long maxDelay) {
        this.reverseMaxDelay = maxDelay;
    }

    public void setCanAddConditionTriggeredVteLink(boolean canAddConditionTriggeredVteLink) {
        isCanAddConditionTriggeredVteLink = canAddConditionTriggeredVteLink;
    }

    public void setAffinityStrategy(AffinityStrategy affinityStrategy) {
        this.affinityStrategy = affinityStrategy;
    }

    public Long getReverseLinkDelay(Link link) {
        if (biDirect != null) {
            return PceUtil.getLinkDelay(ComUtility.getReverseLink4Path(graph, link));
        }

        return 0L;
    }

    public void addExcludedNodes(Set<NodeId> excludedNodes) {
        this.excludedNodes.addAll(excludedNodes);
    }

    public void addExcludedNode(NodeId excludedNode) {
        this.excludedNodes.add(excludedNode);
    }

    public void addExcludedPorts(Set<PortKey> excludedPorts) {
        if ((excludedPorts == null) || excludedPorts.isEmpty()) {
            return;
        }
        this.excludedPorts.addAll(excludedPorts);
    }

    public void addExcludedPort(PortKey excludedPort) {
        if (excludedPort == null) {
            return;
        }
        this.excludedPorts.add(excludedPort);
    }

    public void setStrictNextAddresses(List<NextAddress> strictNextAddresses) {
        this.strictNextAddresses = strictNextAddresses;

        if (strictNextAddresses != null && !strictNextAddresses.isEmpty()) {
            strictNextIterator = strictNextAddresses.iterator();
            currentStrictNextAddress = strictNextIterator.next();
        }
    }

    public void setBandwidth(Long bandWidth, byte preemptPriority, byte holdPriority) {
        this.bandwidth = (bandWidth == null) ? 0 : bandWidth;
        this.preemptPriority = preemptPriority;
        this.holdPriority = holdPriority;
    }

    public void setBwSharedGroups(BwSharedGroupContainer groups, BwSharedGroupContainer deletedGroups) {
        this.bwSharedGroups = groups;
        this.deletedBwSharedGroups = deletedGroups;
    }

    /**
     * get fail reason for specific node.
     *
     * @param target the specific node
     * @return the fail reason
     */
    CalcFailType getFailReason(NodeId target) {
        return ((SourceDataExImpl) getSourceData()).getFailReason(target);
    }

    public List<BandWidthScale> getBandWidthScaleList() {
        return bandWidthScaleList;
    }

    public ContrainedOptimalPathWithoutLooseNext setNeedBandScaled(boolean needBandScaled) {
        isNeedBandScaled = needBandScaled;
        return this;
    }

    protected class SourceDataExImpl extends SourceDataImpl {
        public SourceDataExImpl(NodeId sourceNode) {
            super(sourceNode);
            tentVerifiedMap = new HashMap<>();
            pathVerifiedMap = new HashMap<>();
            pathVerifiedMap.put(sourceNode, new ConstraintsVerifyResult(true, true, true));
        }

        private Map<NodeId, ConstraintsVerifyResult> tentVerifiedMap;
        private Map<NodeId, ConstraintsVerifyResult> pathVerifiedMap;

        private boolean isMatchDestNodeButNotMatchDestTp(NodeId neighborNode, Link incomingEdge) {
            return isMatchDestNode(neighborNode) && !isMatchDestInport(incomingEdge);
        }

        private void updateVerifyResult(
                NodeId localNode, NodeId neighborNode, boolean isBandwidthEnough,
                boolean isDelayEligible) {
            boolean bwEnough = isBandwidthEnough && pathVerifiedMap.get(localNode).isBandwidthEnough();
            ConstraintsVerifyResult
                    verifyResult = new ConstraintsVerifyResult(true, bwEnough, isDelayEligible);
            if (tentVerifiedMap.get(neighborNode) == null || verifyResult
                    .isBetterThan(tentVerifiedMap.get(neighborNode))) {
                tentVerifiedMap.put(neighborNode, verifyResult);
            }
        }

        private boolean isPathValid(Link incomingEdge, NodeId neighborNode) {
            return Conditions.and(!isLinkExcluded(incomingEdge), isLinkMatchStrictNext(incomingEdge),
                                  !isLevelReverse(incomingEdge), TopoServiceAdapter.getInstance().getPceTopoProvider()
                                          .byPassCheck(incomingEdge, neighborNode, getDestNodeList(),
                                                       isCanAddConditionTriggeredVteLink),
                                  isLinkMatchAffinityStrategy(incomingEdge));
        }

        boolean isLinkExcluded(Link link) {
            return isTpExcluded(link.getSource().getSourceNode(), link.getSource().getSourceTp()) || isTpExcluded(
                    link.getDestination().getDestNode(), link.getDestination().getDestTp());
        }

        boolean isLinkMatchAffinityStrategy(Link link) {
            if (biDirect == null) {
                return LinkUtils.getAffinityAttributes(link).isMatchAffinityStrategy(affinityStrategy);
            } else {
                Link reverseLink = ComUtility.getReverseLink4Path(graph, link);
                if (reverseLink != null) {
                    return LinkUtils.getAffinityAttributes(reverseLink).isMatchAffinityStrategy(affinityStrategy);
                } else {
                    Logs.warn(LOG, "Reverse of {} is missing", link);
                    return false;
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void addBestEdge2TentDelay(NodeId localNode, NodeId neighborNode, Link incomingEdge) {
            if (maxDelay == ComUtility.INVALID_DELAY && reverseMaxDelay == ComUtility.INVALID_DELAY) {
                return;
            }

            //we choose positive least delay link to TentDelayMap
            long lessDelay = maxDelay;
            List<Link> temList = new LinkedList<>();
            List<Link> edgeList = tentIncomingEdgesMap.get(neighborNode);
            if (edgeList.contains(incomingEdge)) {
                for (Link edge : edgeList) {
                    long incomingEdgeDelay = PceUtil.getLinkDelay(edge);
                    NodeId preNode = edge.getSource().getSourceNode();

                    PathDelay preLocNodeDelay = (PathDelay) pathDelayMap.get(preNode);
                    long preNodePositiveDelay = preLocNodeDelay.getPostiveDelay();
                    if (incomingEdgeDelay + preNodePositiveDelay < lessDelay) {
                        lessDelay = incomingEdgeDelay + preNodePositiveDelay;
                        temList.clear();
                        temList.add(edge);
                    } else if (incomingEdgeDelay + preNodePositiveDelay == lessDelay) {
                        temList.add(edge);
                    }
                }
                if (!temList.isEmpty()) {
                    edgeList.clear();
                    edgeList.addAll(temList);
                }
                if (edgeList.contains(incomingEdge)) {
                    PathDelay locNodeDelay = (PathDelay) pathDelayMap.get(localNode);
                    long reverseLinkDelay = PceUtil.getReverseLinkDelay(graph, edgeList.get(0));
                    addTentDelay(neighborNode, lessDelay, locNodeDelay.getReverseDelay() + reverseLinkDelay);
                }
            }
        }

        private void addTentDelay(NodeId node, long delay, long reverseDelay) {
            PathDelay pathTentDelay = new PathDelay(delay, reverseDelay);

            tentDelayMap.put(node, pathTentDelay);
        }

        private boolean isLevelReverse(Link incomingEdge) {
            Boolean descendFlag = descendFlagMap.get(incomingEdge.getSource().getSourceNode());

            if (descendFlag == null || !descendFlag || levelMap == null) {
                return false;
            }

            Long localLevel = levelMap.get(incomingEdge.getSource().getSourceNode());
            Long neighborLevel = levelMap.get(incomingEdge.getDestination().getDestNode());

            if (localLevel == null || neighborLevel == null) {
                return false;
            }

            return localLevel < neighborLevel;
        }

        @Override
        public NodeId moveOptimalTentNode2PathList() {
            removeLessMatchLink();
            currentStrictNextAddress = nextNodeStrictNextAddress;
            strictNextIterator = nextNodestrictIterator;

            if (isQueryFailReason()) {
                Optional<Map.Entry<NodeId, ConstraintsVerifyResult>> optimalNode =
                        getOptimalNodeInTentMap(tentVerifiedMap);
                return optimalNode.map(entry -> {
                    NodeId nodeId = entry.getKey();
                    ConstraintsVerifyResult verifyResult = entry.getValue();
                    if (verifyResult.isBetterThan(pathVerifiedMap.get(nodeId))) {
                        pathVerifiedMap.put(nodeId, verifyResult);
                        tentVerifiedMap.remove(nodeId);
                    }
                    superMoveOptimalTentNode2PathList(nodeId, verifyResult.getScore());
                    return nodeId;
                }).orElse(null);
            }

            return super.moveOptimalTentNode2PathList();
        }

        @Override
        public void setDestNodeDescendFlag(List<Link> incomingEdges) {
            if (levelMap == null) {
                return;
            }

            NodeId src = incomingEdges.get(0).getSource().getSourceNode();
            NodeId dest = incomingEdges.get(0).getDestination().getDestNode();
            Boolean descendSrcFlag = descendFlagMap.get(src);
            if (descendSrcFlag != null && descendSrcFlag) {
                descendFlagMap.put(dest, true);
                return;
            }

            Long levelSrc = levelMap.get(src);
            Long levelDest = levelMap.get(dest);
            if (levelSrc != null && levelDest != null && levelDest < levelSrc) {
                descendFlagMap.put(dest, true);
            }
        }

        public void add2TentList(NodeId localNode, NodeId neighborNode, Link incomingEdge) {
            if (ComUtility.isDestPseudo(ComUtility.getLinkPseudo(incomingEdge))) {
                GraphCommonUtils.forEachInEdge(graph, neighborNode, linkTemp -> {
                    NodeId neighborNodeNew = linkTemp.getSource().getSourceNode();
                    boolean isNeighborNodeNewValid =
                            !neighborNodeNew.equals(localNode) && !isNodeAlreadyInPathList(neighborNodeNew);

                    Conditions.ifTrue(isNeighborNodeNewValid, () -> {
                        Link linkNew = ComUtility.newLinkFromPesudo(incomingEdge, linkTemp);
                        add2TentList(localNode, neighborNodeNew, linkNew);
                    });
                });
                return;
            }

            boolean isPathValid = isPathValid(incomingEdge, neighborNode);
            boolean isBandwidthEnough = hasEnoughBw(incomingEdge);
            boolean isDelayEligible = isDelayEligible(localNode, incomingEdge);

            if (!isPathValid || isMatchDestNodeButNotMatchDestTp(neighborNode, incomingEdge)) {
                removeBandWidthList(incomingEdge);
            } else {
                if (isQueryFailReason()) {
                    updateVerifyResult(localNode, neighborNode, isBandwidthEnough, isDelayEligible);
                } else if (!isBandwidthEnough || !isDelayEligible) {
                    removeBandWidthList(incomingEdge);
                    return;
                }

                super.add2TentList(localNode, neighborNode, incomingEdge);
                addBestEdge2TentDelay(localNode, neighborNode, incomingEdge);
            }
        }

        @Override
        public boolean isNodeAlreadyInPathList(NodeId node) {
            if (isQueryFailReason()) {
                return pathVerifiedMap.containsKey(node);
            }
            return super.isNodeAlreadyInPathList(node);
        }

        private void removeLessMatchLink() {
            if (currentStrictNextAddress == null || currentStrictNextAddress.isIpv4() == null
                    || !currentStrictNextAddress.isIpv4()) {
                return;
            }

            bestMatchLink.clear();

            if (lessMatchLink.isEmpty()) {
                return;
            }

            lessMatchLink.forEach(link -> removeTent(link, link.getDestination().getDestNode()));
            lessMatchLink.clear();
        }

        private boolean isQueryFailReason() {
            return tunnelUnifyKey != null && tunnelUnifyKey.isQueryFailReason();
        }

        private Optional<Map.Entry<NodeId, ConstraintsVerifyResult>> getOptimalNodeInTentMap(
                Map<NodeId, ConstraintsVerifyResult> tentMap) {
            return tentMap.entrySet().parallelStream().max(Comparator.comparing(Map.Entry::getValue));
        }

        @SuppressWarnings("unchecked")
        private void superMoveOptimalTentNode2PathList(final NodeId node, final Number distance) {

            final PathDelay pathDelay = (PathDelay) tentDelayMap.get(node);
            removeTentDistance(node);
            removeTentDelay(node);

            List<Link> incomingEdges = tentIncomingEdgesMap.get(node);
            tentIncomingEdgesMap.remove(node);

            setDestNodeDescendFlag(incomingEdges);

            pathDistanceMap.put(node, distance);
            pathDelayMap.put(node, pathDelay);
            pathIncomingEdgeMap.put(node, incomingEdges);
            distanceOrderList.add(node);
        }

        boolean isBwEnough(Link incomingEdge) {
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
                    .hasEnoughBw(incomingEdge, preemptPriority, holdPriority, bandwidth, tunnelUnifyKey,
                                 biDirect != null);
        }

        long bwNeeded(Link incomingEdge) {
            if (bwSharedGroups != null) {
                return BwSharedGroupMng.getInstance()
                        .bwNeedForTunnel(incomingEdge, preemptPriority, holdPriority, bwSharedGroups,
                                         deletedBwSharedGroups, tunnelUnifyKey.isSimulate());
            }

            return BandWidthMng.getInstance()
                    .bwNeedForTunnel(incomingEdge, preemptPriority, holdPriority, bandwidth, tunnelUnifyKey);
        }

        private boolean isCanBandWidthScale(Link incomingEdge, boolean isBwEnough) {
            if (TopoServiceAdapter.getInstance().getPceTopoProvider().isLinkCanBandWidthScaled(incomingEdge)
                    && isNeedScaleBandwidth(isBwEnough) && (tunnelUnifyKey == null || !tunnelUnifyKey.isSimulate())) {
                long bandWidthNeeded = bwNeeded(incomingEdge);
                if (bandWidthNeeded > 0) {
                    bandWidthScaleList.add(new BandWidthScale.BandWidthScaleBuild().setBandWidth(bandWidthNeeded)
                                                   .setLink(incomingEdge).setOperaType(
                                    VteLinkScaleOper.INCREASE).setBandWidthScaled(
                                    TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkBandWidth(incomingEdge)
                                            + bandWidthNeeded).build());
                }
                return true;
            }
            return isBwEnough;
        }

        private boolean isNeedScaleBandwidth(boolean isBwEnough) {
            return !isBwEnough && isNeedBandScaled;
        }

        boolean hasEnoughBw(Link incomingEdge) {
            boolean ret;
            if (bandwidth == 0) {
                return true;
            }
            ret = isBwEnough(incomingEdge);
            return isCanBandWidthScale(incomingEdge, ret);
        }

        void removeBandWidthList(Link incomingEdge) {
            Optional<BandWidthScale> bandWidthScale =
                    bandWidthScaleList.stream().filter(bandScale -> bandScale.getLink().equals(incomingEdge))
                            .findFirst();
            bandWidthScale.ifPresent(bandWidthScaleList::remove);
        }

        @SuppressWarnings("unchecked")
        boolean isDelayEligible(NodeId localNode, Link incomingEdge) {
            if (maxDelay == ComUtility.INVALID_DELAY && reverseMaxDelay == ComUtility.INVALID_DELAY) {
                return true;
            }

            PathDelay locNodeDelay = (PathDelay) pathDelayMap.get(localNode);
            Long incomingEdgeDelay = PceUtil.getLinkDelay(incomingEdge);
            Long reverseEdgeDelay = getReverseLinkDelay(incomingEdge);

            if (biDirect == null) {
                return locNodeDelay.getPostiveDelay() + incomingEdgeDelay <= maxDelay;
            }
            return (locNodeDelay.getPostiveDelay() + incomingEdgeDelay <= maxDelay) && (
                    locNodeDelay.getReverseDelay() + reverseEdgeDelay <= reverseMaxDelay);
        }

        private boolean isTpExcluded(NodeId node, TpId tp) {
            return (excludedNodes != null && excludedNodes.contains(node)) || (excludedPorts != null && excludedPorts
                    .contains(new PortKey(node, tp)));
        }

        private boolean isMatchDestNode(NodeId neighborNode) {
            return destNodeInport != null && neighborNode.equals(destNodeInport.getNode());
        }

        private boolean isMatchDestInport(Link link) {
            return link.getDestination().getDestTp().equals(destNodeInport.getTp());
        }

        boolean isLinkMatchStrictNext(Link link) {
            if (currentStrictNextAddress == null) {
                return true;
            }

            Iterator<NextAddress> iteratorThis =
                    strictNextAddresses.listIterator(strictNextAddresses.indexOf(currentStrictNextAddress));
            NextAddress nextAddressThis = iterateToNextOrNull(iteratorThis);

            return nextAddressThis == null || isMatch(link, nextAddressThis, iteratorThis);
        }

        private boolean isMatch(Link link, NextAddress nextAddressThis, Iterator<NextAddress> iteratorThis) {
            boolean passFlag = false;
            NextAddress nextAddressThisRef = nextAddressThis;
            if (isIpv4NextAddress(nextAddressThisRef)) {

                boolean curFlag;
                //check link outport or sourcenode
                for (int i = 0; i < 2; i++) {
                    curFlag = isSrcMatch(link, nextAddressThisRef);
                    if (curFlag) {
                        nextAddressThisRef = iterateToNextOrNull(iteratorThis);
                    }
                    passFlag = passFlag || curFlag;
                }

                //check link inport or destnode
                for (int i = 0; i < 2; i++) {
                    curFlag = isDstMatch(link, nextAddressThisRef);
                    if (curFlag) {
                        nextAddressThisRef = iterateToNextOrNull(iteratorThis);
                    }
                    passFlag = passFlag || curFlag;
                }
            } else {
                passFlag = isMatchNonIpv4NextAddress(link, nextAddressThisRef);
                nextAddressThisRef = iterateToNextOrNull(iteratorThis);
            }

            if (passFlag) {
                passFlag = whoIsBest(link, nextAddressThisRef, iteratorThis);
            }
            return passFlag;
        }

        private NextAddress iterateToNextOrNull(Iterator<NextAddress> iteratorThis) {
            return iteratorThis.hasNext() ? iteratorThis.next() : null;
        }

        private boolean isIpv4NextAddress(NextAddress nextAddressThis) {
            return nextAddressThis.isIpv4() != null && nextAddressThis.isIpv4();
        }

        private boolean isSrcMatch(Link link, NextAddress nextAddressThisRef) {
            boolean passFlag = false;
            if (isLinkSrcTpMatchNextAddressDstTp(link, nextAddressThisRef)) {
                passFlag = true;
            } else if (isLinkSrcNodeMatchNextAddressDstNode(link, nextAddressThisRef)) {
                passFlag = true;
            }
            return passFlag;
        }

        private boolean isDstMatch(Link link, NextAddress nextAddressThisRef) {
            boolean passFlag = false;
            if (isLinkDstTpMatchNextAddressDstTp(link, nextAddressThisRef)) {
                passFlag = true;
            } else if (isLinkDstNodeMatchNextAddressDstNode(link, nextAddressThisRef)) {
                passFlag = true;
            }
            return passFlag;
        }

        private boolean isMatchNonIpv4NextAddress(Link link, NextAddress nextAddressThis) {
            boolean result = true;

            if (Optional.ofNullable(nextAddressThis.getSource()).map(Source::getSourceNode)
                    .filter(srcNode -> !srcNode.equals(link.getSource().getSourceNode())).isPresent()) {
                result = false;
            } else if (Optional.ofNullable(nextAddressThis.getSource()).map(Source::getSourceTp)
                    .filter(srcTp -> !srcTp.equals(link.getSource().getSourceTp())).isPresent()) {
                result = false;
            } else if (Optional.ofNullable(nextAddressThis.getDestination()).map(Destination::getDestNode)
                    .filter(dstNode -> !dstNode.equals(link.getDestination().getDestNode())).isPresent()) {
                result = false;
            } else if (Optional.ofNullable(nextAddressThis.getDestination()).map(Destination::getDestTp)
                    .filter(dstTp -> !dstTp.equals(link.getDestination().getDestTp())).isPresent()) {
                result = false;
            }
            return result;
        }

        private boolean isLinkDstNodeMatchNextAddressDstNode(Link link, NextAddress nextAddressThis) {
            return nextAddressThis != null && !isNextAddressDestTpNotNull(nextAddressThis)
                    && isNextAddressNodeMatchDestNode(nextAddressThis, link);
        }

        private boolean isLinkDstTpMatchNextAddressDstTp(Link link, NextAddress nextAddressThis) {
            return nextAddressThis != null && isNextAddressDestTpNotNull(nextAddressThis)
                    && isNextAddressDestMatchLinkDest(nextAddressThis, link);
        }

        private boolean isLinkSrcNodeMatchNextAddressDstNode(Link link, NextAddress nextAddressThis) {
            return nextAddressThis != null && !isNextAddressDestTpNotNull(nextAddressThis)
                    && isNextAddressDestMatchSourceNode(nextAddressThis, link);
        }

        private boolean isLinkSrcTpMatchNextAddressDstTp(Link link, NextAddress nextAddressThis) {
            return nextAddressThis != null && isNextAddressDestTpNotNull(nextAddressThis)
                    && isNextAddressDestMatchLinkSource(nextAddressThis, link);
        }

        private boolean isNextAddressDestTpNotNull(NextAddress nextAddress) {
            return nextAddress.getDestination().getDestTp() != null;
        }

        private boolean isNextAddressDestMatchLinkSource(NextAddress nextAddress, Link link) {
            return nextAddress.getDestination().getDestTp().equals(link.getSource().getSourceTp()) && nextAddress
                    .getDestination().getDestNode().equals(link.getSource().getSourceNode());
        }

        private boolean isNextAddressDestMatchSourceNode(NextAddress nextAddress, Link link) {
            if (nextAddress.getDestination().getDestNode() != null) {
                return nextAddress.getDestination().getDestNode().equals(link.getSource().getSourceNode());
            }
            return false;
        }

        private boolean isNextAddressDestMatchLinkDest(NextAddress nextAddress, Link link) {

            return nextAddress.getDestination().getDestTp().equals(link.getDestination().getDestTp()) && nextAddress
                    .getDestination().getDestNode().equals(link.getDestination().getDestNode());
        }

        private boolean isNextAddressNodeMatchDestNode(NextAddress nextAddress, Link link) {
            if (nextAddress.getDestination().getDestNode() != null) {
                return nextAddress.getDestination().getDestNode().equals(link.getDestination().getDestNode());
            }
            return false;
        }

        /*
    *        R1
    *      /   \
    *     /     \
    *   R2-------R3
    *   head:R2 tail:R3 ipv4strictNextAddress:21.21.21.21
    *   after check the nextAddress,currentStrictNextAddress is null
    *   link of R2-R3 will be add to tent, then path will be R2-R3
    *   so add SourceNode R2 to excludedNodes4ipv4StrictNext.
    */
        private boolean whoIsBest(Link link, NextAddress nextAddressThis, Iterator iteratorThis) {
            int currentMatchIndex;
            int prevMatchIndex;
            boolean passFlag = true;

            if (nextAddressThis == null) {
                currentMatchIndex = strictNextAddresses.size();
            } else {
                currentMatchIndex = strictNextAddresses.indexOf(nextAddressThis);
            }
            if (nextNodeStrictNextAddress == null) {
                prevMatchIndex = -1;
            } else {
                prevMatchIndex = strictNextAddresses.indexOf(nextNodeStrictNextAddress);
            }

            if (prevMatchIndex < currentMatchIndex) {
                nextNodeStrictNextAddress = nextAddressThis;
                nextNodestrictIterator = iteratorThis;
                lessMatchLink.addAll(bestMatchLink);
                bestMatchLink.clear();
                bestMatchLink.add(link);
            } else if (prevMatchIndex == currentMatchIndex) {
                bestMatchLink.add(link);
            } else {
                passFlag = false;
            }

            return passFlag;
        }

        public CalcFailType getFailReason(NodeId target) {
            if (!pathVerifiedMap.containsKey(target) || pathVerifiedMap.get(target) == null) {
                return CalcFailType.NoPath;
            }
            boolean isBandwidthEnough = pathVerifiedMap.get(target).isBandwidthEnough();
            if (!isBandwidthEnough) {
                return CalcFailType.NoEnoughBandwidth;
            }
            boolean isDelayEligible = pathVerifiedMap.get(target).isDelayEligible();
            if (!isDelayEligible) {
                return CalcFailType.DelayIneligible;
            }
            return CalcFailType.Other;
        }
    }
}
