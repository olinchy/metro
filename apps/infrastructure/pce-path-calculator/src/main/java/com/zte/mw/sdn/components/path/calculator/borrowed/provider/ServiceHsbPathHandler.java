/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.util.List;
import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.common.rev170601.service.master.path.ServiceMasterPathLink;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.common.rev170601.service.master.path.ServiceMasterPathLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.common.rev170601.service.slave.path.ServiceSlavePathLink;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.common.rev170601.service.slave.path.ServiceSlavePathLinkBuilder;
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

import com.zte.mw.sdn.components.path.calculator.borrowed.servicepath.ServiceHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.RpcReturnUtils;

import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;

import static com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider.CREATE_PATH_UNSUCCESSFULLY;

public class ServiceHsbPathHandler implements RpcCreateHandler<CreateServiceInput, CreateServiceOutput>,
        RpcRemoveHandler<RemoveServiceInput, Void>, RpcUpdateHandler<UpdateServiceInput, UpdateServiceOutput> {
    public ServiceHsbPathHandler(PcePathHolder pathHolder) {
        this.pathHolder = pathHolder;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServiceHsbPathHandler.class);
    private PcePathHolder pathHolder;

    private static boolean needRollback(Boolean isSaveCreateFail) {
        return isSaveCreateFail != null && !isSaveCreateFail;
    }

    private static CreateServiceOutput buildCreateServiceResult(
            ServiceHsbPathInstance serviceHsbPathInstance,
            CalcResult calcResult) {
        boolean isSuccessful =
                isPathOk(serviceHsbPathInstance.getMasterPath()) && isPathOk(serviceHsbPathInstance.getSlavePath());
        PceUtil.logLspPath(serviceHsbPathInstance.getMasterPath());
        PceUtil.logTopoBandWidthInfo(serviceHsbPathInstance.isSimulate());
        return new CreateServiceOutputBuilder().setSuccessful(isSuccessful)
                .setServiceMasterPathLink(buildMasterPath(serviceHsbPathInstance))
                .setServiceSlavePathLink(buildSlavePath(serviceHsbPathInstance))
                .setFailReason(calcResult.getCalcFailType()).build();
    }

    private static boolean isPathOk(List<Link> path) {
        return path != null && !path.isEmpty();
    }

    private static ServiceMasterPathLink buildMasterPath(ServiceHsbPathInstance servicePathInstance) {
        return new ServiceMasterPathLinkBuilder()
                .setPathLink(PceUtil.transform2PathLink(servicePathInstance.getMasterLsp()))
                .setLspDelay(servicePathInstance.getMasterDelay()).setLspMetric(servicePathInstance.getMasterMetric())
                .build();
    }

    private static ServiceSlavePathLink buildSlavePath(ServiceHsbPathInstance servicePathInstance) {
        return new ServiceSlavePathLinkBuilder()
                .setPathLink(PceUtil.transform2PathLink(servicePathInstance.getSlaveLsp()))
                .setLspDelay(servicePathInstance.getSlaveDelay()).setLspMetric(servicePathInstance.getSlaveMetric())
                .build();
    }

    private static boolean updateInputCheck(UpdateServiceInput input) {
        if (input.getNextAddress() == null || input.getNextAddress().isEmpty()) {
            return false;
        }
        if (input.getSlaveTeArgument() == null || input.getSlaveTeArgument().getNextAddress() == null || input
                .getSlaveTeArgument().getNextAddress().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public Future<RpcResult<CreateServiceOutput>> create(CreateServiceInput input) {
        ServiceHsbPathInstance serviceHsbPathInstance =
                pathHolder.getServiceHsbInstance(input.getHeadNodeId(), input.getServiceName());

        if (serviceHsbPathInstance == null) {
            Logs.info(LOG, "Create Hsb Service instance {} {}", input.getHeadNodeId(), input.getServiceName());
            ServiceHsbPathInstance newServiceInstance = new ServiceHsbPathInstance(input);
            boolean isFailRollback = needRollback(input.isSaveCreateFail());

            ListenableFuture<CalcResult> future =
                    Futures.transform(
                            newServiceInstance.calcPathWithFailRollBackAsync(isFailRollback),
                            new CreateServiceInstanceCallBack(input, newServiceInstance));

            return Futures.transform(
                    future,
                    (AsyncFunction<CalcResult, RpcResult<CreateServiceOutput>>) calcResult -> Futures.immediateFuture(
                            RpcResultBuilder.success(buildCreateServiceResult(newServiceInstance, calcResult))
                                    .build()));
        } else {
            Logs.info(LOG, "No need create Hsb Service instance {} {}", input.getHeadNodeId(), input.getServiceName());
            return Futures.immediateFuture(
                    RpcResultBuilder.success(buildCreateServiceResult(serviceHsbPathInstance, new CalcResult()))
                            .build());
        }
    }

    @Override
    public Future<RpcResult<Void>> remove(RemoveServiceInput input) {
        ServiceHsbPathInstance serviceInstance =
                pathHolder.getServiceHsbInstance(input.getHeadNodeId(), input.getServiceName());
        if (serviceInstance != null) {
            serviceInstance.destroy();
            pathHolder.removeServiceHsbInstance(input.getHeadNodeId(), input.getServiceName());
            serviceInstance.removeDb();
        }

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<UpdateServiceOutput>> update(UpdateServiceInput input) {
        if (!updateInputCheck(input)) {
            return RpcReturnUtils.returnErr(CREATE_PATH_UNSUCCESSFULLY);
        }
        PceResult pceResult;
        ServiceHsbPathInstance serviceInstance =
                pathHolder.getServiceHsbInstance(input.getHeadNodeId(), input.getServiceName());
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
        boolean isSuccessful = isPathOk(serviceInstance.getMasterPath()) && isPathOk(serviceInstance.getSlavePath());
        UpdateServiceOutput updateServiceOutput =
                new UpdateServiceOutputBuilder().setServiceMasterPathLink(buildMasterPath(serviceInstance))
                        .setServiceSlavePathLink(buildSlavePath(serviceInstance)).setSuccessful(isSuccessful)
                        .setFailReason(pceResult.getFailReason()).build();
        return Futures.immediateFuture(RpcResultBuilder.success(updateServiceOutput).build());
    }

    private class CreateServiceInstanceCallBack implements AsyncFunction<PceResult, CalcResult> {
        CreateServiceInstanceCallBack(CreateServiceInput input, ServiceHsbPathInstance serviceHsbPathInstance) {
            this.input = input;
            this.serviceHsbPathInstance = serviceHsbPathInstance;
        }

        CreateServiceInput input;
        ServiceHsbPathInstance serviceHsbPathInstance;

        @Override
        public ListenableFuture<CalcResult> apply(PceResult pceResult) throws Exception {
            CalcResult calcResult = new CalcResult();

            if (input.isSaveCreateFail() != null && !input.isSaveCreateFail() && CollectionUtils
                    .isNullOrEmpty(serviceHsbPathInstance.getMasterPath())) {
                Logs.debug(LOG, "no need write service instance");
                calcResult.setCalcFailType(pceResult.getCalcFailType());
            } else {
                pathHolder.writeService(serviceHsbPathInstance);
                if (pceResult.isCalcFail()) {
                    calcResult.setCalcFailType(pceResult.getCalcFailType());
                }
            }
            Logs.debug(LOG, TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());
            PceUtil.logTopoBandWidthInfo(input.isSimulateTunnel());
            return Futures.immediateFuture(calcResult);
        }
    }
}
