/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.LinkUtils;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;

/**
 * Created by 10087505 on 2017/8/1.
 */
public class HsbTunnelUtil {
    private HsbTunnelUtil() {

    }

    public static boolean isPathDelayEligible(
            TopologyId topologyId, boolean isSimulate, long delay,
            List<Link> pathLinks) {
        long totalDelay = 0L;
        long reverseTotalDelay = 0L;
        for (Link link : pathLinks) {
            totalDelay += TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkDelay(link);
            reverseTotalDelay += TopoServiceAdapter.getInstance().getPceTopoProvider().getLinkDelay(ComUtility
                                                                                                            .getReverseLink4Path(
                                                                                                                    TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoGraph(
                                                                                                                            isSimulate,
                                                                                                                            topologyId),
                                                                                                                    link));
            if (totalDelay > delay || reverseTotalDelay > delay) {
                return false;
            }
        }
        return true;
    }

    public static boolean needRollback(boolean isFailRollback, PceResult pceResult) {
        return isFailRollback && pceResult.isCalcFail();
    }

    public static List<Link> calcOverlapPath(List<Link> masterPath, List<Link> slavePath) {
        if (masterPath == null || slavePath == null) {
            return Collections.emptyList();
        }
        List<Link> overlapPath = new ArrayList<>();
        for (Link masterLink : masterPath) {
            if (isLinkInPath(masterLink, slavePath)) {
                overlapPath.add(masterLink);
            }
        }
        return overlapPath;
    }

    private static boolean isLinkInPath(Link link, List<Link> path) {
        for (Link pathLink : path) {
            if (LinkUtils.linkEqual(link, pathLink)) {
                return true;
            }
        }
        return false;
    }
}
