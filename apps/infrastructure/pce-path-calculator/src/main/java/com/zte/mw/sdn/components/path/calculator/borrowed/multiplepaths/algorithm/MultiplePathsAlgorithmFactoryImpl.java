/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.algorithm;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input.ConstrainedOptimalPathInput;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;

/**
 * Created by 10204924 on 2017/6/5.
 */
public class MultiplePathsAlgorithmFactoryImpl implements MultiplePathsAlgorithmFactory<PathResult, NodeId, Link> {
    @Override
    public MultiplePathsAlgorithm<PathResult> create(
            ConstrainedOptimalPathInput<NodeId, Link> input,
            ConstrainedOptimalPathAlgorithmFactory<NodeId, Link> factory) {
        return new YenKspImpl(input, factory);
    }
}
