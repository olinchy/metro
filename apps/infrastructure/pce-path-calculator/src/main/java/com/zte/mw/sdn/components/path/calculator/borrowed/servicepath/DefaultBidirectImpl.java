/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.servicepath;

import java.util.Collections;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.BiDirectArgument;

import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;

/**
 * Created by 10204924 on 2017/7/6.
 */
public class DefaultBidirectImpl extends BiDirect {
    public DefaultBidirectImpl(BiDirectArgument biDirectArg) {
        super(biDirectArg, false);
    }

    @Override
    public List<Link> getPositivePathById(TunnelUnifyKey positiveTunnel) {
        return Collections.emptyList();
    }

    @Override
    public BiDirect getBiDirectById(NodeId tail, long reverseId) {
        return null;
    }
}
