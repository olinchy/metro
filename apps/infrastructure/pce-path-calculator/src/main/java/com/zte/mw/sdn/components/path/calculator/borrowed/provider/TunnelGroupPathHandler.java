/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelGroupPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelGroupPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelGroupPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelGroupPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelGroupPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelGroupPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.tunnel.group.path.output.TgMasterPathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.tunnel.group.path.output.TgSlavePathBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.TunnelGroupPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.TunnelGroupPathKey;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.RpcReturnUtils;

import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;

import static com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider.CREATE_PATH_UNSUCCESSFULLY;

/**
 * Created by 10204924 on 2017/5/15.
 */
public class TunnelGroupPathHandler
        implements RpcCreateHandler<CreateTunnelGroupPathInput, CreateTunnelGroupPathOutput>,
        RpcUpdateHandler<UpdateTunnelGroupPathInput, UpdateTunnelGroupPathOutput> {
    TunnelGroupPathHandler(PcePathHolder pathHolder) {
        this.pathHolder = pathHolder;
    }

    private static final Logger LOG = LoggerFactory.getLogger(TunnelGroupPathHandler.class);
    private PcePathHolder pathHolder;

    private static boolean needRollback(Boolean isSaveCreateFail) {
        return isSaveCreateFail != null && !isSaveCreateFail;
    }

    @Override
    public Future<RpcResult<CreateTunnelGroupPathOutput>> create(CreateTunnelGroupPathInput input) {
        TunnelGroupPathKey key = new TunnelGroupPathKey(input.getHeadNodeId(), input.getTunnelGroupId().intValue());
        TunnelGroupPathInstance path = pathHolder.getTunnelGroupInstance(key);
        if (path == null) {
            path = new TunnelGroupPathInstance(input);
            if (!needRollback(input.isSaveCreateFail()) || !CollectionUtils.isNullOrEmpty(path.getMasterPath())) {
                pathHolder.putTunnelGroupPath(key, path);
                path.writeDb();
            }
        }

        Logs.debug(LOG, "MASTER Path:{}", ComUtility.pathToString(path.getMasterLsp()));
        Logs.debug(LOG, "SLAVE Path:{}", ComUtility.pathToString(path.getSlaveLsp()));
        CreateTunnelGroupPathOutput output = new CreateTunnelGroupPathOutputBuilder().setTgMasterPath(
                new TgMasterPathBuilder().setPathLink(PceUtil.transform2PathLink(path.getMasterLsp())).build())
                .setTgSlavePath(
                        new TgSlavePathBuilder().setPathLink(PceUtil.transform2PathLink(path.getSlaveLsp())).build())
                .build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<UpdateTunnelGroupPathOutput>> update(UpdateTunnelGroupPathInput input) {
        TunnelGroupPathKey key = new TunnelGroupPathKey(input.getHeadNodeId(), input.getTunnelGroupId().intValue());
        TunnelGroupPathInstance path = pathHolder.getTunnelGroupInstance(key);
        if (path == null) {
            return RpcReturnUtils.returnErr(CREATE_PATH_UNSUCCESSFULLY);
        }

        PceResult pceResult = path.update(input, null, null, null);
        path.writeDb();
        if (pceResult.isNeedRefreshUnestablishTunnels()) {
            PcePathProvider.getInstance()
                    .refreshUnestablishTunnels(path.isSimulate(), path.getTopoId(), path.getMasterTunnelUnifyKey(),
                                               path.getHoldPriority());
        }

        UpdateTunnelGroupPathOutput output = new UpdateTunnelGroupPathOutputBuilder()
                .setTgMasterPath(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                        .tunnel.group.path.output.TgMasterPathBuilder()
                                         .setPathLink(PceUtil.transform2PathLink(path.getMasterLsp()))
                                         .build())
                .setTgSlavePath(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                        .tunnel.group.path.output.TgSlavePathBuilder()
                                        .setPathLink(PceUtil.transform2PathLink(path.getSlaveLsp()))
                                        .build())
                .build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }
}
