/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateSlaveTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateSlaveTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateSlaveTunnelPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelHsbPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelHsbPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelHsbPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbWithoutRollbackInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbWithoutRollbackOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbWithoutRollbackOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.slave.tunnel.path.output.SlavePreemptedTunnelsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.slave.tunnel.path.output.SlaveTunnelPathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.slave.tunnel.path.output.SlaveTunnelPathSegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.tunnel.hsb.path.output.HsbPreemptedTunnelsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.tunnel.hsb.path.output.MasterPathSegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.tunnel.hsb.path.output.SlavePathSegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.tunnel.hsb.path.output.TunnelMasterPathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.tunnel.hsb.path.output.TunnelSlavePathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.links.PathLink;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.preepted.tunnels.PreemptedTunnel;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.segments.Segment;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelhsbpath.TunnelHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathKey;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.RpcReturnUtils;

import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Conditions;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;

import static com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider.CREATE_PATH_UNSUCCESSFULLY;

/**
 * Created by 10204924 on 2017/5/15.
 */
public class TunnelHsbPathHandler implements RpcCreateHandler<CreateTunnelHsbPathInput, CreateTunnelHsbPathOutput>,
        RpcCreateSlaveHandler<CreateSlaveTunnelPathInput, CreateSlaveTunnelPathOutput>,
        RpcUpdateHandler<UpdateTunnelHsbPathInput, UpdateTunnelHsbPathOutput>,
        RpcUpdateWithoutRollbackHandler<UpdateTunnelHsbWithoutRollbackInput, UpdateTunnelHsbWithoutRollbackOutput> {
    TunnelHsbPathHandler(PcePathHolder pathHolder, PcePathRefreshHandler refreshHandler) {
        this.pathHolder = pathHolder;
        this.refreshHandler = refreshHandler;
    }

    private static final Logger LOG = LoggerFactory.getLogger(TunnelHsbPathHandler.class);
    private PcePathHolder pathHolder;
    private PcePathRefreshHandler refreshHandler;

    private static boolean needRollback(Boolean isSaveCreateFail) {
        return isSaveCreateFail != null && !isSaveCreateFail;
    }

    private static CreateTunnelHsbPathOutput buildHsbCreateResult(
            TunnelHsbPathInstance path,
            CalcResult calcResult) {
        Logs.debug(LOG, "{} MASTER Path:{} SLAVE Path:{}", path.getTunnelUnifyKey(),
                   ComUtility.pathToString(path.getMasterLsp()),
                   ComUtility.pathToString(path.getSlaveLsp()));

        logTopoBandWidthInfo(path.isSimulate());
        //merge the positive and reverse preempted tunnels

        return new CreateTunnelHsbPathOutputBuilder().setTunnelMasterPath(
                new TunnelMasterPathBuilder()
                        .setPathLink(PceUtil.transform2PathLink(path.getMasterLsp()))
                        .setLspDelay(path.getMasterDelay())
                        .setLspMetric(path.getMasterMetric())
                        .build())
                .setTunnelSlavePath(
                        new TunnelSlavePathBuilder()
                                .setPathLink(PceUtil.transform2PathLink(path.getSlaveLsp()))
                                .setLspDelay(path.getSlaveDelay())
                                .setLspMetric(path.getSlaveMetric())
                                .build())
                .setMasterPathSegments(new MasterPathSegmentsBuilder()
                                               .setSegment(PceUtil.transformToSegments(
                                                       path.getMasterPathSegments())).build())
                .setSlavePathSegments(new SlavePathSegmentsBuilder()
                                              .setSegment(
                                                      PceUtil.transformToSegments(path.getSlavePathSegments())).build())
                .setHsbPreemptedTunnels(new HsbPreemptedTunnelsBuilder()
                                                .setPreemptedTunnel(calcResult.getTunnels()).build())
                .setCalcFailReason(calcResult.getCalcFailType())
                .build();
    }

    private static void logTopoBandWidthInfo(Boolean isSimulate) {
        if (ComUtility.isSimulateTunnel(isSimulate)) {
            Logs.debug(LOG, BandWidthMng.getInstance().getSimulateBandWidthString());
        } else {
            Logs.debug(LOG, BandWidthMng.getInstance().getBandWidthString());
        }
    }

    private static String checkForCreateSlaveTunnelPath(
            TunnelPathInstance masterTunnelPathInstance,
            TunnelHsbPathInstance msTunnelPathInstance) {
        String checkedResult = null;
        if (masterTunnelNotExist(masterTunnelPathInstance, msTunnelPathInstance)) {
            checkedResult = "createSlaveTunnel fail, because masterTunnel do not exist";
        } else if (masterTunnelCalcPathFail(masterTunnelPathInstance)) {
            checkedResult = "createSlaveTunnel fail, because masterTunnel exist but lsp==null";
        } else if (msTunnelMasterPathNull(msTunnelPathInstance)) {
            checkedResult = "createSlaveTunnel fail, because masterTunnel and msTunnel exist but lsp==null";
        }
        return checkedResult;
    }

    private static boolean masterTunnelNotExist(
            TunnelPathInstance masterTunnelPathInstance,
            TunnelHsbPathInstance msTunnelPathInstance) {
        return Conditions.allNull(masterTunnelPathInstance, msTunnelPathInstance);
    }

    private static boolean masterTunnelCalcPathFail(TunnelPathInstance masterTunnelPathInstance) {
        return masterTunnelPathInstance != null && CollectionUtils.isNullOrEmpty(masterTunnelPathInstance.getLsp());
    }

    private static boolean msTunnelMasterPathNull(TunnelHsbPathInstance msTunnelPathInstance) {
        return msTunnelPathInstance != null && CollectionUtils.isNullOrEmpty(msTunnelPathInstance.getMasterLsp());
    }

    @Override
    public Future<RpcResult<CreateTunnelHsbPathOutput>> create(CreateTunnelHsbPathInput input) {
        TunnelHsbPathInstance path = pathHolder
                .getTunnelHsbInstanceByKey(input.getHeadNodeId(), input.getTunnelId().intValue(),
                                           input.isSimulateTunnel());

        if (path == null) {
            TunnelHsbPathInstance newPath = new TunnelHsbPathInstance(input);
            boolean isFailRollback = (input.isSaveCreateFail() != null) && (!input.isSaveCreateFail());
            ListenableFuture<CalcResult> calcResult =
                    Futures.transform(
                            newPath.calcPathWithFailRollBackAsync(isFailRollback),
                            new CreateTunnelHsbPathAsyncFunction(input, newPath));
            return Futures.transform(
                    calcResult,
                    (AsyncFunction<CalcResult, RpcResult<CreateTunnelHsbPathOutput>>) result -> Futures
                            .immediateFuture(
                                    RpcResultBuilder.success(buildHsbCreateResult(newPath, result)).build()));
        } else {
            return Futures.immediateFuture(
                    RpcResultBuilder.success(buildHsbCreateResult(path, new CalcResult())).build());
        }
    }

    @Override
    public Future<RpcResult<CreateSlaveTunnelPathOutput>> createSlave(CreateSlaveTunnelPathInput input) {
        TunnelPathInstance masterTunnelPathInstance = pathHolder
                .getTunnelPathInstance(input.getHeadNodeId(), input.getTunnelId().intValue(), input.isSimulateTunnel());
        TunnelHsbPathInstance msTunnelPathInstance =
                pathHolder.getTunnelHsbInstanceByKey(input.getHeadNodeId(), input.getTunnelId().intValue(),
                                                     input.isSimulateTunnel());

        String checkedResult = checkForCreateSlaveTunnelPath(masterTunnelPathInstance, msTunnelPathInstance);
        if (checkedResult != null) {
            Logs.error(LOG, checkedResult);
            return Futures.immediateFuture(RpcResultBuilder.<CreateSlaveTunnelPathOutput>failed()
                                                   .withError(RpcError.ErrorType.APPLICATION, checkedResult).build());
        }

        List<PreemptedTunnel> outputPreemptedTunnels = new ArrayList<>();
        List<PreemptedTunnel> outputReversePreemptedTunnels = new ArrayList<>();

        if (null == msTunnelPathInstance) {
            msTunnelPathInstance = new TunnelHsbPathInstance(masterTunnelPathInstance, input);
            pathHolder.removeTunnelPathInstance(masterTunnelPathInstance);
            PceResult pceResult = msTunnelPathInstance.calcSlavePath();
            pathHolder.writeHsbTunnel(msTunnelPathInstance);
            outputPreemptedTunnels = pceResult.preemptedTunnelsProcess(false);
        }

        if (BiDirect.isBiDirectPositive(msTunnelPathInstance.getBiDirect())) {
            outputReversePreemptedTunnels = createReverseSlaveTunnel(input, msTunnelPathInstance);
        }
        msTunnelPathInstance.reNotifyTunnelPath();

        //merge the positive and reverse preempted tunnels
        outputPreemptedTunnels.addAll(outputReversePreemptedTunnels);

        CreateSlaveTunnelPathOutput output = new CreateSlaveTunnelPathOutputBuilder()
                .setSlaveTunnelPath(new SlaveTunnelPathBuilder()
                                            .setPathLink(PceUtil.transform2PathLink(msTunnelPathInstance.getSlaveLsp()))
                                            .setLspMetric(msTunnelPathInstance.getSlaveMetric())
                                            .build())
                .setSlavePreemptedTunnels(new SlavePreemptedTunnelsBuilder()
                                                  .setPreemptedTunnel(outputPreemptedTunnels)
                                                  .build())
                .setSlaveTunnelPathSegments(new SlaveTunnelPathSegmentsBuilder()
                                                    .setSegment(PceUtil.transformToSegments(
                                                            msTunnelPathInstance.getSlavePathSegments()))
                                                    .build())
                .build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    private List<PreemptedTunnel> createReverseSlaveTunnel(
            CreateSlaveTunnelPathInput input,
            TunnelHsbPathInstance msTunnelPathInstance) {
        TunnelPathKey reverseKey = new TunnelPathKey(input.getTailNodeId(), input.getTunnelId().intValue());
        TunnelHsbPathInstance hsbReversePath = pathHolder.getTunnelHsbPaths().get(reverseKey);
        TunnelPathInstance reverseMasterPath = pathHolder.getTunnelPaths().get(reverseKey);
        if (hsbReversePath == null) {
            hsbReversePath = new TunnelHsbPathInstance(msTunnelPathInstance);
            pathHolder.removeTunnelPath(reverseKey);
            pathHolder.putTunnelHsbPath(reverseKey, hsbReversePath);
            reverseMasterPath.destroy2Hotstandby();
            PceResult reversePceResult = hsbReversePath.calcPathWithNoFailrollBackAndShareGroup();
            hsbReversePath.writeDb();
            reverseMasterPath.removeDb();
            return reversePceResult.preemptedTunnelsProcess(false);
        } else {
            LOG.error("Reverse tunnel:{} is exist in tunnelHsbPaths", reverseKey);
            return new ArrayList<>();
        }
    }

    @Override
    public Future<RpcResult<UpdateTunnelHsbPathOutput>> update(UpdateTunnelHsbPathInput input) {
        logTopoBandWidthInfo(input.isSimulateTunnel());

        TunnelHsbPathInstance path = pathHolder
                .getTunnelHsbInstanceByKey(input.getHeadNodeId(), input.getTunnelId().intValue(),
                                           input.isSimulateTunnel());
        if (path == null) {
            return RpcReturnUtils.returnErr(CREATE_PATH_UNSUCCESSFULLY);
        }
        List<PathLink> masterPath = null;
        List<PathLink> slavePath = null;
        List<Segment> masterSegments = Collections.emptyList();
        List<Segment> slaveSegments = Collections.emptyList();
        long masterLspDelay = 0;
        long slaveLspDelay = 0;
        long masterLspMetric = 0;
        long slaveLspMetric = 0;
        List<PreemptedTunnel> outputPreemptedTunnels = null;

        PceResult pceResult = path.update(input);
        if (!pceResult.isCalcFail()) {
            pathHolder.writeHsbTunnel(path);
            outputPreemptedTunnels = pceResult.preemptedTunnelsProcess(false);
            if (pceResult.isNeedRefreshUnestablishTunnels()) {
                refreshHandler.refreshUnestablishTunnels(ComUtility.isSimulateTunnel(input.isSimulateTunnel()),
                                                         path.getTopoId(), path.getMasterTunnelUnifyKey(),
                                                         path.getHoldPriority(), null);
            }
            masterPath = PceUtil.transform2PathLink(path.getMasterLsp());
            slavePath = PceUtil.transform2PathLink(path.getSlaveLsp());
            masterSegments = PceUtil.transformToSegments(path.getMasterPathSegments());
            slaveSegments = PceUtil.transformToSegments(path.getSlavePathSegments());
            masterLspDelay = path.getMasterDelay();
            masterLspMetric = path.getMasterMetric();
            slaveLspDelay = path.getSlaveDelay();
            slaveLspMetric = path.getSlaveMetric();
        } else {
            LOG.info("updateTunnelHsbPath fail!");
        }
        path.reNotifyTunnelPath();
        Logs.debug(LOG, "MASTER Path:{}", ComUtility.pathToString(path.getMasterLsp()));
        Logs.debug(LOG, "SLAVE Path:{}", ComUtility.pathToString(path.getSlaveLsp()));
        logTopoBandWidthInfo(input.isSimulateTunnel());

        UpdateTunnelHsbPathOutput output = new UpdateTunnelHsbPathOutputBuilder()
                .setTunnelMasterPath(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                        .tunnel.hsb.path.output.TunnelMasterPathBuilder()
                                             .setLspMetric(masterLspMetric)
                                             .setLspDelay(masterLspDelay)
                                             .setPathLink(masterPath)
                                             .build())
                .setTunnelSlavePath(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                        .tunnel.hsb.path.output.TunnelSlavePathBuilder()
                                            .setLspDelay(slaveLspDelay)
                                            .setLspMetric(slaveLspMetric)
                                            .setPathLink(slavePath)
                                            .build())
                .setMasterPathSegments(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                        .tunnel.hsb.path.output.MasterPathSegmentsBuilder()
                                               .setSegment(masterSegments).build())
                .setSlavePathSegments(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                        .tunnel.hsb.path.output.SlavePathSegmentsBuilder()
                                              .setSegment(slaveSegments).build())
                .setHsbPreemptedTunnels(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                        .tunnel.hsb.path.output.HsbPreemptedTunnelsBuilder()
                                                .setPreemptedTunnel(outputPreemptedTunnels)
                                                .build())
                .setCalcFailReason(pceResult.getCalcFailType())
                .build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<UpdateTunnelHsbWithoutRollbackOutput>> updateWithoutRollback(
            UpdateTunnelHsbWithoutRollbackInput input) {
        TunnelHsbPathInstance path = pathHolder
                .getTunnelHsbInstanceByKey(input.getHeadNodeId(), input.getTunnelId().intValue(),
                                           input.isSimulateTunnel());

        if (path == null) {
            return RpcReturnUtils.returnErr(CREATE_PATH_UNSUCCESSFULLY);
        }

        PceResult pceResult =
                path.updateWithoutRollback(input, input.getSlaveTeArgument(), input.getBwSharedGroupContainer());
        List<PreemptedTunnel> outputPreemptedTunnels = null;
        if (!pceResult.isCalcFail()) {
            pathHolder.writeHsbTunnel(path);
            outputPreemptedTunnels = pceResult.preemptedTunnelsProcess(false);
            if (pceResult.isNeedRefreshUnestablishTunnels()) {
                PcePathProvider.getInstance().refreshUnestablishTunnels(path.isSimulate(), path.getTopoId(),
                                                                        path.getMasterTunnelUnifyKey(),
                                                                        path.getHoldPriority());
            }
        }
        path.reNotifyTunnelPath();
        UpdateTunnelHsbWithoutRollbackOutput output = new UpdateTunnelHsbWithoutRollbackOutputBuilder()
                .setTunnelMasterPath(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                        .tunnel.hsb.without.rollback.output.TunnelMasterPathBuilder()
                                             .setPathLink(PceUtil.transform2PathLink(path.getMasterLsp()))
                                             .setLspDelay(path.getMasterDelay())
                                             .setLspMetric(path.getMasterMetric())
                                             .build())
                .setTunnelSlavePath(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                        .tunnel.hsb.without.rollback.output.TunnelSlavePathBuilder()
                                            .setLspDelay(path.getSlaveDelay())
                                            .setLspMetric(path.getSlaveMetric())
                                            .setPathLink(PceUtil.transform2PathLink(path.getSlaveLsp()))
                                            .build())
                .setMasterPathSegments(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                        .tunnel.hsb.without.rollback.output.MasterPathSegmentsBuilder()
                                               .setSegment(PceUtil.transformToSegments(
                                                       path.getMasterPathSegments())).build())
                .setSlavePathSegments(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                        .tunnel.hsb.without.rollback.output.SlavePathSegmentsBuilder()
                                              .setSegment(
                                                      PceUtil.transformToSegments(path.getSlavePathSegments())).build())
                .setHsbPreemptedTunnels(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                        .tunnel.hsb.without.rollback.output.HsbPreemptedTunnelsBuilder()
                                                .setPreemptedTunnel(outputPreemptedTunnels)
                                                .build())
                .setCalcFailReason(pceResult.getCalcFailType())
                .build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    private class CreateTunnelHsbPathAsyncFunction implements AsyncFunction<PceResult, CalcResult> {
        CreateTunnelHsbPathAsyncFunction(CreateTunnelHsbPathInput input, TunnelHsbPathInstance tunnel) {
            this.input = input;
            this.tunnel = tunnel;
        }

        CreateTunnelHsbPathInput input;
        TunnelHsbPathInstance tunnel;

        @Override
        public ListenableFuture<CalcResult> apply(PceResult pceResult) throws Exception {
            boolean calcFailSaveFlag = true;
            CalcResult calcResult = new CalcResult();
            List<PreemptedTunnel> outputPreemptedTunnels = new ArrayList<>();
            if (needRollback(input.isSaveCreateFail()) && CollectionUtils.isNullOrEmpty(tunnel.getMasterLsp())) {
                //do nothing
                calcFailSaveFlag = false;
            } else {
                pathHolder.writeHsbTunnel(tunnel);
            }

            outputPreemptedTunnels.addAll(pceResult.preemptedTunnelsProcess(false));
            calcResult.setTunnels(outputPreemptedTunnels);
            calcResult.setCalcFailType(pceResult.getCalcFailType());
            if (BiDirect.isBiDirectPositive(tunnel.getBiDirect()) && calcFailSaveFlag) {
                return Futures.transform(
                        createReverseHsbTunnel(input, tunnel),
                        (AsyncFunction<List<PreemptedTunnel>, CalcResult>) tunnels -> {
                            outputPreemptedTunnels.addAll(tunnels);
                            calcResult.setTunnels(outputPreemptedTunnels);
                            tunnel.reNotifyTunnelPath();
                            return Futures.immediateFuture(calcResult);
                        });
            } else {
                tunnel.reNotifyTunnelPath();
                return Futures.immediateFuture(calcResult);
            }
        }

        private ListenableFuture<List<PreemptedTunnel>> createReverseHsbTunnel(
                CreateTunnelHsbPathInput input,
                TunnelHsbPathInstance tunnelHsb) {
            TunnelHsbPathInstance reversePath = pathHolder
                    .getTunnelHsbInstanceByKey(input.getTailNodeId(), (int) tunnelHsb.getBiDirect().getReverseId(),
                                               input.isSimulateTunnel());
            if (reversePath == null) {
                TunnelHsbPathInstance newReversePath = new TunnelHsbPathInstance(tunnelHsb);
                return Futures.transform(
                        newReversePath.calcPathWithNoFailrollBackAndShareGroupAsync(),
                        (AsyncFunction<PceResult, List<PreemptedTunnel>>) reversePceResult -> {
                            pathHolder.writeHsbTunnel(newReversePath);
                            return Futures.immediateFuture(reversePceResult.preemptedTunnelsProcess(false));
                        });
            } else {
                LOG.error("Reverse tunnel:{} {} is exist in tunnelHsbPaths", input.getTailNodeId(),
                          tunnelHsb.getBiDirect().getReverseId());
                return Futures.immediateFuture(new ArrayList<>());
            }
        }
    }
}
