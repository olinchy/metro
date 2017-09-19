/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.pathcore;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.uci.ics.jung.graph.Graph;
import org.apache.mina.util.ConcurrentHashSet;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.ngip.ipsdn.pce.path.api.graph.GraphCommonUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.PortKey;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

public class TunnelsRecordPerPort {
    private TunnelsRecordPerPort() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(TunnelsRecordPerPort.class);
    private static TunnelsRecordPerPort instance = new TunnelsRecordPerPort();
    private Map<PortKey, PortRecord> portRecords = new ConcurrentHashMap<>();
    private Map<PortKey, PortRecord> portSimuRecords = new ConcurrentHashMap<>();

    public static TunnelsRecordPerPort getInstance() {
        return instance;
    }

    private static Link getReverseLink(Link link) {
        return new LinkBuilder().setSource(new SourceBuilder().setSourceNode(link.getDestination().getDestNode())
                                                   .setSourceTp(
                                                           link.getDestination().getDestTp()).build()).setDestination(
                new DestinationBuilder().setDestNode(link.getSource().getSourceNode())
                        .setDestTp(link.getSource().getSourceTp()).build()).build();
    }

    public void update(TunnelUnifyKey tunnel, List<Link> oldPath, List<Link> newPath) {
        if (oldPath == null && newPath == null) {
            return;
        }
        TunnelUnifyKey reverseTunnel = null;
        if (tunnel.isBiDirectional()) {
            reverseTunnel = new TunnelUnifyRecordKey(tunnel, true);
        }
        if (oldPath == null) {
            addRecords(newPath, tunnel, reverseTunnel);
        } else if (newPath == null) {
            delRecords(oldPath, tunnel, reverseTunnel);
        } else {
            mergeRecords(tunnel, oldPath, newPath, reverseTunnel);
        }
    }

    private void addRecords(List<Link> newPath, TunnelUnifyKey tunnel, TunnelUnifyKey reverseTunnel) {
        for (Link link : newPath) {
            addRecord(link, tunnel);
            if (reverseTunnel != null) {
                Link reverseLink = getReverseLink(link);
                addRecord(reverseLink, reverseTunnel);
            }
        }
    }

    private void delRecords(List<Link> oldPath, TunnelUnifyKey tunnel, TunnelUnifyKey reverseTunnel) {
        for (Link link : oldPath) {
            delRecord(link, tunnel);
            if (reverseTunnel != null) {
                Link reverseLink = getReverseLink(link);
                delRecord(reverseLink, reverseTunnel);
            }
        }
    }

    private void mergeRecords(
            TunnelUnifyKey tunnel, List<Link> oldPath, List<Link> newPath,
            TunnelUnifyKey reverseTunnel) {
        Set<Link> newLinkSet = new HashSet<>();
        for (Link link : newPath) {
            newLinkSet.add(link);
        }

        Iterator<Link> it = oldPath.iterator();
        while (it.hasNext()) {
            Link link = it.next();
            if (newLinkSet.contains(link)) {
                newLinkSet.remove(link);
                continue;
            }

            delRecord(link, tunnel);
            if (reverseTunnel != null) {
                Link reverseLink = getReverseLink(link);
                delRecord(reverseLink, reverseTunnel);
            }
        }

        for (Link link : newLinkSet) {
            addRecord(link, tunnel);
            if (reverseTunnel != null) {
                Link reverseLink = getReverseLink(link);
                addRecord(reverseLink, reverseTunnel);
            }
        }
    }

    private void addRecord(Link link, TunnelUnifyKey tunnel) {
        PortKey portKey = new PortKey(link.getSource().getSourceNode(), link.getSource().getSourceTp());
        Map<PortKey, PortRecord> portKeyPortRecordMap = tunnel.isSimulate() ? portSimuRecords : portRecords;
        if (portKeyPortRecordMap == null) {
            return;
        }
        PortRecord record = portKeyPortRecordMap.get(portKey);
        if (record == null) {
            synchronized (this) {
                record = portKeyPortRecordMap.get(portKey);
                if (record == null) {
                    record = new PortRecord(link);
                    portKeyPortRecordMap.put(portKey, record);
                }
            }
        }

        record.add(tunnel);
    }

    private void delRecord(Link link, TunnelUnifyKey tunnel) {
        PortKey portKey = new PortKey(link.getSource().getSourceNode(), link.getSource().getSourceTp());
        Map<PortKey, PortRecord> portKeyPortRecordMap = tunnel.isSimulate() ? portSimuRecords : portRecords;
        if (portKeyPortRecordMap == null) {
            return;
        }
        PortRecord record = portKeyPortRecordMap.get(portKey);
        if (record != null) {
            record.delete(tunnel);
        }
    }

    /*
    *Node1----Ps----Node2
    *          |
    *          |____Node3
    *link:Ps->Node1,pseudo node don't have ip address or TP.
    *linktemp:Node2->Ps,Node3->Ps
    *The method can get extra tunnels through node2 and node3, don't care.
    * */
    public Set<TunnelUnifyKey> getTunnelKeyOnLink(Link link, Graph<NodeId, Link> graph) {
        Set<TunnelUnifyKey> tunnelSet = new HashSet<>();

        if (!ComUtility.isSourcePseudo(ComUtility.getLinkPseudo(link))) {
            tunnelSet.addAll(getTunnelKeyOnSingleLink(false, link));
            Logs.debug(LOG, "getTunnelKeyOnLink {}\n {}", ComUtility.getLinkString(link), tunnelSet);
            return tunnelSet;
        }

        GraphCommonUtils.forEachInEdge(graph, link.getSource().getSourceNode(),
                                       linkTemp -> tunnelSet.addAll(getTunnelKeyOnSingleLink(false, linkTemp)));
        return tunnelSet;
    }

    public Set<TunnelUnifyKey> getTunnelKeyOnSingleLink(boolean isSimulate, Link link) {
        Set<TunnelUnifyKey> tunnelSet = new HashSet<>();
        Set<TunnelUnifyKey> tunnleKeyOnPort =
                getTunnelsRecord(
                        new PortKey(link.getSource().getSourceNode(), link.getSource().getSourceTp()),
                        isSimulate);
        if ((tunnleKeyOnPort != null) && (!tunnleKeyOnPort.isEmpty())) {
            tunnelSet.addAll(tunnleKeyOnPort);
        }
        return tunnelSet;
    }

    public Set<TunnelUnifyKey> getTunnelsRecord(PortKey portKey, Boolean isSimulate) {
        boolean isSimu = isSimulate == null ? false : isSimulate;
        return isSimu ? getTunnelsSimulateRecord(portKey) : getTunnelsRecord(portKey);
    }

    /**
     * get all simulate tunnel.
     *
     * @param portKey link generator portkey
     * @return tunnels
     */
    public Set<TunnelUnifyKey> getTunnelsSimulateRecord(PortKey portKey) {
        PortRecord portRecord = portSimuRecords.get(portKey);
        return (portRecord == null) ? null : portRecord.getTunnelsRecord();
    }

    public Set<TunnelUnifyKey> getTunnelsRecord(PortKey portKey) {
        PortRecord portRecord = portRecords.get(portKey);
        return (portRecord == null) ? null : portRecord.getTunnelsRecord();
    }

    public Set<TunnelUnifyKey> getTunnelKeyOnLink(boolean isSimulate, Link link, Graph<NodeId, Link> graph) {
        Set<TunnelUnifyKey> tunnelSet = new HashSet<>();

        if (!ComUtility.isSourcePseudo(ComUtility.getLinkPseudo(link))) {
            tunnelSet.addAll(getTunnelKeyOnSingleLink(isSimulate, link));
            return tunnelSet;
        }

        GraphCommonUtils.forEachInEdge(graph, link.getSource().getSourceNode(),
                                       linkTemp -> tunnelSet.addAll(getTunnelKeyOnSingleLink(isSimulate, linkTemp)));
        return tunnelSet;
    }

    public void updateUnifyKey(List<Link> path, TunnelUnifyKey tunnelKeyOld, TunnelUnifyKey tunnelKeyNew) {
        for (Link link : path) {
            delRecord(link, tunnelKeyOld);
            addRecord(link, tunnelKeyNew);
        }
    }

    public void destroy() {
        portRecords.clear();
    }

    /**
     * mirror all record ports.
     */
    public void copyPortSimuRecordMirror() {
        portRecords.forEach((key, value) ->
                                    portSimuRecords.put(
                                            new PortKey(key.getNode(), key.getTp()), new PortRecord(value)));
        LOG.info("Mirror port record generator {}", portSimuRecords);
    }

    /**
     * destroy all mirror records.
     */
    public void destroyPortSimuRecordMirror() {
        portSimuRecords.clear();
        LOG.info("Mirror port record clear {}", portSimuRecords);
    }

    private class PortRecord {
        PortRecord(Link link) {
            this.portKey = new PortKey(
                    link.getSource().getSourceNode(),
                    link.getSource().getSourceTp());
        }

        PortRecord(PortRecord source) {
            setPortKey(new PortKey(source.portKey.getNode(), source.portKey.getTp()));
            Set<TunnelUnifyKey> tunnelUnifyKeys = new ConcurrentHashSet<>();
            source.tunnelSet.forEach(key -> {
                TunnelUnifyKey newKey = new TunnelUnifyKey(key);
                newKey.setSimulateFlag(true);
                tunnelUnifyKeys.add(new TunnelUnifyRecordKey(newKey));
            });
            setTunnelSet(tunnelUnifyKeys);
        }

        @SuppressWarnings("unused")
        private PortKey portKey;
        private Set<TunnelUnifyKey> tunnelSet = new ConcurrentHashSet<>();

        public void setPortKey(PortKey portKey) {
            this.portKey = portKey;
        }

        public void setTunnelSet(Set<TunnelUnifyKey> tunnelSet) {
            this.tunnelSet = tunnelSet;
        }

        public Set<TunnelUnifyKey> getTunnelsRecord() {
            return tunnelSet;
        }

        public void delete(TunnelUnifyKey tunnelKey) {
            tunnelSet.remove(tunnelKey);
        }

        public void add(TunnelUnifyKey tunnelKey) {
            tunnelSet.add(tunnelKey);
        }
    }
}
