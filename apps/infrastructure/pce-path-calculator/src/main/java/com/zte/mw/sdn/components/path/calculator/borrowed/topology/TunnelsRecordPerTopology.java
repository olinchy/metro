/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.topology;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.util.ConcurrentHashSet;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.TunnelGroupPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathInstance;

import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

public class TunnelsRecordPerTopology {
    private TunnelsRecordPerTopology() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(TunnelsRecordPerTopology.class);
    private static TunnelsRecordPerTopology instance = new TunnelsRecordPerTopology();
    private Map<TopologyId, TopologyRecord> topologyRecords = new ConcurrentHashMap<>();
    private Map<TopologyId, TopologyRecord> topologySimulateRecords = new ConcurrentHashMap<>();
    private PcePathProvider pcePathService;

    public static TunnelsRecordPerTopology getInstance() {
        return instance;
    }

    public void setPcePathService(PcePathProvider pcePathService) {
        this.pcePathService = pcePathService;
    }

    public void add(TopologyId topoId, TunnelUnifyKey tunnelUnifyKey) {
        Map<TopologyId, TopologyRecord> topologyRecordMap =
                tunnelUnifyKey.isSimulate() ? topologySimulateRecords : topologyRecords;
        TopologyRecord topoRecord = topologyRecordMap.get(topoId);
        if (topoRecord == null) {
            synchronized (this) {
                topoRecord = topologyRecordMap.get(topoId);
                if (topoRecord == null) {
                    topoRecord = new TopologyRecord(topoId);
                    topologyRecordMap.put(topoId, topoRecord);
                }
            }
        }

        topoRecord.add(tunnelUnifyKey);
    }

    public void remove(TopologyId topoId, TunnelUnifyKey tunnelUnifyKey) {
        Map<TopologyId, TopologyRecord> topologyRecordMap =
                tunnelUnifyKey.isSimulate() ? topologySimulateRecords : topologyRecords;

        TopologyRecord topoRecord = topologyRecordMap.get(topoId);
        if (topoRecord != null) {
            topoRecord.remove(tunnelUnifyKey);
        }
    }

    @VisibleForTesting
    public void migrateTopologyId(TopologyId fromTopologyId, TopologyId toTopologyId) {
        TopologyRecord topologyRecord = topologyRecords.get(fromTopologyId);
        if (topologyRecord == null) {
            return;
        }

        topologyRecords.remove(fromTopologyId);
        topologyRecords.put(toTopologyId, topologyRecord);

        topologyRecord.updateTopologyId(toTopologyId);
    }

    @VisibleForTesting
    public Set<TunnelUnifyKey> getTunnelSetByTopoId(TopologyId topologyId) {
        TopologyRecord topologyRecord = topologyRecords.get(topologyId);
        if (topologyRecord == null) {
            return new HashSet<>();
        }

        return topologyRecord.get();
    }

    public void copySimilateTopoRecord() {
        topologyRecords.forEach((key, value) ->
                                        topologySimulateRecords.put(
                                                TopologyId.getDefaultInstance(key.getValue()),
                                                new TopologyRecord(value)));
        LOG.info("copySimilateTopoRecord {}", topologySimulateRecords);
    }

    public void destroySimilateTopoRecord() {
        topologySimulateRecords.clear();
        LOG.info("Port map Mirror destroy {}", topologySimulateRecords);
    }

    private class TopologyRecord {
        TopologyRecord(TopologyId topoId) {
            this.topoId = topoId;
        }

        TopologyRecord(TopologyRecord source) {
            tunnelSet.forEach(key -> this.tunnelSet.add(new TunnelUnifyKey(key).setSimulateFlag(true)));
            this.topoId = TopologyId.getDefaultInstance(source.topoId.getValue());
        }

        private TopologyId topoId;
        private Set<TunnelUnifyKey> tunnelSet = new ConcurrentHashSet<>();

        public void remove(TunnelUnifyKey tunnelUnifyKey) {
            tunnelSet.remove(tunnelUnifyKey);
        }

        public void add(TunnelUnifyKey tunnelUnifyKey) {
            tunnelSet.add(tunnelUnifyKey);
        }

        public Set<TunnelUnifyKey> get() {
            return tunnelSet;
        }

        public void updateTopologyId(TopologyId toTopologyId) {
            this.topoId = toTopologyId;
            for (TunnelUnifyKey tunnel : tunnelSet) {
                if (tunnel.isTg()) {
                    TunnelGroupPathInstance tgPath = pcePathService.getTunnelGroupInstance(
                            tunnel.getHeadNode(),
                            tunnel.getTgId());
                    if (tgPath != null) {
                        tgPath.updateTopoId(topoId);
                    }
                } else {
                    TunnelPathInstance tunnelPath = pcePathService.getTunnelInstance(
                            tunnel.getHeadNode(),
                            tunnel.getTunnelId());
                    if (tunnelPath != null) {
                        tunnelPath.updateTopoId(topoId);
                    }
                }
            }
        }
    }
}
