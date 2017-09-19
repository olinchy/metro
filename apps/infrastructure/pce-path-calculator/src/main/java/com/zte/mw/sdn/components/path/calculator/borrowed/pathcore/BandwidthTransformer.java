/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.pathcore;

import edu.uci.ics.jung.graph.Graph;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.DiffServBw;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformer;

/**
 * Created by 10088483 on 5/2/16.
 */
public class BandwidthTransformer implements ITransformer<Link> {
    public BandwidthTransformer(TunnelUnifyKey tunnelKey) {
        this.tunnelKey = tunnelKey;
    }

    private TunnelUnifyKey tunnelKey;

    @Override
    public Double transform(Link link, boolean isBiDirect, TopologyId topoId) {
        long reservedBwToTunnel;
        long positiveReservedBwToTunnel =
                PceUtil.getReservedBwToTunnelInLink(tunnelKey, link, DiffServBw.LOWEST_PRIORITY);

        if (isBiDirect) {
            Graph<NodeId, Link> graph = PceUtil.getTopoGraph(ComUtility.getSimulateFlag(tunnelKey), topoId);
            Link reverseLink = ComUtility.getReverseLink4Path(graph, link);
            long reverseReservedBwToTunnel = 0;
            if (null != reverseLink) {
                reverseReservedBwToTunnel =
                        PceUtil.getReservedBwToTunnelInLink(tunnelKey, reverseLink, DiffServBw.LOWEST_PRIORITY);
            }

            if (positiveReservedBwToTunnel >= reverseReservedBwToTunnel) {
                reservedBwToTunnel = reverseReservedBwToTunnel;
            } else {
                reservedBwToTunnel = positiveReservedBwToTunnel;
            }
        } else {
            reservedBwToTunnel = positiveReservedBwToTunnel;
        }
        return (double) reservedBwToTunnel;
    }

    @Override
    public long transformSingleDirection(Link link, boolean isPositive, TopologyId topoId) {
        if (isPositive) {
            return PceUtil.getReservedBwToTunnelInLink(tunnelKey,
                                                       link, DiffServBw.LOWEST_PRIORITY);
        } else {
            Graph<NodeId, Link> graph = PceUtil.getTopoGraph(ComUtility.getSimulateFlag(tunnelKey), topoId);
            Link reverseLink = ComUtility.getReverseLink4Path(graph, link);
            if (null != reverseLink) {
                return PceUtil.getReservedBwToTunnelInLink(tunnelKey,
                                                           reverseLink, DiffServBw.LOWEST_PRIORITY);
            } else {
                return 0;
            }
        }
    }
}


