/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.pathcore;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Function;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

import com.zte.ngip.ipsdn.pce.path.core.strategy.ICalcStrategy;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformer;

/**
 * Created by 10088483 on 5/2/16.
 */
public class MaxBandwidthStrategy<V, E> implements ICalcStrategy<V, E> {
    public MaxBandwidthStrategy(boolean isBiDirect, TopologyId topoId) {
        this.topoId = topoId;
        this.isBiDirect = isBiDirect;
    }

    private TopologyId topoId;
    private boolean isBiDirect;

    @Override
    public Function<ITransformer<E>, Double> getTransformFunction(E incomingEdge) {
        return transformer -> transformer.transform(incomingEdge, isBiDirect, topoId);
    }

    @Override
    public long getDefaultMeasure() {
        return 0;
    }

    @Override
    public boolean isCurNodeMoreOptimal(long curNodeMeasure, long incomingEdgeMeasure, long neighborNodeMeasure) {
        return incomingEdgeMeasure > neighborNodeMeasure;
    }

    @Override
    public Map.Entry<V, Number> getOptimalNodeInTentMap(NavigableMap<V, Number> tentMap) {
        return tentMap.lastEntry();
    }

    @Override
    public long transEdgeMeasure(long curNodeMeasure, long incomingEdgeMeasure) {
        return incomingEdgeMeasure;
    }

    @Override
    public int compare(Collection<E> edges1, Collection<E> edges2, ITransformer<E> transformer) {
        long positiveMeasure1 = getEdgesMeasure(transformer, edges1, true);
        long positiveMeasure2 = getEdgesMeasure(transformer, edges2, true);

        if (isBiDirect) {
            long reverseMeasure1 = getEdgesMeasure(transformer, edges1, false);
            long reverseMeasure2 = getEdgesMeasure(transformer, edges2, false);
            return (int) (Math.min(positiveMeasure1, reverseMeasure1) - Math.min(positiveMeasure2, reverseMeasure2));
        } else {
            return (int) (positiveMeasure1 - positiveMeasure2);
        }
    }

    @Override
    public long getEdgesMeasure(ITransformer<E> transformer, Collection<E> edges, boolean isPositive) {
        return edges.parallelStream().mapToLong(edge -> transformer.transformSingleDirection(edge, isPositive, topoId))
                .sum();
    }
}
