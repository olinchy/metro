/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input;

import java.util.ArrayList;
import java.util.List;

import edu.uci.ics.jung.graph.Graph;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.input.OptimalPathInput;
import com.zte.ngip.ipsdn.pce.path.core.input.OptimalPathInputBuilder;
import com.zte.ngip.ipsdn.pce.path.core.strategy.ICalcStrategy;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformer;

/**
 * Created by 10204924 on 2017/6/5.
 */
public class ConstrainedOptimalPathInputBuilder {
    public ConstrainedOptimalPathInputBuilder(ConstrainedOptimalPathInput<NodeId, Link> otherInput) {
        this();
        optimalPathInputBuilder.setTopologyId(otherInput.getTopologyId());
        optimalPathInputBuilder.setGraph(otherInput.getGraph());
        optimalPathInputBuilder.setCalcStrategy(otherInput.getCalcStrategy());
        optimalPathInputBuilder.setTransformer(otherInput.getTransformer());
        optimalPathInputBuilder.setHeadNodeId(otherInput.getHeadNodeId());
        optimalPathInputBuilder.setTailNodeId(otherInput.getTailNodeId());
        optimalPathInputBuilder.setSimulateFlag(ComUtility.getSimulateFlag(otherInput.getTunnelUnifyKey()));

        impl.optimalPathInput = optimalPathInputBuilder.build();
        impl.tunnelUnifyKey = otherInput.getTunnelUnifyKey();
        impl.teArgumentBean = otherInput.getTeArgumentBean();
        impl.oldPath = otherInput.getOldPath();
        impl.biDirect = otherInput.getBiDirect();
        impl.recalc = otherInput.isRecalc();
        impl.needScaleBandwidth = otherInput.isNeedScaleBandwidth();
        impl.bwSharedGroups = otherInput.getBwSharedGroups();
        impl.deletedBwSharedGroups = otherInput.getDeletedBwSharedGroups();
        impl.reqMaxDelay = otherInput.getReqMaxDelay();
        impl.masterPath = otherInput.getMasterPath();
        impl.excludedLinks = otherInput.getExcludedLinks();
        impl.isSimulate = otherInput.isSimulate();
    }

    public ConstrainedOptimalPathInputBuilder() {
        this.optimalPathInputBuilder = new OptimalPathInputBuilder();
        this.impl = new ConstrainedOptimalPathInputImpl();
    }

    private OptimalPathInputBuilder optimalPathInputBuilder;
    private ConstrainedOptimalPathInputImpl impl;

    public ConstrainedOptimalPathInputBuilder setOldPath(List<Link> oldPath) {
        impl.oldPath = oldPath;
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setTunnelUnifyKey(TunnelUnifyKey tunnelUnifyKey) {
        impl.tunnelUnifyKey = tunnelUnifyKey;
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setTeArgumentBean(TeArgumentBean teArgumentBean) {
        impl.teArgumentBean = teArgumentBean;
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setBiDirect(BiDirect biDirect) {
        impl.biDirect = biDirect;
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setIsNeedScaleBandwith(boolean isNeedScaleBandwith) {
        impl.needScaleBandwidth = isNeedScaleBandwith;
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setBwSharedGroups(BwSharedGroupContainer bwSharedGroups) {
        impl.bwSharedGroups = bwSharedGroups;
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setDeletedBwSharedGroups(BwSharedGroupContainer deletedBwSharedGroups) {
        impl.deletedBwSharedGroups = deletedBwSharedGroups;
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setExcludedLinks(List<Link> excludedLinks) {
        impl.excludedLinks = excludedLinks;
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setReqMaxDelay(long reqMaxDelay) {
        impl.reqMaxDelay = reqMaxDelay;
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setMasterPath(List<Link> masterPath) {
        impl.masterPath = masterPath;
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setCalcStrategy(ICalcStrategy<NodeId, Link> calcStrategy) {
        optimalPathInputBuilder.setCalcStrategy(calcStrategy);
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setHeadNodeId(NodeId headNodeId) {
        optimalPathInputBuilder.setHeadNodeId(headNodeId);
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setTailNodeId(NodeId tailNodeId) {
        optimalPathInputBuilder.setTailNodeId(tailNodeId);
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setTransformer(ITransformer<Link> transformer) {
        optimalPathInputBuilder.setTransformer(transformer);
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setGraph(Graph<NodeId, Link> graph) {
        optimalPathInputBuilder.setGraph(graph);
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setTopologyId(TopologyId topologyId) {
        optimalPathInputBuilder.setTopologyId(topologyId);
        return this;
    }

    public ConstrainedOptimalPathInputBuilder setSimulateFlag(boolean isSimulate) {
        optimalPathInputBuilder.setSimulateFlag(isSimulate);
        return this;
    }

    public ConstrainedOptimalPathInput<NodeId, Link> build() {
        impl.optimalPathInput = optimalPathInputBuilder.build();
        return impl;
    }

    private final class ConstrainedOptimalPathInputImpl implements ConstrainedOptimalPathInput<NodeId, Link> {
        private ConstrainedOptimalPathInputImpl() {
        }

        private OptimalPathInput<NodeId, Link> optimalPathInput;
        private TunnelUnifyKey tunnelUnifyKey;
        private TeArgumentBean teArgumentBean;
        private List<Link> oldPath;
        private BiDirect biDirect;
        private boolean recalc;
        private boolean needScaleBandwidth;
        private BwSharedGroupContainer bwSharedGroups;
        private BwSharedGroupContainer deletedBwSharedGroups;
        private List<Link> masterPath;
        private long reqMaxDelay = ComUtility.INVALID_DELAY;
        private List<Link> excludedLinks = new ArrayList<>();
        private boolean isSimulate;

        @Override
        public List<Link> getOldPath() {
            return oldPath;
        }

        @Override
        public TunnelUnifyKey getTunnelUnifyKey() {
            return tunnelUnifyKey;
        }

        @Override
        public TeArgumentBean getTeArgumentBean() {
            return teArgumentBean;
        }

        @Override
        public BiDirect getBiDirect() {
            return biDirect;
        }

        @Override
        public List<Link> getExcludedLinks() {
            return excludedLinks;
        }

        @Override
        public BwSharedGroupContainer getBwSharedGroups() {
            return bwSharedGroups;
        }

        @Override
        public BwSharedGroupContainer getDeletedBwSharedGroups() {
            return deletedBwSharedGroups;
        }

        @Override
        public boolean isNeedScaleBandwidth() {
            return needScaleBandwidth;
        }

        @Override
        public boolean isRecalc() {
            return recalc;
        }

        @Override
        public long getReqMaxDelay() {
            return reqMaxDelay;
        }

        @Override
        public List<Link> getMasterPath() {
            return masterPath;
        }

        @Override
        public NodeId getHeadNodeId() {
            return optimalPathInput.getHeadNodeId();
        }

        @Override
        public NodeId getTailNodeId() {
            return optimalPathInput.getTailNodeId();
        }

        @Override
        public ICalcStrategy<NodeId, Link> getCalcStrategy() {
            return optimalPathInput.getCalcStrategy();
        }

        @Override
        public ITransformer<Link> getTransformer() {
            return optimalPathInput.getTransformer();
        }

        @Override
        public Graph<NodeId, Link> getGraph() {
            return optimalPathInput.getGraph();
        }

        @Override
        public TopologyId getTopologyId() {
            return optimalPathInput.getTopologyId();
        }

        @Override
        public boolean isSimulate() {
            return isSimulate;
        }
    }
}
