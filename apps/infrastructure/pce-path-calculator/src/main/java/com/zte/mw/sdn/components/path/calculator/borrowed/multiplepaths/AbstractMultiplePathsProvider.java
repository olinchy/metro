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
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.algorithm.MultiplePathsAlgorithm;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.algorithm.MultiplePathsAlgorithmFactoryImpl;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input.ConstrainedOptimalPathInput;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser.PathChooser;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;

/**
 * Created by 10204924 on 2017/2/3.
 */
public abstract class AbstractMultiplePathsProvider {
    AbstractMultiplePathsProvider(ConstrainedOptimalPathInput<NodeId, Link> input) {
        this.input = input;
        this.multiplePathsAlgorithm = new MultiplePathsAlgorithmFactoryImpl()
                .create(input, createOptimalPathAlgorithmFactory());
    }

    public static final int MAX_K = Integer.MAX_VALUE;
    protected ConstrainedOptimalPathInput<NodeId, Link> input;
    MultiplePathsAlgorithm<PathResult> multiplePathsAlgorithm;

    /**
     * Get the factory of a Shortest Path Algorithm.
     *
     * @return the factory
     */
    public abstract ConstrainedOptimalPathAlgorithmFactory<NodeId, Link> createOptimalPathAlgorithmFactory();

    /**
     * Return one path result from chosen list(get by calcMultiplePaths method).
     *
     * @param maxK    maxK The upper limit to how many paths the KSP algorithm will try to calculate out.
     * @param chooser chooser Define which path(s) should be chosen.
     * @return PathResult
     */
    public abstract PathResult calcMultiplePathsAndChooseOne(int maxK, PathChooser<PathResult> chooser);

    /**
     * Try to calculate multiple paths and choose some by chooser.
     *
     * @param maxK    maxK The upper limit to how many paths the KSP algorithm will try to calculate out.
     * @param chooser chooser Define which path(s) should be chosen.
     * @return PathResult
     */
    public abstract List<PathResult> calcMultiplePaths(int maxK, PathChooser<PathResult> chooser);
}
