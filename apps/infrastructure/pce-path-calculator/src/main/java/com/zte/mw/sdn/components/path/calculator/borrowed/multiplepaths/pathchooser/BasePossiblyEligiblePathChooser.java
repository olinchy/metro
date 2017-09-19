/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.MultiplePathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

/**
 * Created by 10204924 on 2017/3/2.<br>
 * Only choose paths which strictly satisfy our conditions.
 * When no paths are chosen, this chooser try to return the first PathResult in originalList as a default result.<br>
 * The default result may not satisfy our conditions.<br>
 * This path chooser is usually used in these cases:<br>
 * &nbsp;&nbsp;1. Calculating single tunnel or HSB_PATHS tunnel with Min-Metric strategy.<br>
 * &nbsp;&nbsp;2. Calculating HSB_PATHS slave path in Min-Delay strategy when master path exists.<br>
 */
public class BasePossiblyEligiblePathChooser extends BaseStrictlyEligiblePathChooser {
    public BasePossiblyEligiblePathChooser(
            int chooseNum, long maxDelay, boolean biDirect, TopologyId topologyId,
            TunnelUnifyKey tunnelUnifyKey) {
        super(chooseNum, maxDelay, biDirect, topologyId, tunnelUnifyKey);
    }

    private static final Logger LOG = LoggerFactory.getLogger(BasePossiblyEligiblePathChooser.class);

    @Override
    protected List<PathResult> getDefaultPathResults(MultiplePathResult<PathResult> originalResult) {
        PathResult defaultResult = originalResult.getPathResults().stream().findFirst().orElse(new PathResult());
        super.setCalcFailType(defaultResult, originalResult);
        Logs.debug(getLogger(), "getDefaultPathResults: path={} failType={}",
                   ComUtility.pathToString(defaultResult.getPath()), defaultResult.getPceResult().getCalcFailType());
        return Lists.newArrayList(defaultResult);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
