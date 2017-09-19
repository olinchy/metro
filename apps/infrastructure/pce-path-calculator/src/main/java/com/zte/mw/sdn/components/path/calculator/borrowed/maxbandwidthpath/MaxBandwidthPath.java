/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.maxbandwidthpath;

import java.util.LinkedList;
import java.util.List;

import edu.uci.ics.jung.graph.Graph;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetMaxAvailableBandwidthInput;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.DiffServBw;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.BandwidthTransformer;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.BandwidthTransformerFactory;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.MaxBandwidthStrategy;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.PathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;

/**
 * Created by 10088483 on 5/2/16.
 */
public class MaxBandwidthPath {
    public MaxBandwidthPath(GetMaxAvailableBandwidthInput input) {
        this.headNodeId = input.getHeadNodeId();
        this.tailNodeId = input.getTailNodeId();
        this.topoId = (input.getTopologyId() != null)
                ? input.getTopologyId() : TopologyId.getDefaultInstance(ComUtility.DEFAULT_TOPO_ID_STRING);
        this.teArg = new TeArgumentBean(input, this.topoId);
        /*(input.getTunnelId() != null)?input.getTunnelId().intValue():0; not support input tunnel temporily*/
        int tunnelId = 0;
        this.tunnelKey = new TunnelUnifyKey(this.headNodeId, tunnelId, false, true, true);
        this.isBiDirect = (input.isIsBiDirect() != null) ? input.isIsBiDirect() : false;
    }

    private NodeId headNodeId;
    private NodeId tailNodeId;
    private LinkedList<Link> path;
    private TopologyId topoId;
    private TeArgumentBean teArg;
    private TunnelUnifyKey tunnelKey;
    private boolean isBiDirect;

    public void calcPath() {
        PathProvider<BandwidthTransformer> pathProvider = new PathProvider(headNodeId, tunnelKey, tailNodeId, topoId,
                                                                           new MaxBandwidthStrategy<NodeId, Link>(
                                                                                   isBiDirect, topoId),
                                                                           new BandwidthTransformerFactory());

        PceResult result = new PceResult();
        pathProvider.setTeArgWithBuildNew(teArg);
        pathProvider.setIsRealTimePath(true);
        pathProvider.setRecalc(true);

        pathProvider.calcPath(result);
        path = (LinkedList<Link>) pathProvider.getPath();
    }

    public List<Link> getLsp() {
        return path;
    }

    public long getMaxBandwidthToCreateTunnel() {
        if (null == path || path.isEmpty()) {
            return 0;
        }

        long positiveBw = getPositiveBw();
        long reverseBw = getReverseBw();

        if (positiveBw > reverseBw) {
            return reverseBw;
        }
        return positiveBw;
    }

    private long getPositiveBw() {

        long positiveBw = Long.MAX_VALUE;
        for (Link link : path) {
            long curLinkReservedBw = PceUtil.getReservedBwToTunnelInLink(tunnelKey,
                                                                         link, DiffServBw.LOWEST_PRIORITY);
            if (positiveBw > curLinkReservedBw) {
                positiveBw = curLinkReservedBw;
            }
        }
        return positiveBw;
    }

    private long getReverseBw() {

        long reverseBw = Long.MAX_VALUE;
        if (isBiDirect) {
            Graph<NodeId, Link> graph =
                    TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoGraph(false, topoId);
            for (Link link : path) {
                Link reverseLink = ComUtility.getReverseLink4Path(graph, link);

                long curLinkReservedBw = PceUtil.getReservedBwToTunnelInLink(tunnelKey,
                                                                             reverseLink, DiffServBw.LOWEST_PRIORITY);
                if (reverseBw > curLinkReservedBw) {
                    reverseBw = curLinkReservedBw;
                }
            }
        }
        return reverseBw;
    }
}