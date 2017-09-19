/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.realtimepath;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathHolder;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelhsbpath.TunnelHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.RpcReturnUtils;

/**
 * Created by 10204924 on 2017/8/30.
 */
public class RealtimeDispatcher {
    private RealtimeDispatcher() {
    }

    public static Future<RpcResult<GetRealtimePathOutput>> dispatch(
            GetRealtimePathInput input,
            PcePathHolder pathHolder) {
        NodeId headNode = input.getHeadNodeId();
        Long tunnelId = input.getTunnelId();
        if (tunnelId == null || tunnelId == 0) {
            return new RealtimePathHandler().create(input);
        }
        TunnelPathInstance tunnelPathInstance = pathHolder.getTunnelInstance(headNode, tunnelId.intValue());
        if (tunnelPathInstance != null) {
            return new RealtimePathHandler().update(input);
        }
        TunnelHsbPathInstance tunnelHsbPathInstance = pathHolder.getTunnelHsbInstance(headNode, tunnelId.intValue());
        if (tunnelHsbPathInstance != null) {
            return new RealtimeHsbPathHandler().update(input);
        }
        return RpcReturnUtils.returnErr("Cannot find tunnel with id " + tunnelId);
    }
}
