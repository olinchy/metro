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

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathWithoutRollbackInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathWithoutRollbackOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathWithoutRollbackOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.tunnel.path.output.PreemptedTunnelsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.tunnel.path.output.TunnelPathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.create.tunnel.path.output.TunnelPathSegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.links.PathLink;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.preepted.tunnels.PreemptedTunnel;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.segments.Segment;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update.tunnel.path.output.PathSegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update.tunnel.path.output.SlavePathSegmentsBuilder;
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
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.RpcReturnUtils;

import com.zte.ngip.ipsdn.pce.path.api.segmentrouting.PathSegment;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Conditions;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;

import static com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider.CREATE_PATH_UNSUCCESSFULLY;

/**
 * Created by 10204924 on 2017/5/15.
 */
public class TunnelPathHandler implements RpcCreateHandler<CreateTunnelPathInput, CreateTunnelPathOutput>,
        RpcUpdateHandler<UpdateTunnelPathInput, UpdateTunnelPathOutput>,
        RpcUpdateWithoutRollbackHandler<UpdateTunnelPathWithoutRollbackInput, UpdateTunnelPathWithoutRollbackOutput> {
    TunnelPathHandler(PcePathHolder pathHolder) {
        this.pathHolder = pathHolder;
    }

    private static final Logger LOG = LoggerFactory.getLogger(TunnelPathHandler.class);
    private PcePathHolder pathHolder;

    private static boolean needRollback(Boolean isSaveCreateFail) {
        return isSaveCreateFail != null && !isSaveCreateFail;
    }

    private static CreateTunnelPathOutput buildCreateTunnelResult(
            TunnelPathInstance tunnel,
            TunnelHsbPathInstance msTunnelPathInstance, CalcResult calcResult) {
        List<Link> lsp;
        long lspMetric;
        long lspDelay;
        List<PathSegment> pathSegments;
        if (msTunnelPathInstance == null) {
            lsp = tunnel.getLsp();
            lspMetric = tunnel.getLspMetric();
            lspDelay = tunnel.getLspDelay();
            pathSegments = tunnel.getSegments();
            logLspPath(lsp);
            logTopoBandWidthInfo(tunnel.isSimulate());
        } else {
            lsp = msTunnelPathInstance.getMasterLsp();
            lspMetric = msTunnelPathInstance.getMasterMetric();
            lspDelay = msTunnelPathInstance.getMasterDelay();
            pathSegments = msTunnelPathInstance.getMasterPathSegments();
            logLspPath(lsp);
            logTopoBandWidthInfo(msTunnelPathInstance.isSimulate());
        }
        return new CreateTunnelPathOutputBuilder().setTunnelPath(
                new TunnelPathBuilder().setPathLink(PceUtil.transform2PathLink(lsp)).setLspMetric(lspMetric)
                        .setLspDelay(lspDelay).build())
                .setPreemptedTunnels(new PreemptedTunnelsBuilder().setPreemptedTunnel(calcResult.getTunnels()).build())
                .setCalcFailReason(calcResult.getCalcFailType()).setTunnelPathSegments(
                        new TunnelPathSegmentsBuilder().setSegment(PceUtil.transformToSegments(pathSegments)).build())
                .build();
    }

    private static void logLspPath(List<Link> lsp) {
        Logs.debug(LOG, "Lsp Path:{}", ComUtility.pathToString(lsp));
    }

    private static void logTopoBandWidthInfo(Boolean isSimulate) {
        if (ComUtility.isSimulateTunnel(isSimulate)) {
            Logs.debug(LOG, BandWidthMng.getInstance().getSimulateBandWidthString());
        } else {
            Logs.debug(LOG, BandWidthMng.getInstance().getBandWidthString());
        }
    }

    @Override
    public Future<RpcResult<CreateTunnelPathOutput>> create(CreateTunnelPathInput input) {
        TunnelPathInstance tunnel = PcePathProvider.getInstance()
                .getTunnelPathInstance(input.getHeadNodeId(), input.getTunnelId().intValue(), input.isSimulateTunnel());
        TunnelHsbPathInstance msTunnelPathInstance = PcePathProvider.getInstance()
                .getTunnelHsbInstanceByKey(input.getHeadNodeId(), input.getTunnelId().intValue(),
                                           input.isSimulateTunnel());
        if (tunnel == null && msTunnelPathInstance == null) {
            TunnelPathInstance newTunnel = new TunnelPathInstance(input);
            boolean isFailRollback = needRollback(input.isSaveCreateFail());

            ListenableFuture<CalcResult> future =
                    Futures.transform(
                            newTunnel.calcPathAsync(isFailRollback),
                            new CreateTunnelPathCallBack(input, newTunnel));

            return Futures.transform(
                    future,
                    (AsyncFunction<CalcResult, RpcResult<CreateTunnelPathOutput>>) calcResult -> Futures
                            .immediateFuture(
                                    RpcResultBuilder.success(buildCreateTunnelResult(newTunnel, null, calcResult))
                                            .build()));
        } else {
            Logs.error(LOG, "{} tunnel {} is exist", input.getHeadNodeId(), input.getTunnelId());
            return Futures.immediateFuture(RpcResultBuilder
                                                   .success(buildCreateTunnelResult(tunnel, msTunnelPathInstance,
                                                                                    new CalcResult()))
                                                   .build());
        }
    }

    @Override
    public Future<RpcResult<UpdateTunnelPathOutput>> update(UpdateTunnelPathInput input) {
        TunnelPathInstance path = pathHolder
                .getTunnelPathInstance(input.getHeadNodeId(), input.getTunnelId().intValue(), input.isSimulateTunnel());
        TunnelHsbPathInstance msTunnelPathInstance =
                pathHolder.getTunnelHsbInstanceByKey(input.getHeadNodeId(), input.getTunnelId().intValue(),
                                                     input.isSimulateTunnel());

        if (path != null) {
            PceResult pceResult = path.update(input);
            List<PathLink> pathLink = null;
            long metric = 0;
            long delay = 0;
            List<Segment> segments = Collections.emptyList();
            final List<PreemptedTunnel> outputPreemptedTunnels = pceResult.preemptedTunnelsProcess(false);
            if (!pceResult.isCalcFail()) {
                pathHolder.writeTunnel(path);
                Conditions.ifTrue(pceResult.isNeedRefreshUnestablishTunnels(), () -> PcePathProvider.getInstance()
                        .refreshUnestablishTunnels(path.isSimulate(), path.getTopoId(), path.getTunnelUnifyKey(),
                                                   path.getHoldPriority()));
                pathLink = PceUtil.transform2PathLink(path.getLsp());
                segments = PceUtil.transformToSegments(path.getSegments());
                metric = path.getLspMetric();
                delay = path.getLspDelay();
            } else {
                Logs.info(LOG, "updateTunnelPath fail!");
            }
            logLspPath(path.getLsp());
            logTopoBandWidthInfo(input.isSimulateTunnel());
            UpdateTunnelPathOutput output = new UpdateTunnelPathOutputBuilder()
                    .setTunnelPath(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                            .tunnel.path.output.TunnelPathBuilder()
                                           .setPathLink(pathLink)
                                           .setLspMetric(metric)
                                           .setLspDelay(delay)
                                           .build())
                    .setPathSegments(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                            .tunnel.path.output.PathSegmentsBuilder()
                                             .setSegment(segments).build())
                    .setPreemptedTunnels(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.update
                            .tunnel.path.output.PreemptedTunnelsBuilder()
                                                 .setPreemptedTunnel(outputPreemptedTunnels)
                                                 .build())
                    .setCalcFailReason(pceResult.getCalcFailType())
                    .build();
            path.reNotifyTunnelPath();
            return Futures.immediateFuture(RpcResultBuilder.success(output).build());
        } else if (msTunnelPathInstance != null) {

            PceResult result = msTunnelPathInstance.update(input);
            List<PathLink> masterPathLink = null;
            long masterMetric = 0;
            List<Segment> masterSegments = Collections.emptyList();
            List<PathLink> slavePathLink = null;
            long slaveMetric = 0;
            List<Segment> slaveSegments = Collections.emptyList();
            if (!result.isCalcFail()) {
                msTunnelPathInstance.writeDb();
                masterPathLink = PceUtil.transform2PathLink(msTunnelPathInstance.getMasterPath());
                masterSegments = PceUtil.transformToSegments(msTunnelPathInstance.getMasterPathSegments());
                masterMetric = msTunnelPathInstance.getMasterMetric();
                slavePathLink = PceUtil.transform2PathLink(msTunnelPathInstance.getSlavePath());
                slaveSegments = PceUtil.transformToSegments(msTunnelPathInstance.getSlavePathSegments());
                slaveMetric = msTunnelPathInstance.getSlaveMetric();
            }
            msTunnelPathInstance.reNotifyTunnelPath();
            List<PreemptedTunnel> outputPreemptedTunnels = result.preemptedTunnelsProcess(false);
            UpdateTunnelPathOutput output = new UpdateTunnelPathOutputBuilder().setTunnelPath(
                    new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                            .update.tunnel.path.output.TunnelPathBuilder()
                            .setPathLink(masterPathLink).setLspMetric(masterMetric).build()).setSlaveTunnelPath(
                    new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                            .update.tunnel.path.output.SlaveTunnelPathBuilder()
                            .setPathLink(slavePathLink).setLspMetric(slaveMetric).build()).setPreemptedTunnels(
                    new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                            .update.tunnel.path.output.PreemptedTunnelsBuilder()
                            .setPreemptedTunnel(outputPreemptedTunnels).build())
                    .setPathSegments(new PathSegmentsBuilder().setSegment(masterSegments).build())
                    .setSlavePathSegments(new SlavePathSegmentsBuilder().setSegment(slaveSegments).build())
                    .build();
            return Futures.immediateFuture(RpcResultBuilder.success(output).build());
        } else {
            return RpcReturnUtils.returnErr(CREATE_PATH_UNSUCCESSFULLY);
        }
    }

    @Override
    public Future<RpcResult<UpdateTunnelPathWithoutRollbackOutput>> updateWithoutRollback(
            UpdateTunnelPathWithoutRollbackInput input) {
        TunnelPathInstance path = pathHolder
                .getTunnelPathInstance(input.getHeadNodeId(), input.getTunnelId().intValue(), input.isSimulateTunnel());
        TunnelHsbPathInstance msTunnelPathInstance =
                pathHolder.getTunnelHsbInstanceByKey(input.getHeadNodeId(), input.getTunnelId().intValue(),
                                                     input.isSimulateTunnel());

        if (path == null && msTunnelPathInstance == null) {
            return RpcReturnUtils.returnErr(CREATE_PATH_UNSUCCESSFULLY);
        }

        if (path != null) {
            PceResult pceResult = path.updateWithoutRollback(input);
            path.writeDb();
            List<PreemptedTunnel> outputPreemptedTunnels = pceResult.preemptedTunnelsProcess(false);
            if (pceResult.isNeedRefreshUnestablishTunnels()) {
                PcePathProvider.getInstance()
                        .refreshUnestablishTunnels(path.isSimulate(), path.getTopoId(), path.getTunnelUnifyKey(),
                                                   path.getHoldPriority());
            }

            UpdateTunnelPathWithoutRollbackOutput output = new UpdateTunnelPathWithoutRollbackOutputBuilder()
                    .setTunnelPath(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                            .update.tunnel.path.without.rollback.output.TunnelPathBuilder()
                                           .setPathLink(PceUtil.transform2PathLink(path.getLsp()))
                                           .setLspMetric(path.getLspMetric())
                                           .setLspDelay(path.getLspDelay())
                                           .build())
                    .setPreemptedTunnels(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                            .update.tunnel.path.without.rollback.output.PreemptedTunnelsBuilder()
                                                 .setPreemptedTunnel(outputPreemptedTunnels)
                                                 .build())
                    .setPathSegments(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                            .update.tunnel.path.without.rollback.output.PathSegmentsBuilder()
                                             .setSegment(PceUtil.transformToSegments(path.getSegments())).build())
                    .setCalcFailReason(pceResult.getCalcFailType())
                    .build();
            path.reNotifyTunnelPath();
            return Futures.immediateFuture(RpcResultBuilder.success(output).build());
        }

        PceResult result = msTunnelPathInstance.updateWithoutRollback(input);
        msTunnelPathInstance.writeDb();
        msTunnelPathInstance.reNotifyTunnelPath();
        List<PreemptedTunnel> outputPreemptedTunnels = result.preemptedTunnelsProcess(false);
        UpdateTunnelPathWithoutRollbackOutput output = new UpdateTunnelPathWithoutRollbackOutputBuilder()
                .setTunnelPath(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                        .update.tunnel.path.without.rollback.output.TunnelPathBuilder()
                                       .setPathLink(PceUtil.transform2PathLink(msTunnelPathInstance.getMasterPath()))
                                       .setLspMetric(msTunnelPathInstance.getMasterMetric())
                                       .build())
                .setSlaveTunnelPath(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                        .update.tunnel.path.without.rollback.output.SlaveTunnelPathBuilder()
                                            .setPathLink(
                                                    PceUtil.transform2PathLink(msTunnelPathInstance.getSlavePath()))
                                            .setLspMetric(msTunnelPathInstance.getSlaveMetric())
                                            .build())
                .setPreemptedTunnels(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                        .update.tunnel.path.without.rollback.output.PreemptedTunnelsBuilder()
                                             .setPreemptedTunnel(outputPreemptedTunnels)
                                             .build())
                .setPathSegments(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                        .update.tunnel.path.without.rollback.output.PathSegmentsBuilder()
                                         .setSegment(PceUtil.transformToSegments(
                                                 msTunnelPathInstance.getMasterPathSegments())).build())
                .setSlavePathSegments(new org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                        .update.tunnel.path.without.rollback.output.SlavePathSegmentsBuilder()
                                              .setSegment(PceUtil.transformToSegments(
                                                      msTunnelPathInstance.getSlavePathSegments())).build())
                .build();
        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    private class CreateTunnelPathCallBack implements AsyncFunction<PceResult, CalcResult> {
        CreateTunnelPathCallBack(CreateTunnelPathInput input, TunnelPathInstance tunnel) {
            this.input = input;
            this.tunnel = tunnel;
        }

        CreateTunnelPathInput input;
        TunnelPathInstance tunnel;

        @Override
        public ListenableFuture<CalcResult> apply(PceResult pceResult) throws Exception {
            boolean needCreateReverse = true;
            CalcResult calcResult = new CalcResult();
            final List<PreemptedTunnel> outputPreemptedTunnels = new ArrayList<>();

            if (input.isSaveCreateFail() != null && !input.isSaveCreateFail() && CollectionUtils
                    .isNullOrEmpty(tunnel.getPath())) {
                needCreateReverse = false;
            } else {
                pathHolder.writeTunnel(tunnel);
                outputPreemptedTunnels.addAll(pceResult.preemptedTunnelsProcess(false));
                calcResult.setTunnels(outputPreemptedTunnels);
                calcResult.setCalcFailType(pceResult.getCalcFailType());
            }

            if (BiDirect.isBiDirectPositive(tunnel.getBiDirect()) && needCreateReverse) {
                ListenableFuture<List<PreemptedTunnel>> reverseResult = createReverseTunnel(input, tunnel);
                Logs.debug(LOG, TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());
                logTopoBandWidthInfo(input.isSimulateTunnel());
                return Futures.transform(
                        reverseResult,
                        (AsyncFunction<List<PreemptedTunnel>, CalcResult>) result -> {
                            outputPreemptedTunnels.addAll(result);
                            calcResult.setTunnels(outputPreemptedTunnels);
                            tunnel.reNotifyTunnelPath();
                            return Futures.immediateFuture(calcResult);
                        });
            } else {
                tunnel.reNotifyTunnelPath();
                Logs.debug(LOG, TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());
                logTopoBandWidthInfo(input.isSimulateTunnel());
                return Futures.immediateFuture(calcResult);
            }
        }

        private ListenableFuture<List<PreemptedTunnel>> createReverseTunnel(
                CreateTunnelPathInput input,
                TunnelPathInstance tunnel) {
            TunnelPathInstance reversePath = pathHolder
                    .getTunnelPathInstance(input.getTailNodeId(), (int) tunnel.getBiDirect().getReverseId(),
                                           input.isSimulateTunnel());
            if (reversePath == null) {
                final TunnelPathInstance newReversePath = new TunnelPathInstance(tunnel);
                return Futures.transform(
                        newReversePath.calcPathAsync(),
                        (AsyncFunction<PceResult, List<PreemptedTunnel>>) resultReverse -> {
                            List<PreemptedTunnel> outputReversePreemptedTunnels = new ArrayList<>();
                            if (!resultReverse.isCalcFail()) {
                                pathHolder.writeTunnel(newReversePath);
                                outputReversePreemptedTunnels.addAll(resultReverse.preemptedTunnelsProcess(false));
                                LOG.info("Add reverse tunnel {}-->{}", newReversePath, outputReversePreemptedTunnels);
                            }
                            return Futures.immediateFuture(outputReversePreemptedTunnels);
                        });
            } else {
                LOG.error("Reverse tunnel:{} {}is exist in tunnelPaths", input.getTailNodeId(),
                          tunnel.getBiDirect().getReverseId());
                return Futures.immediateFuture(new ArrayList<>());
            }
        }
    }
}
