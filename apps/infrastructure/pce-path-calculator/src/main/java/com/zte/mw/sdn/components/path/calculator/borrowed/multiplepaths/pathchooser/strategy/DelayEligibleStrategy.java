/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser.strategy;

import java.util.function.Predicate;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.util.Logs;

/**
 * Created by 10204924 on 2017/3/7.
 * Test the path's delay is satisfied our conditions.
 */
public class DelayEligibleStrategy implements Predicate<PathResult> {
    private DelayEligibleStrategy(
            long reqPositiveDelay, long reqReverseDelay, boolean isBidirect,
            TopologyId topologyId, boolean isSimulate) {
        this.reqPositiveDelay = reqPositiveDelay;
        this.reqReverseDelay = reqReverseDelay;
        this.bidirect = isBidirect;
        this.topologyId = topologyId;
        this.isSimulate = isSimulate;
    }

    private static final Logger LOG = LoggerFactory.getLogger(DelayEligibleStrategy.class);
    private final long reqPositiveDelay;
    private final long reqReverseDelay;
    private final boolean bidirect;
    private final TopologyId topologyId;
    private final boolean isSimulate;

    public static DelayEligibleStrategy create(
            long reqMaxDelay, boolean isBidirect, TopologyId topologyId,
            boolean isSimulate) {
        return new DelayEligibleStrategy(reqMaxDelay, reqMaxDelay, isBidirect, topologyId, isSimulate);
    }

    @Override
    public boolean test(PathResult pathResult) {
        long positiveDelay = PceUtil.calcPositiveDelay(pathResult.getPath());
        long reverseDelay = 0;
        boolean isDelayEligible = positiveDelay <= reqPositiveDelay;
        if (bidirect) {
            reverseDelay = PceUtil.calcReverseDelay(isSimulate, pathResult.getPath(), topologyId);
            isDelayEligible = isDelayEligible && reverseDelay <= reqReverseDelay;
        }
        final String logStr = "positiveDelay=" + positiveDelay + (bidirect ? " reverseDelay=" + reverseDelay : "")
                + " reqMaxDelay=" + reqPositiveDelay + " isDelayEligible=" + isDelayEligible;
        Logs.debug(LOG, logStr);

        return isDelayEligible;
    }
}
