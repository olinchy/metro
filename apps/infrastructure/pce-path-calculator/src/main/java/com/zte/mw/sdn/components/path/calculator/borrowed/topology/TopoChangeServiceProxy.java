/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.topology;

import java.util.List;
import java.util.Set;

import edu.uci.ics.jung.graph.Graph;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceHServiceProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;

import com.zte.ngip.ipsdn.pce.path.api.RefreshTarget;
import com.zte.ngip.ipsdn.pce.path.api.TopoChangeListener;
import com.zte.ngip.ipsdn.pce.path.api.topochange.LinkChange;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

public class TopoChangeServiceProxy implements TopoChangeListener {
    public TopoChangeServiceProxy(PcePathProvider pcePathProvider, BandWidthMng bandWidthMng) {
        this.pcePathProvider = pcePathProvider;
        this.bandWidthMng = bandWidthMng;
    }

    public TopoChangeServiceProxy(
            PcePathProvider pcePathProvider, PceHServiceProvider pceHServiceProvider,
            BandWidthMng bandWidthMng) {
        this.pcePathProvider = pcePathProvider;
        this.pceHServiceProvider = pceHServiceProvider;
        this.bandWidthMng = bandWidthMng;
    }

    private PcePathProvider pcePathProvider;
    private PceHServiceProvider pceHServiceProvider;
    private BandWidthMng bandWidthMng;

    @Override
    public void refreshTunnels(
            boolean isSimulate, List<TunnelUnifyKey> migrateTunnels,
            Set<RefreshTarget> refreshTargets, TopologyId topoId) {
        pcePathProvider.refreshTunnels(isSimulate, migrateTunnels, refreshTargets, topoId);
    }

    @Override
    public void refreshTunnelsOnLink(
            boolean isSimulate, Link link, Graph<NodeId, Link> graph,
            Set<RefreshTarget> targets) {
        pcePathProvider.refreshTunnelsOnLink(isSimulate, link, graph, targets);
    }

    @Override
    public void notifyLinkChange(List<LinkChange> linkChangeList, Graph<NodeId, Link> graph) {
        pceHServiceProvider.handleLinkChange(linkChangeList, graph);
    }

    @Override
    public void addPort(boolean isSimulate, Link link, long maxBw) {
        bandWidthMng.addPort(isSimulate, link, maxBw);
    }

    @Override
    public void delPort(boolean isSimulate, Link link) {
        bandWidthMng.delPort(isSimulate, link);
    }

    @Override
    public List<TunnelUnifyKey> updatePort(boolean isSimulate, Link link, long maxBw) {
        return bandWidthMng.updatePort(isSimulate, link, maxBw);
    }

    @Override
    public void recoverAddPort(Link link, long maxBw) {
        bandWidthMng.recoverAddPort(link, maxBw);
    }
}
