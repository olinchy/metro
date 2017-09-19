/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser;

import java.util.List;
import java.util.Optional;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input.ConstrainedOptimalPathInput;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;

import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.strategy.DelayStrategy;
import com.zte.ngip.ipsdn.pce.path.core.strategy.ICalcStrategy;
import com.zte.ngip.ipsdn.pce.path.core.strategy.MetricStrategy;

/**
 * Created by 10204924 on 2017/2/14.
 */
public class PathChooserFactory {
    private PathChooserFactory() {
    }

    /**
     * Create Path Chooser.
     *
     * @param chooseNum       the number of paths wanted.
     * @param input           input
     * @param isRecalc        isRecalc is true means wanting a ineligible path.
     * @param avoidLinks      avoidLinks try to avoid links.
     * @param pathChooserName pathChooserName
     * @return a Path Chooser
     */
    public static PathChooser<PathResult> create(
            final int chooseNum,
            final ConstrainedOptimalPathInput<NodeId, Link> input, final boolean isRecalc, List<Link> avoidLinks,
            PathChooserName pathChooserName) {
        PathChooserName chooserName = Optional.ofNullable(pathChooserName).orElse(PathChooserName.DEFAULT);
        if (PathChooserName.DOMAIN_PATH_CHOOSER.equals(chooserName)) {
            return new DomainPathChooser(chooseNum);
        } else {
            return getPathChooserForTunnel(chooseNum, input, isRecalc, avoidLinks);
        }
    }

    private static PathChooser<PathResult> getPathChooserForTunnel(
            final int chooseNum,
            final ConstrainedOptimalPathInput<NodeId, Link> input, final boolean isRecalc, List<Link> avoidLinks) {
        final ICalcStrategy<NodeId, Link> calcStrategy = input.getCalcStrategy();

        if (calcStrategy instanceof MetricStrategy) {
            return getPathChooserForMetricStrategy(chooseNum, input, isRecalc, avoidLinks, input.getTunnelUnifyKey());
        } else if (calcStrategy instanceof DelayStrategy) {
            return getPathChooserForDelayStrategy(chooseNum, input, isRecalc, input.getTunnelUnifyKey());
        } else {
            return new MaxBandwidthPathChooser(chooseNum);
        }
    }

    private static PathChooser<PathResult> getPathChooserForMetricStrategy(
            int chooseNum,
            ConstrainedOptimalPathInput<NodeId, Link> input, boolean isRecalc, List<Link> avoidLinks,
            TunnelUnifyKey tunnelUnifyKey) {
        if (isRecalc) {
            return new MinMetricPossiblyEligiblePathChooser(chooseNum, input.getReqMaxDelay(),
                                                            input.getBiDirect() != null, input.getTopologyId(),
                                                            avoidLinks, tunnelUnifyKey);
        } else {
            return new BaseStrictlyEligiblePathChooser(chooseNum, input.getReqMaxDelay(), input.getBiDirect() != null,
                                                       input.getTopologyId(), input.getTunnelUnifyKey());
        }
    }

    private static PathChooser<PathResult> getPathChooserForDelayStrategy(
            int chooseNum,
            ConstrainedOptimalPathInput<NodeId, Link> input, boolean isRecalc, TunnelUnifyKey tunnelUnifyKey) {
        if (isRecalc) {
            return new MinDelayPossiblyEligiblePathChooser(chooseNum, input.getReqMaxDelay(),
                                                           input.getBiDirect() != null, input.getTopologyId(),
                                                           tunnelUnifyKey);
        } else {
            return new BaseStrictlyEligiblePathChooser(chooseNum, input.getReqMaxDelay(),
                                                       input.getBiDirect() != null, input.getTopologyId(),
                                                       input.getTunnelUnifyKey());
        }
    }

    public enum PathChooserName {
        MAX_BANDWIDTH, MIN_DELAY_POSSIBLY, MIN_METRIC_POSSIBLY, BASE_STRICTLY, DOMAIN_PATH_CHOOSER, DEFAULT
    }
}
