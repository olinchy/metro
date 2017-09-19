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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import edu.uci.ics.jung.graph.Graph;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.CreateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.CreateServiceOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.GetDomainPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.GetDomainPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.GetDomainPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.GetRealtimePathListInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.GetRealtimePathListOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.GetRealtimePathListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.PceHControlService;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.RemoveServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.UpdateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.UpdateServiceOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.get.domain.path.output.DomainPath;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.get.domain.path.output.DomainPathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.get.realtime.path.list.input.PathCalcRequests;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.get.realtime.path.list.output.PathCalcResults;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.get.realtime.path.list.output.PathCalcResultsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.get.realtime.path.list.output.path.calc.results.RealtimePathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.multiple.paths.param.grouping.MultiplePathsParam;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.realtimepath.RealtimePathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.servicepath.ServiceHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.servicepath.ServiceOnTopoChangeHandler;
import com.zte.mw.sdn.components.path.calculator.borrowed.servicepath.ServicePathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.RpcInputChecker;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.RpcReturnUtils;

import com.zte.ngip.ipsdn.pce.path.api.topochange.LinkChange;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;

public class PceHServiceProvider implements PceHControlService {
    public PceHServiceProvider() {
        pathHolder = PcePathHolder.getInstance();
        this.serviceDispatchHandler = new ServiceDispatchHandler(pathHolder);
        this.topoChangeHandler = new ServiceOnTopoChangeHandler(pathHolder);
    }

    private static final Logger LOG = LoggerFactory.getLogger(PceHServiceProvider.class);
    private PcePathHolder pathHolder;
    private ServiceDispatchHandler serviceDispatchHandler;
    private ServiceOnTopoChangeHandler topoChangeHandler;

    private static DomainPath buildDomainPath(PathResult pathResult) {
        return new DomainPathBuilder().setPathLink(PceUtil.transform2PathLink(pathResult.getPath()))
                .setLspMetric(pathResult.getLspMetric()).setLspDelay(pathResult.getLspDelay()).build();
    }

    private static PathCalcResults buildResult(
            PathCalcRequests pathrequest,
            org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.realtime.path.output
                    .RealtimePath realtimePath) {
        if (realtimePath == null) {
            return new PathCalcResultsBuilder().setHeadNodeId(pathrequest.getHeadNodeId())
                    .setRealtimePath(new RealtimePathBuilder().setPathLink(Collections.emptyList()).build())
                    .setTailNodeId(pathrequest.getTailNodeId()).build();
        }
        return new PathCalcResultsBuilder().setHeadNodeId(pathrequest.getHeadNodeId())
                .setTailNodeId(pathrequest.getTailNodeId()).setRealtimePath(
                        new RealtimePathBuilder().setPathLink(realtimePath.getPathLink())
                                .setLspDelay(realtimePath.getLspDelay()).setLspMetric(realtimePath.getLspMetric())
                                .build()).build();
    }

    @Override
    public Future<RpcResult<GetDomainPathOutput>> getDomainPath(GetDomainPathInput input) {
        if (input.getHeadNodeId() == null || input.getTailNodeId() == null) {
            return RpcReturnUtils.returnErr("Illegal input arguments");
        }
        LOG.debug(input.toString());
        LOG.debug(TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());
        GetDomainPathOutput output;
        try {
            output = MoreExecutors.newDirectExecutorService().submit(() -> {
                RealtimePathInstance path = new RealtimePathInstance(input, getTunnelUnifyKey(input));
                path.calcPath();
                return new GetDomainPathOutputBuilder().setZeroBandwidthPath(path.isZeroBandwidthPath())
                        .setDomainPath(buildPathOutPut(path, input.getMultiplePathsParam())).build();
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("getDomainPath face exception {}", e);
            return Futures.immediateFailedFuture(e);
        }

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<UpdateServiceOutput>> updateService(UpdateServiceInput input) {
        if (!RpcInputChecker.check(input)) {
            Logs.error(LOG, "updateService failed! {}", RpcReturnUtils.ILLEGAL_ARGUMENT);
            return RpcReturnUtils.returnErr(RpcReturnUtils.ILLEGAL_ARGUMENT);
        }
        Logs.debug(LOG, input.toString());
        return serviceDispatchHandler.update(input);
    }

    @Override
    public Future<RpcResult<CreateServiceOutput>> createService(CreateServiceInput input) {
        if (!RpcInputChecker.check(input)) {
            Logs.error(LOG, "createService failed! {}", RpcReturnUtils.ILLEGAL_ARGUMENT);
            return RpcReturnUtils.returnErr(RpcReturnUtils.ILLEGAL_ARGUMENT);
        }

        Logs.debug(LOG, input.toString());
        Logs.debug(LOG, TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoString());
        PceUtil.logTopoBandWidthInfo(input.isSimulateTunnel());

        return serviceDispatchHandler.create(input);
    }

    @Override
    public Future<RpcResult<Void>> removeService(RemoveServiceInput input) {
        if (!RpcInputChecker.check(input)) {
            Logs.error(LOG, "removeService failed! {}", RpcReturnUtils.ILLEGAL_ARGUMENT);
            return RpcReturnUtils.returnErr(RpcReturnUtils.ILLEGAL_ARGUMENT);
        }
        Logs.debug(LOG, input.toString());
        return serviceDispatchHandler.remove(input);
    }

    @Override
    public Future<RpcResult<GetRealtimePathListOutput>> getRealtimePathList(GetRealtimePathListInput input) {
        Logs.info(LOG, "getRealtimePathList input {}", input);
        List<PathCalcResults> resultList =
                input.getPathCalcRequests().stream().map(this::calcPathRequest).filter(result -> result != null)
                        .collect(Collectors.toList());
        GetRealtimePathListOutput output =
                new GetRealtimePathListOutputBuilder().setPathCalcResults(resultList).build();
        Logs.info(LOG, "getRealtimePathList output {}", output);
        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    private TunnelUnifyKey getTunnelUnifyKey(GetDomainPathInput input) {
        if (input.getServiceName() == null) {
            return null;
        }
        ServicePathInstance servicePathInstance =
                pathHolder.getServiceInstance(input.getHeadNodeId(), input.getServiceName());
        if (servicePathInstance != null) {
            return servicePathInstance.getTunnelUnifyKey();
        }
        ServiceHsbPathInstance serviceHsbPathInstance =
                pathHolder.getServiceHsbInstance(input.getHeadNodeId(), input.getServiceName());
        if (serviceHsbPathInstance != null) {
            return serviceHsbPathInstance.getTunnelUnifyKey();
        }
        return null;
    }

    private List<DomainPath> buildPathOutPut(RealtimePathInstance path, MultiplePathsParam multiplePathsParam) {
        List<DomainPath> pathLists = new ArrayList<>();
        if (null == multiplePathsParam) {
            long lspMetric = path.getLspMetric();
            long lspDelay = path.getLspDelay();
            LOG.debug("Path:" + ComUtility.pathToString(path.getLsp()));
            DomainPath domainPath = new DomainPathBuilder().setPathLink(PceUtil.transform2PathLink(path.getLsp()))
                    .setLspMetric(lspMetric).setLspDelay(lspDelay).build();
            pathLists.add(domainPath);
        } else {
            pathLists.addAll(path.getPathResultList().stream()
                                     .peek(pcePath -> LOG.debug("Path:" + ComUtility.pathToString(pcePath.getPath())))
                                     .filter(result -> result.getPath() != null && !result.getPath().isEmpty())
                                     .map(PceHServiceProvider::buildDomainPath).collect(Collectors.toList()));
        }

        return pathLists;
    }

    private PathCalcResults calcPathRequest(PathCalcRequests pathrequest) {
        if (pathrequest.getHeadNodeId() == null || pathrequest.getTailNodeId() == null) {
            Logs.info(LOG, "calcPathRequest input error{}", pathrequest);
            return null;
        }

        GetRealtimePathInput input = new GetRealtimePathInputBuilder().setTopologyId(pathrequest.getTopologyId())
                .setHeadNodeId(pathrequest.getHeadNodeId()).setTailNodeId(pathrequest.getTailNodeId())
                .setBandwidth(pathrequest.getBandwidth()).setMaxDelay(pathrequest.getMaxDelay())
                .setContrainedAddress(pathrequest.getContrainedAddress()).setNextAddress(pathrequest.getNextAddress())
                .setExcludingAddress(pathrequest.getExcludingAddress())
                .setAffinityStrategy(pathrequest.getAffinityStrategy())
                .setBiDirectContainer(pathrequest.getBiDirectContainer())
                .setTryToAvoidLink(pathrequest.getTryToAvoidLink()).build();
        org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.get.realtime.path.output.RealtimePath
                realtimePath = null;
        try {
            realtimePath = PcePathProvider.getInstance().getRealtimePath(input).get().getResult().getRealtimePath();
        } catch (InterruptedException | ExecutionException e) {
            Logs.info(LOG, "calcPathRequest face error{}", e);
        }
        return buildResult(pathrequest, realtimePath);
    }

    public ServiceHsbPathInstance getServiceHsbInstance(NodeId headNode, String serviceName) {
        return pathHolder.getServiceHsbInstance(headNode, serviceName);
    }

    public ServicePathInstance getServiceInstance(NodeId headNode, String serviceName) {
        return pathHolder.getServiceInstance(headNode, serviceName);
    }

    public void handleLinkChange(List<LinkChange> linkChangeList, Graph<NodeId, Link> graph) {
        topoChangeHandler.handleLinkChange(linkChangeList, graph);
    }
}
