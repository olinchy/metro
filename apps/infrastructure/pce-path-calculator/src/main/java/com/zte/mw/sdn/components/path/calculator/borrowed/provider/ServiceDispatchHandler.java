/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.CreateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.CreateServiceOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.RemoveServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.UpdateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.UpdateServiceOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.create.service.input.SlaveTeArgument;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

import com.zte.ngip.ipsdn.pce.path.api.util.Logs;

/**
 * Created by 10087505 on 2017/6/14.
 */
public class ServiceDispatchHandler implements RpcCreateHandler<CreateServiceInput, CreateServiceOutput>,
        RpcRemoveHandler<RemoveServiceInput, Void>, RpcUpdateHandler<UpdateServiceInput, UpdateServiceOutput> {
    public ServiceDispatchHandler(PcePathHolder pathHolder) {
        this.pathHolder = pathHolder;
        this.servicePathHandler = new ServicePathHandler(pathHolder);
        this.serviceHsbHandler = new ServiceHsbPathHandler(pathHolder);
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServiceDispatchHandler.class);
    private PcePathHolder pathHolder;
    private ServicePathHandler servicePathHandler;
    private ServiceHsbPathHandler serviceHsbHandler;

    private static boolean isCreateHsbService(SlaveTeArgument slaveTeArgument) {
        return slaveTeArgument != null && slaveTeArgument.getNextAddress() != null && !slaveTeArgument.getNextAddress()
                .isEmpty();
    }

    @Override
    public Future<RpcResult<CreateServiceOutput>> create(CreateServiceInput input) {
        if (isCreateHsbService(input.getSlaveTeArgument())) {
            return serviceHsbHandler.create(input);
        }
        return servicePathHandler.create(input);
    }

    @Override
    public Future<RpcResult<Void>> remove(RemoveServiceInput input) {
        if (null != pathHolder.getServiceInstance(input.getHeadNodeId(), input.getServiceName())) {
            return servicePathHandler.remove(input);
        }
        if (null != pathHolder.getServiceHsbInstance(input.getHeadNodeId(), input.getServiceName())) {
            return serviceHsbHandler.remove(input);
        }
        Logs.info(LOG, "can no find service instance {}", input);
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<UpdateServiceOutput>> update(UpdateServiceInput input) {

        if (null != pathHolder.getServiceInstance(input.getHeadNodeId(), input.getServiceName())) {
            return servicePathHandler.update(input);
        }
        if (null != pathHolder.getServiceHsbInstance(input.getHeadNodeId(), input.getServiceName())) {
            return serviceHsbHandler.update(input);
        }
        Logs.info(LOG, "can no find service instance {}", input);
        return Futures.immediateFuture(RpcResultBuilder.<UpdateServiceOutput>success().build());
    }
}
