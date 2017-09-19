/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.sr.argument.SrArgument;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;

import com.zte.ngip.ipsdn.pce.path.api.srlg.Srlg;
import com.zte.ngip.ipsdn.pce.path.api.srlg.SrlgAttribute;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;

public interface ITunnel {
    int getId();

    NodeId getHeadNode();

    NodeId getTailNode();

    TopologyId getTopoId();

    long getOccupyBw();

    byte getHoldPriority();

    TunnelUnifyKey getTunnelUnifyKey();

    List<Link> getMasterPath();

    List<Link> getSlavePath();

    SrArgument getSrArgument();

    void writeDb();

    void removeDb();

    void writeMemory();

    void destroy();

    PceResult reCalcPath(TunnelUnifyKey path);

    PceResult refreshPath(TunnelUnifyKey path);

    default PceResult refreshPath() {
        return PceResult.nullPceResult;
    }

    default PceResult refreshUnestablishPath() {
        return PceResult.nullPceResult;
    }

    default PceResult refreshUnestablishAndSrlgPath() {
        return PceResult.nullPceResult;
    }

    void refreshSegments();

    default void decreaseBandwidth(long newBandwidth, BwSharedGroupContainer bwContainer) {
        // do nothing
    }

    default void notifyPathChange() {
        // do nothing
    }

    default void notifySegmentsChange() {
        // do nothing
    }

    BiDirect getBiDirect();

    int getTunnelState();

    int setTunnelState(int newState);

    boolean comAndSetTunnelState(int oldState, int newState);

    void reNotifyTunnelPath();

    default List<Long> transSrlgAttr(SrlgAttribute srlgAttribute) {
        if (srlgAttribute == null || srlgAttribute.getSrlgs().isEmpty()) {
            return Collections.emptyList();
        }
        return srlgAttribute.getSrlgs().stream().map(Srlg::getValue).collect(Collectors.toList());
    }

    TeArgumentBean getTeArgumentBean();

    boolean isDelayRestricted();

    boolean isSrlgOverlap();

    default boolean isDelayEligible() {
        return true;
    }

    boolean isUnestablished();

    boolean isPathOverlap();

    boolean isMetricStrategy();

    boolean isDelayStrategy();

    boolean isSrTunnel();

    boolean isSimulate();

    boolean isChangeToZeroBandWidth();
}
