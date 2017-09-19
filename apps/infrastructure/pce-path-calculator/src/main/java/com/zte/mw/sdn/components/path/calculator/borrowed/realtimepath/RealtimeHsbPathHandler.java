/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.realtimepath;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.realtime.path.output.RealtimePathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.realtime.path.output.SlaveRealtimePathBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.RpcUpdateHandler;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelhsbpath.TunnelHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.RpcReturnUtils;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;

/**
 * Created by 10204924 on 2017/8/30.
 */
public class RealtimeHsbPathHandler implements RpcUpdateHandler<GetRealtimePathInput, GetRealtimePathOutput> {
    private static final Logger LOG = LoggerFactory.getLogger(RealtimeHsbPathHandler.class);

    @Override
    public Future<RpcResult<GetRealtimePathOutput>> update(GetRealtimePathInput input) {
        NodeId headNode = input.getHeadNodeId();
        int tunnelId = input.getTunnelId().intValue();
        TunnelHsbPathInstance hsbInstance =
                PcePathProvider.getInstance().getTunnelHsbInstance(headNode, tunnelId);

        RealtimeHsbPathInstance path = new RealtimeHsbPathInstance(input, hsbInstance);
        PceResult pceResult;
        try {
            pceResult = path.calcPathAsync(hsbInstance.getBwSharedGroups(), input.getBwSharedGroupContainer()).get();
        } catch (InterruptedException | ExecutionException e) {
            Logs.error(LOG, "calc realtime path for tunnel id {} exceptionally {}.", tunnelId, e);
            return RpcReturnUtils.returnErr("calc realtime path for tunnel id " + tunnelId + " exceptionally.");
        }
        Logs.debug(LOG, "MasterPath: " + ComUtility.pathToString(path.getPath()));
        Logs.debug(LOG, "SlavePath: {}", ComUtility.pathToString(path.getSlavePath()));

        long lspMetric = path.getLspMetric();
        long lspDelay = path.getLspDelay();
        long slaveLspMetric = path.getSlaveLspMetric();
        long slaveLspDelay = path.getSlaveLspDelay();
        GetRealtimePathOutput output = new GetRealtimePathOutputBuilder()
                .setRealtimePath(new RealtimePathBuilder().setLspMetric(lspMetric).setLspDelay(lspDelay)
                                         .setPathLink(PceUtil.transform2PathLink(path.getPath()))
                                         .build())
                .setSlaveRealtimePath(new SlaveRealtimePathBuilder().setLspMetric(slaveLspMetric)
                                              .setLspDelay(slaveLspDelay).setPathLink(
                                PceUtil.transform2PathLink(path.getSlavePath()))
                                              .build())
                .setCalcFailReason(pceResult.getCalcFailType())
                .build();
        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }
}
