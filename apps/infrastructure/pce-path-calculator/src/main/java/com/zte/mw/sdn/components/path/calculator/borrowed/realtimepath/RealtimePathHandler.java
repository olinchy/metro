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
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.RpcCreateHandler;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.RpcUpdateHandler;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;

/**
 * Created by 10204924 on 2017/8/30.
 */
public class RealtimePathHandler implements RpcCreateHandler<GetRealtimePathInput, GetRealtimePathOutput>,
        RpcUpdateHandler<GetRealtimePathInput, GetRealtimePathOutput> {
    private static final Logger LOG = LoggerFactory.getLogger(RealtimePathHandler.class);

    @Override
    public Future<RpcResult<GetRealtimePathOutput>> create(GetRealtimePathInput input) {
        GetRealtimePathOutput output;
        try {
            output = MoreExecutors.newDirectExecutorService().submit(() -> {
                RealtimePathInstance path = new RealtimePathInstance(input);
                PceResult pceResult = path.calcPath();
                long lspMetric = path.getLspMetric();
                long lspDelay = path.getLspDelay();
                LOG.debug("Path:" + ComUtility.pathToString(path.getLsp()));
                return new GetRealtimePathOutputBuilder().setRealtimePath(
                        new RealtimePathBuilder().setPathLink(PceUtil.transform2PathLink(path.getLsp()))
                                .setLspMetric(lspMetric).setLspDelay(lspDelay).build())
                        .setCalcFailReason(pceResult.getCalcFailType()).build();
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("getRealtimePath face exception {}", e);
            return Futures.immediateFailedFuture(e);
        }

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<GetRealtimePathOutput>> update(GetRealtimePathInput input) {
        NodeId headNode = input.getHeadNodeId();
        int tunnelId = input.getTunnelId().intValue();
        TunnelPathInstance tunnelPathInstance = PcePathProvider.getInstance().getTunnelInstance(headNode, tunnelId);

        RealtimePathInstance path = new RealtimePathInstance(input, tunnelPathInstance);
        PceResult pceResult = path.calcPath(tunnelPathInstance.getBwSharedGroups(), input.getBwSharedGroupContainer());

        long lspMetric = path.getLspMetric();
        long lspDelay = path.getLspDelay();
        Logs.debug(LOG, "Path:" + ComUtility.pathToString(path.getLsp()));
        GetRealtimePathOutput output = new GetRealtimePathOutputBuilder().setRealtimePath(
                new RealtimePathBuilder().setPathLink(PceUtil.transform2PathLink(path.getLsp())).setLspMetric(lspMetric)
                        .setLspDelay(lspDelay).build()).setCalcFailReason(pceResult.getCalcFailType()).build();
        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }
}
