/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

public class TunnelGroupPathKey {
    public TunnelGroupPathKey(NodeId nodeId, int tunnelGroupId) {
        this.nodeId = nodeId;
        this.tunnelGroupId = tunnelGroupId;
    }

    private NodeId nodeId;
    private int tunnelGroupId;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
        result = prime * result + tunnelGroupId;
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
        return isTunnelGroupPathKeyEqual((TunnelGroupPathKey) obj);
    }

    private boolean isTunnelGroupPathKeyEqual(TunnelGroupPathKey other) {
        if (nodeId == null) {
            if (other.nodeId != null) {
                return false;
            }
        } else if (!nodeId.equals(other.nodeId)) {
            return false;
        }
        if (tunnelGroupId != other.tunnelGroupId) {
            return false;
        }
        return true;
    }
}
