/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.algorithm.ConstrainedOptimalPathAlgorithmFactory;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.algorithm.ConstrainedOptimalPathAlgorithmFactoryImpl;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input.ConstrainedOptimalPathInput;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser.PathChooser;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.MultiplePathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;

/**
 * Created by 10204924 on 2017/2/7.
 */
public class ContrainedMultiplePathsProvider extends AbstractMultiplePathsProvider {
    public ContrainedMultiplePathsProvider(ConstrainedOptimalPathInput<NodeId, Link> input) {
        super(input);
    }

    @Override
    public ConstrainedOptimalPathAlgorithmFactory<NodeId, Link> createOptimalPathAlgorithmFactory() {
        return new ConstrainedOptimalPathAlgorithmFactoryImpl();
    }

    @Override
    public PathResult calcMultiplePathsAndChooseOne(int maxK, PathChooser<PathResult> chooser) {
        return calcMultiplePaths(maxK, chooser).get(0);
    }

    @Override
    public List<PathResult> calcMultiplePaths(int maxK, PathChooser<PathResult> chooser) {
        MultiplePathResult<PathResult> multiplePathResult =
                this.multiplePathsAlgorithm.calcMultiplePaths(maxK, chooser);

        return chooser.getChosenList(multiplePathResult);
    }
}
