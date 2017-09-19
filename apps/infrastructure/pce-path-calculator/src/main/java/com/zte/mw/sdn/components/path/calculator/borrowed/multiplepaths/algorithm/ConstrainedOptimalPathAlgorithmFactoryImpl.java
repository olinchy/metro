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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input.ConstrainedOptimalPathInput;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.ContrainedOptimalPath;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;

/**
 * Created by 10204924 on 2017/6/3.
 */
public class ConstrainedOptimalPathAlgorithmFactoryImpl
        implements ConstrainedOptimalPathAlgorithmFactory<NodeId, Link> {
    private static final Logger LOG = LoggerFactory.getLogger(ConstrainedOptimalPathAlgorithmFactoryImpl.class);

    @Override
    public ConstrainedOptimalPathAlgorithm<NodeId, Link> create(ConstrainedOptimalPathInput<NodeId, Link> input) {
        Logs.debug(LOG, "{}", input.getTopologyId());
        ContrainedOptimalPath csp = new ContrainedOptimalPath(input.getHeadNodeId(), input.getTailNodeId(),
                                                              TopoServiceAdapter.getInstance().getPceTopoProvider()
                                                                      .getTopoGraph(
                                                                              ComUtility.getSimulateFlag(
                                                                                      input.getTunnelUnifyKey()),
                                                                              input.getTopologyId()),
                                                              input.getTunnelUnifyKey(), input.getCalcStrategy());

        csp.setTeArgument(input.getTeArgumentBean());
        csp.setEdgeMetric(input.getTransformer());
        csp.setBiDirect(input.getBiDirect());
        csp.setExcludePath(input.getExcludedLinks());
        csp.setBwSharedGroups(input.getBwSharedGroups(), input.getDeletedBwSharedGroups());
        csp.setNeedBandScaled(input.isNeedScaleBandwidth());
        return csp;
    }
}
