/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.common.data.TeArgCommonData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.common.data.TeArgCommonDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TeArgument;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TeArgumentLsp;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.zte.mw.sdn.components.path.calculator.borrowed.maintenancewindow.MaintenanceTeArgumentBeanLsp;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.LspAttributes;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.PathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.srlg.SrlgAttribute;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBeanLsp;
import com.zte.ngip.ipsdn.pce.path.core.strategy.ICalcStrategy;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformerFactory;
import com.zte.ngip.ipsdn.pce.path.core.transformer.MetricTransformer;

/**
 * Created by 10204924 on 2017/7/6.
 */
public abstract class AbstractTunnelPathInstance extends CommonTunnel {
    protected AbstractTunnelPathInstance(NodeId headNodeId, String serviceName) {
        super(headNodeId, serviceName);
    }

    AbstractTunnelPathInstance(NodeId headNode, int id) {
        super(headNode, id);
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTunnelPathInstance.class);
    protected NodeId headNodeId;
    protected NodeId tailNodeId;
    protected TopologyId topoId;
    protected TeArgumentBean teArg;
    protected TunnelUnifyKey tunnelUnifyKey;
    protected List<Link> path;
    protected BiDirect biDirect;
    protected LspAttributes lspAttributes = new LspAttributes();

    @SuppressWarnings("unchecked")
    protected PathProvider<MetricTransformer> buildBasePathProvider(
            boolean failRollback, TeArgumentBean newArg,
            List<Link> overlapPath, BwSharedGroupContainer newBwSharedGroups,
            BwSharedGroupContainer deletedBwSharedGroups) {
        PathProvider<MetricTransformer> pathProvider =
                new PathProvider(headNodeId, tunnelUnifyKey, tailNodeId, topoId, generateCalculateStrategy(),
                                 generateTransformerFactory());

        pathProvider.setTeArgWithBuildNew(newArg);
        pathProvider.setOldPath(path);
        pathProvider.setFailRollback(failRollback);
        pathProvider.setOverlapPath(overlapPath);
        pathProvider.setBwSharedGroups(newBwSharedGroups, deletedBwSharedGroups);
        pathProvider.setBiDirect(biDirect);
        pathProvider.setRecalc(recalcWithoutDelay);
        return pathProvider;
    }

    ICalcStrategy<NodeId, Link> generateCalculateStrategy() {
        return PceUtil.createCalcStrategy(calculateStrategy, biDirect != null, topoId);
    }

    ITransformerFactory generateTransformerFactory() {
        return PceUtil.createTransformerFactory(calculateStrategy);
    }

    protected TeArgumentBean getNewTeArg(
            TeArgument newTeAgCom, TeArgumentLsp teArgumentLsp,
            TeArgumentLsp maintenanceLsp) {
        if (PceUtil.isMaintenance(maintenanceLsp)) {
            TeArgCommonData teArgComm = new TeArgCommonDataBuilder()
                    .setHoldPriority((short) teArg.getHoldPriority())
                    .setPreemptPriority((short) teArg.getPreemptPriority())
                    .setBandwidth(teArg.getBandWidth())
                    .setMaxDelay(teArg.getMaxDelay())
                    .build();
            TeArgumentBeanLsp originalLspBean = new TeArgumentBeanLsp(teArgumentLsp, topoId);

            MaintenanceTeArgumentBeanLsp maintenanceTeArgBeanLsp =
                    new MaintenanceTeArgumentBeanLsp(maintenanceLsp, topoId);
            maintenanceTeArgBeanLsp.mergeToOriginalMaster(originalLspBean);
            TeArgumentBean teArgumentBean = new TeArgumentBean(teArgComm, originalLspBean);
            Logs.debug(LOG, "tunnel teArg is merged with MaintenanceTeArgument: {}", teArgumentBean);
            return teArgumentBean;
        }
        return getNewTeArg(newTeAgCom, teArgumentLsp);
    }

    protected TeArgumentBean getNewTeArg(TeArgument newTeAgCom, TeArgumentLsp teArgumentLsp) {
        //there isn't priority and priority can't change.
        TeArgCommonData teArgComm = new TeArgCommonDataBuilder().setHoldPriority((short) teArg.getHoldPriority())
                .setPreemptPriority((short) teArg.getPreemptPriority()).setBandwidth(newTeAgCom.getBandwidth())
                .setMaxDelay(newTeAgCom.getMaxDelay()).build();

        TeArgumentBeanLsp teArgLsp = new TeArgumentBeanLsp(teArgumentLsp, topoId);

        return new TeArgumentBean(teArgComm, teArgLsp);
    }

    protected PceResult calcRefreshPath(Supplier<PceResult> calcPathFunc) {
        PceResult pceResult = calcPathFunc.get();
        if (pceResult.isCalcFail()) {
            pceResult = calcWithZeroBandWidth();
            if (!pceResult.isCalcFail() && !CollectionUtils.isNullOrEmpty(path)) {
                Logs.info(LOG, "Tunnel change to zero bandWith");
                pceResult.enableBandwidthFreeFlag();
                setChangeToZeroBandWidth(true);
            } else {
                setChangeToZeroBandWidth(false);
            }
        } else {
            setChangeToZeroBandWidth(false);
        }
        return pceResult;
    }

    protected synchronized PceResult calcWithZeroBandWidth() {
        TeArgumentBean newArg = generatorZeroBandWidthTeArg(teArg);
        if (newArg == null) {
            return PceResult.create();
        }
        PceResult pceResult = PceResult.create();
        Logs.info(LOG, "Tunnel {} change bandwidth to 0 to computer", getId());
        try {
            pceResult = calcPathAsync(false, newArg, path, null, null, bwSharedGroups).get();
        } catch (InterruptedException | ExecutionException e) {
            Logs.info(LOG, "tunnel path calcWithZeroBandWidth {}", e);
        }
        return pceResult;
    }

    protected ListenableFuture<PceResult> calcPathAsync(
            boolean failRollback, TeArgumentBean newArg,
            List<Link> masterPath, List<Link> overlapPath, BwSharedGroupContainer newBwSharedGroups,
            BwSharedGroupContainer deletedBwSharedGroups) {
        PceResult result = new PceResult();
        if (!newArg.isValid()) {
            result.setCalcFail(true);
            return Futures.immediateFuture(result);
        }
        PathProvider<MetricTransformer> pathProvider = buildPathProvider(failRollback, newArg, overlapPath, masterPath,
                                                                         newBwSharedGroups, deletedBwSharedGroups);
        return Futures
                .transform(pathProvider.calcPathAsync(), (AsyncFunction<PceResult, PceResult>) pceResult -> {
                    if (failRollback && (pceResult.isCalcFail())) {
                        return Futures.immediateFuture(pceResult);
                    }
                    setPathInfo(pathProvider);
                    return Futures.immediateFuture(pceResult);
                });
    }

    @SuppressWarnings("unchecked")
    protected abstract PathProvider<MetricTransformer> buildPathProvider(
            boolean failRollback, TeArgumentBean newArg,
            List<Link> masterPath, List<Link> overlapPath, BwSharedGroupContainer newBwSharedGroups,
            BwSharedGroupContainer deletedBwSharedGroups);

    protected void setPathInfo(PathProvider<MetricTransformer> pathProvider) {
        path = pathProvider.getPath();
        lspAttributes.setLspMetric(pathProvider.getLspMetric());
        lspAttributes.setLspDelay(pathProvider.getLspDelay());
        lspAttributes.setSrlgAttr(pathProvider.getSrlgAttr());
    }

    protected PceResult calcUnestablishedPath(Supplier<PceResult> calcPathFunc) {
        PceResult pceResult;
        if (isChangeToZeroBandWidth()) {
            pceResult = PceResult.create();
            try {
                pceResult = calcPathAsync(true, teArg, path, null, bwSharedGroups, null).get();
            } catch (InterruptedException | ExecutionException e) {
                Logs.info(LOG, "tunnel path calcUnestablishedPath {}", e);
            }
            if (!pceResult.isCalcFail()) {
                Logs.info(LOG, "calcUnestablishedPath success,and clear zero bandwidth flag");
                setChangeToZeroBandWidth(false);
            }
        } else {
            pceResult = calcPathFunc.get();
        }
        return pceResult;
    }

    public List<Link> getLsp() {
        return path;
    }

    public long getLspMetric() {
        return lspAttributes.getLspMetric();
    }

    public long getLspDelay() {
        return lspAttributes.getLspDelay();
    }

    public SrlgAttribute getSrlgAttr() {
        return lspAttributes.getSrlgAttr();
    }

    protected void setLspAttributes(long lspMetric, long lspDelay, SrlgAttribute srlgAttr) {
        lspAttributes.setLspMetric(lspMetric);
        lspAttributes.setLspDelay(lspDelay);
        lspAttributes.setSrlgAttr(srlgAttr);
    }

    @Override
    public boolean isDelayEligible() {
        return !isDelayRestricted() || lspAttributes.getLspDelay() <= teArg.getMaxDelay();
    }

    @Override
    public boolean isUnestablished() {
        return CollectionUtils.isNullOrEmpty(path);
    }

    @Override
    public boolean isPathOverlap() {
        return false;
    }
}
