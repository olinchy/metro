/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.common.rev170601.service.master.path.ServiceMasterPathLink;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.common.rev170601.service.master.path.ServiceMasterPathLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.CreateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.CreateServiceOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.CreateServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.RemoveServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.UpdateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.UpdateServiceOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.UpdateServiceOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.zte.mw.sdn.components.path.calculator.borrowed.servicepath.ServicePathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.RpcReturnUtils;

import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;

import static com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider.CREATE_PATH_UNSUCCESSFULLY;

public class ServicePathHandler implements RpcCreateHandler<CreateServiceInput, CreateServiceOutput>,
        RpcRemoveHandler<RemoveServiceInput, Void>, RpcUpdateHandler<UpdateServiceInput, UpdateServiceOutput> {
    public ServicePathHandler(PcePathHolder pathHolder) {
        this.pathHolder = pathHolder;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServicePathHandler.class);
    private PcePathHolder pathHolder;

    private static boolean needRollback(Boolean isSaveCreateFail) {
        return isSaveCreateFail != null && !isSaveCreateFail;
    }

    private static CreateServiceOutput buildCreateServiceResult(
            ServicePathInstance servicePathInstance,
            CalcResult calcResult) {
        boolean isSuccess = false;
        if (servicePathInstance.getPath() != null && !servicePathInstance.getPath().isEmpty()) {
            isSuccess = true;
        }

        PceUtil.logTopoBandWidthInfo(servicePathInstance.isSimulate());
        return new CreateServiceOutputBuilder().setSuccessful(isSuccess)
                .setServiceMasterPathLink(buildMasterPath(servicePathInstance))
                .setFailReason(calcResult.getCalcFailType()).build();
    }

    private static ServiceMasterPathLink buildMasterPath(ServicePathInstance servicePathInstance) {
        return new ServiceMasterPathLinkBuilder().setPathLink(PceUtil.transform2PathLink(servicePathInstance.getPath()))
                .setLspDelay(servicePathInstance.getLspDelay()).setLspMetric(servicePathInstance.getLspMetric())
                .build();
    }

    @Override
    public Future<RpcResult<CreateServiceOutput>> create(CreateServiceInput input) {
        ServicePathInstance servicePathInstance =
                pathHolder.getServiceInstance(input.getHeadNodeId(), input.getServiceName());

        if (servicePathInstance == null) {
            Logs.info(LOG, "Create Service instance {} {}", input.getHeadNodeId(), input.getServiceName());
            ServicePathInstance newServiceInstance = new ServicePathInstance(input);
            boolean isFailRollback = needRollback(input.isSaveCreateFail());

            ListenableFuture<CalcResult> future = Futures.transform(
                    newServiceInstance.calcPathAsync(isFailRollback),
                    new CreateServicePathCallBack(input, newServiceInstance));

            return Futures.transform(
                    future,
                    (AsyncFunction<CalcResult, RpcResult<CreateServiceOutput>>) calcResult -> Futures.immediateFuture(
                            RpcResultBuilder.success(buildCreateServiceResult(newServiceInstance, calcResult))
                                    .build()));
        } else {
            Logs.info(LOG, "No need create Service instance {} {}", input.getHeadNodeId(), input.getServiceName());
            return Futures.immediateFuture(
                    RpcResultBuilder.success(buildCreateServiceResult(servicePathInstance, new CalcResult())).build());
        }
    }

    @Override
    public Future<RpcResult<Void>> remove(RemoveServiceInput input) {
        ServicePathInstance serviceInstance =
                pathHolder.getServiceInstance(input.getHeadNodeId(), input.getServiceName());
        if (serviceInstance != null) {
            serviceInstance.destroy();
            pathHolder.removeServiceInstance(input.getHeadNodeId(), input.getServiceName());
            serviceInstance.removeDb();
        }

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<UpdateServiceOutput>> update(UpdateServiceInput input) {
        if (input.getNextAddress() == null || input.getNextAddress().isEmpty()) {
            return RpcReturnUtils.returnErr(CREATE_PATH_UNSUCCESSFULLY);
        }
        PceResult pceResult;
        ServicePathInstance serviceInstance =
                pathHolder.getServiceInstance(input.getHeadNodeId(), input.getServiceName());
        if (serviceInstance != null) {
            pceResult = isRollBack(input.isRollBack())
                    ? serviceInstance.updateWithRollback(input) :
                    serviceInstance.updateWithoutRollback(input);
            pathHolder.writeService(serviceInstance);
        } else {
            return RpcReturnUtils.returnErr(CREATE_PATH_UNSUCCESSFULLY);
        }
        if (pceResult.isCalcFail()) {
            Logs.info(LOG, "update hsb tunnel Fail");
            return RpcReturnUtils.returnErr(CREATE_PATH_UNSUCCESSFULLY);
        }

        UpdateServiceOutput updateServiceOutput = new UpdateServiceOutputBuilder()
                .setSuccessful(serviceInstance.getPath() != null && !serviceInstance.getPath().isEmpty())
                .setServiceMasterPathLink(buildMasterPath(serviceInstance)).setFailReason(pceResult.getFailReason())
                .build();
        return Futures.immediateFuture(RpcResultBuilder.success(updateServiceOutput).build());
    }

    private class CreateServicePathCallBack implements AsyncFunction<PceResult, CalcResult> {
        CreateServicePathCallBack(CreateServiceInput input, ServicePathInstance servicePathInstance) {
            this.input = input;
            this.servicePathInstance = servicePathInstance;
        }

        CreateServiceInput input;
        ServicePathInstance servicePathInstance;

        @Override
        public ListenableFuture<CalcResult> apply(PceResult pceResult) throws Exception {
            CalcResult calcResult = new CalcResult();

            if (input.isSaveCreateFail() != null && !input.isSaveCreateFail() && CollectionUtils
                    .isNullOrEmpty(servicePathInstance.getPath())) {
                Logs.info(LOG, "CreateServicePathCallBack  no need write", input.getHeadNodeId(),
                          input.getServiceName());
            } else {
                pathHolder.writeService(servicePathInstance);
                calcResult.setCalcFailType(pceResult.getCalcFailType());
            }

            Logs.debug(LOG, TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());
            PceUtil.logTopoBandWidthInfo(input.isSimulateTunnel());
            return Futures.immediateFuture(calcResult);
        }
    }
}
