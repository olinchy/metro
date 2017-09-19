/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.MultiplePathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;

import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

/**
 * Created by 10204924 on 2017/2/25.
 * This path chooser is used for Min-Delay strategy. It tries to choose some paths that are delay-eligible.
 * When there are no delay-eligible paths, this path chooser tries to return the min-delay paths.
 */
public class MinDelayPossiblyEligiblePathChooser extends BasePossiblyEligiblePathChooser {
    public MinDelayPossiblyEligiblePathChooser(
            int chooseNum, long maxDelay, boolean biDirect, TopologyId topologyId,
            TunnelUnifyKey tunnelUnifyKey) {
        super(chooseNum, maxDelay, biDirect, topologyId, tunnelUnifyKey);
    }

    private static final Logger LOG = LoggerFactory.getLogger(MinDelayPossiblyEligiblePathChooser.class);

    @Override
    protected boolean check(PathResult result) {
        return true;
    }

    @Override
    public List<PathResult> getChosenList(MultiplePathResult<PathResult> originalResult) {
        if (!chosenList.isEmpty()) {
            chosenList.stream().filter(pathResult -> !super.check(pathResult))
                    .peek(pr -> Logs.info(getLogger(), "set calcFailType for ineligible result"))
                    .forEach(pathResult -> pathResult.getPceResult().setCalcFailType(CalcFailType.DelayIneligible));
            return new ArrayList<>(chosenList);
        }
        return super.getChosenList(originalResult);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
