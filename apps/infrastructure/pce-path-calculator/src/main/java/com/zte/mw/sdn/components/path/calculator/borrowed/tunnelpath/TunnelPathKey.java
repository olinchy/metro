/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

public class TunnelPathKey {
    public TunnelPathKey(NodeId headNodeId, int tunnelId) {
        this.headNodeId = headNodeId;
        this.tunnelId = tunnelId;
    }

    public TunnelPathKey(NodeId headNodeId, String serviceName) {
        this.headNodeId = headNodeId;
        this.serviceName = serviceName;
    }

    private NodeId headNodeId;
    private int tunnelId;
    private String serviceName;

    public NodeId getHeadNodeId() {
        return headNodeId;
    }

    public int getTunnelId() {
        return tunnelId;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((headNodeId == null) ? 0 : headNodeId.hashCode());
        result = prime * result + tunnelId;
        if (serviceName != null) {
            result = prime * result + serviceName.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        return isTunnelPathKeyEqual((TunnelPathKey) obj);
    }

    private boolean isTunnelPathKeyEqual(TunnelPathKey other) {
        if (headNodeId == null) {
            if (other.headNodeId != null) {
                return false;
            }
        } else if (!headNodeId.equals(other.headNodeId)) {
            return false;
        }
        if (tunnelId != other.tunnelId) {
            return false;
        }
        if (!isServiceNameEqual(other)) {
            return false;
        }
        return true;
    }

    private boolean isServiceNameEqual(TunnelPathKey other) {
        if (serviceName == null && other.serviceName == null) {
            return true;
        }
        if (serviceName != null && other.serviceName != null) {
            return serviceName.equals(other.serviceName);
        }
        return false;
    }
}
