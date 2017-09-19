/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.MultiplePathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;

import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

/**
 * Created by 10204924 on 2017/2/25.
 * This path chooser is used for Min-Metric strategy. It tries to choose some paths that are delay-eligible.
 * When there are no delay-eligible paths, this path chooser tries to return the min-metric path.
 */
public class MinMetricPossiblyEligiblePathChooser extends BasePossiblyEligiblePathChooser {
    public MinMetricPossiblyEligiblePathChooser(
            int chooseNum, long maxDelay, boolean biDirect, TopologyId topologyId,
            List<Link> avoidLinks, TunnelUnifyKey tunnelUnifyKey) {
        super(chooseNum, maxDelay, biDirect, topologyId, tunnelUnifyKey);
        this.avoidLinks = avoidLinks;
    }

    private static final Logger LOG = LoggerFactory.getLogger(MinMetricPossiblyEligiblePathChooser.class);
    private List<Link> avoidLinks;

    @Override
    public List<PathResult> getChosenList(MultiplePathResult<PathResult> originalResult) {

        Predicate<Link> isInAvoidLinks = avoidLinks::contains;
        Predicate<PathResult> hasAvoidLinks = pathResult -> {
            List<Link> path = Optional.ofNullable(pathResult.getPath()).orElse(new LinkedList<>());
            return path.parallelStream().anyMatch(isInAvoidLinks);
        };
        List<PathResult> filtered =
                this.chosenList.parallelStream().filter(hasAvoidLinks.negate()).collect(Collectors.toList());

        // For ensuring TryToAvoid has high priority than DelayEligible, we should do as below:
        // When there are some chosen paths having avoid links,
        // exclude them and re-choose some min metric paths instead.
        if (filtered.size() < chosenList.size()) {
            Logs.debug(getLogger(), "Chosen paths contains try-to-avoid-links, "
                    + "exclude them and re-choose some min metric(but delay ineligible) paths instead.");

            List<PathResult> retResults =
                    originalResult.getPathResults().stream().filter(pathResult -> !filtered.contains(pathResult))
                            .limit(chosenList.size() - filtered.size()).collect(Collectors.toList());
            retResults.forEach(pathResult -> pathResult.getPceResult().setCalcFailType(CalcFailType.DelayIneligible));
            retResults.addAll(filtered);
            return retResults;
        }

        return super.getChosenList(originalResult);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
