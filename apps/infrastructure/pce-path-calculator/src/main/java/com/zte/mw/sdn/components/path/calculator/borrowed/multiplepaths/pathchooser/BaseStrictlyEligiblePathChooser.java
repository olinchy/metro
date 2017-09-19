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
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser.strategy.DelayEligibleStrategy;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.MultiplePathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

/**
 * Created by 10204924 on 2017/2/28.<br>
 * Only choose paths which strictly satisfy our conditions.
 * When no paths are chosen, this chooser build an empty PathResult with corresponding calcFailType.<br>
 * This path chooser is usually used in these cases:<br>
 * &nbsp;&nbsp;1. Calculating single tunnel or HSB master with Min-Delay strategy.<br>
 */
public class BaseStrictlyEligiblePathChooser extends AbstractPathChooser {
    /**
     * Constructor.
     *
     * @param chooseNum  chooseNum
     * @param maxDelay   maxDelay
     * @param biDirect   biDirect
     * @param topologyId topologyId
     */
    public BaseStrictlyEligiblePathChooser(
            int chooseNum, long maxDelay, boolean biDirect, TopologyId topologyId,
            TunnelUnifyKey tunnelUnifyKey) {
        super(chooseNum);
        this.maxDelay = maxDelay;
        this.biDirect = biDirect;
        this.topologyId = topologyId;
        this.tunnelUnifyKey = tunnelUnifyKey;
    }

    private static final Logger LOG = LoggerFactory.getLogger(BaseStrictlyEligiblePathChooser.class);
    protected final long maxDelay;
    protected final boolean biDirect;
    protected final TopologyId topologyId;
    protected final TunnelUnifyKey tunnelUnifyKey;

    @Override
    protected List<PathResult> getDefaultPathResults(MultiplePathResult<PathResult> originalResult) {
        PathResult defaultResult = new PathResult();
        setCalcFailType(defaultResult, originalResult);
        Logs.debug(getLogger(), "getDefaultPathResults calcFailType {}",
                   defaultResult.getPceResult().getCalcFailType());
        return Lists.newArrayList(defaultResult);
    }

    @Override
    protected boolean check(PathResult result) {
        return DelayEligibleStrategy.create(maxDelay, biDirect, topologyId, ComUtility.getSimulateFlag(tunnelUnifyKey))
                .test(result);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    protected void setCalcFailType(PathResult defaultResult, MultiplePathResult<PathResult> originalResult) {

        if (originalResult.getPathResults() == null || originalResult.getPathResults().isEmpty()) {
            defaultResult.getPceResult().setCalcFailType(CalcFailType.NoPath);
            defaultResult.getPceResult().setFailReason(originalResult.getFailType());
            defaultResult.getPceResult().setCalcFail(true);
        } else {
            defaultResult.getPceResult().setCalcFailType(CalcFailType.DelayIneligible);
        }
    }
}
