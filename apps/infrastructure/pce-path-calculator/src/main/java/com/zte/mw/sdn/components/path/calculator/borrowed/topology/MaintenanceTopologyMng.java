/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.topology;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import com.zte.ngip.ipsdn.pce.path.api.util.PortKey;

/**
 * Created by 10204924 on 2017/1/10.
 */
public class MaintenanceTopologyMng {
    private MaintenanceTopologyMng() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceTopologyMng.class);
    private static MaintenanceTopologyMng instance = new MaintenanceTopologyMng();
    private Set<NodeId> excludingNodes = Collections.synchronizedSet(new HashSet<>());
    private Set<PortKey> excludingPorts = Collections.synchronizedSet(new HashSet<>());
    private Set<NodeId> simulateExcludingNodes = Collections.synchronizedSet(new HashSet<>());
    private Set<PortKey> simulateExcludingPorts = Collections.synchronizedSet(new HashSet<>());

    public static MaintenanceTopologyMng getInstance() {
        return instance;
    }

    public Set<PortKey> getExcludingPorts(boolean isSimulate) {
        return isSimulate ? ImmutableSet.copyOf(simulateExcludingPorts) : ImmutableSet.copyOf(excludingPorts);
    }

    public Set<NodeId> getExcludingNodes(boolean isSimulate) {
        return isSimulate ? ImmutableSet.copyOf(simulateExcludingNodes) : ImmutableSet.copyOf(excludingNodes);
    }

    public void setAllExcludingAddresses(
            Set<NodeId> excludingNodes, Set<PortKey> excludingPorts,
            boolean isSimulate) {
        setExcludingNodes(excludingNodes, isSimulate);
        setExcludingPorts(excludingPorts, isSimulate);
    }

    public void setExcludingNodes(Set<NodeId> excludingNodes, boolean isSimulate) {
        Set<NodeId> nodeIdSet = isSimulate ? this.simulateExcludingNodes : this.excludingNodes;
        nodeIdSet.clear();
        nodeIdSet.addAll(excludingNodes);
    }

    public void setExcludingPorts(Set<PortKey> excludingPorts, boolean isSimulate) {
        Set<PortKey> portKeySet = isSimulate ? this.simulateExcludingPorts : this.excludingPorts;
        portKeySet.clear();
        portKeySet.addAll(excludingPorts);
    }

    public void copySimulateGlobalExcludingAddresses() {
        this.simulateExcludingNodes.clear();
        this.simulateExcludingNodes.addAll(this.excludingNodes);
        LOG.info("copy simulateExcludingNodes {}", simulateExcludingNodes);
        this.simulateExcludingPorts.clear();
        this.simulateExcludingPorts.addAll(this.excludingPorts);
        LOG.info("copy simulateExcludingPorts {}", simulateExcludingNodes);
    }

    public void destroySimulateGlobalExcludingAddresses() {
        this.simulateExcludingNodes.clear();
        LOG.info("simulateExcludingNodes Mirror destroy {}", simulateExcludingNodes);
        this.simulateExcludingPorts.clear();
        LOG.info("simulateExcludingPorts Mirror destroy {}", simulateExcludingPorts);
    }

    public void destroy() {
        excludingNodes.clear();
        excludingPorts.clear();
        simulateExcludingNodes.clear();
        simulateExcludingPorts.clear();
    }
}
