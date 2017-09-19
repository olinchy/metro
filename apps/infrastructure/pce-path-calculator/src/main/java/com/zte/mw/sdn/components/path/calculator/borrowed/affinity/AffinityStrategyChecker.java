/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.affinity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.affinity.AffinityStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.ngip.ipsdn.pce.path.api.util.Checker;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;

/**
 * Created by 10204924 on 2017/4/12.
 */
public class AffinityStrategyChecker implements Checker<AffinityStrategy> {
    private static final Logger LOG = LoggerFactory.getLogger(AffinityStrategyChecker.class);
    private static final String AFFINITY_STRATEGY_OUT_OF_RANGE = "AffinityStrategy is out of range";
    private static final String AFFINITY_STRATEGY_CONFUSING = "AffinityStrategy is confusing!";

    @Override
    public String check(AffinityStrategy affinityStrategy) {
        if (affinityStrategy == null) {
            return "";
        }

        List<Integer> excludeAny = new ArrayList<>(Optional.ofNullable(affinityStrategy.getExcludeAny())
                                                           .orElse(Collections.emptyList()));
        List<Integer> includeAny = new ArrayList<>(Optional.ofNullable(affinityStrategy.getIncludeAny())
                                                           .orElse(Collections.emptyList()));
        List<Integer> includeAll = new ArrayList<>(Optional.ofNullable(affinityStrategy.getIncludeAll())
                                                           .orElse(Collections.emptyList()));
        Logs.debug(LOG, "excludeAny {} includeAny {} includeAll {}", excludeAny, includeAny, includeAll);

        if (checkAffinityStrategyRange(excludeAny, includeAny, includeAll)) {
            Logs.error(LOG, "AffinityStrategy is out of range: {}", affinityStrategy);
            return AFFINITY_STRATEGY_OUT_OF_RANGE;
        }

        // 如果includeAny是excludeAny的子集，或者includeAll和excludeAny有交集，那么肯定算不出路来，就没必要算了
        if ((!includeAny.isEmpty() && excludeAny.containsAll(includeAny))
                || CollectionUtils.isIntersecting(excludeAny, includeAll)) {
            Logs.error(LOG, "AffinityStrategy is confusing: {}", affinityStrategy);
            return AFFINITY_STRATEGY_CONFUSING;
        }
        return "";
    }

    private boolean checkAffinityStrategyRange(
            List<Integer> excludeAny, List<Integer> includeAny,
            List<Integer> includeAll) {
        Predicate<Integer> outOfRange = val -> val < 0 || val > 31;
        return Stream.of(excludeAny, includeAny, includeAll).flatMap(List::parallelStream).anyMatch(outOfRange);
    }
}
