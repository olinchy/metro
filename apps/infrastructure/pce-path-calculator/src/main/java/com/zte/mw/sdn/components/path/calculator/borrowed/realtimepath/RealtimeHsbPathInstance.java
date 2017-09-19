/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.realtimepath;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroupMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.PathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.srlg.PriorityAvoidLinks;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.HsbNewTeArgBuilder;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelhsbpath.TunnelHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.ITunnel;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.srlg.AovidLinks;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBeanLsp;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;
import com.zte.ngip.ipsdn.pce.path.core.transformer.MetricTransformer;

/**
 * Created by 10204924 on 2017/8/30.
 */
public class RealtimeHsbPathInstance extends BaseRealtimePathInstance {
    RealtimeHsbPathInstance(GetRealtimePathInput input, TunnelHsbPathInstance tunnelHsbPathInstance) {
        super(input, tunnelHsbPathInstance);
        this.slaveTunnelUnifyKey = tunnelHsbPathInstance.getSlaveTunnelUnifyKey();
        this.slavePath = tunnelHsbPathInstance.getSlavePath();
        this.lspMetric = tunnelHsbPathInstance.getMasterMetric();
        this.slaveLspMetric = tunnelHsbPathInstance.getSlaveMetric();
        this.lspDelay = tunnelHsbPathInstance.getMasterDelay();
        this.slaveLspDelay = tunnelHsbPathInstance.getSlaveDelay();
        this.slaveTeArg = tunnelHsbPathInstance.getSlaveTeArg();
        this.isSrlgEnabled = tunnelHsbPathInstance.isSrlgEnabled();
    }

    private List<Link> slavePath;
    private long slaveLspMetric;
    private long slaveLspDelay;
    private TunnelUnifyKey slaveTunnelUnifyKey;
    private TeArgumentBeanLsp slaveTeArg;
    private boolean isSrlgEnabled;

    @Override
    void setRealtimePathTeArgBean(GetRealtimePathInput input, ITunnel existedTunnel) {
        HsbNewTeArgBuilder hsbNewTeArgBuilder = new HsbNewTeArgBuilder(existedTunnel.getTeArgumentBean(), topoId);
        this.teArg = hsbNewTeArgBuilder.buildNewTeArg(input, input, null);
    }

    /**
     * Call this method when {@link GetRealtimePathInput} contains a valid tunnelId.
     *
     * @param oldBwGroup oldBwGroup
     * @param newBwGroup newBwGroup
     * @return PceResult
     */
    public ListenableFuture<PceResult> calcPathAsync(
            BwSharedGroupContainer oldBwGroup,
            BwSharedGroupContainer newBwGroup) {
        BwSharedGroupContainer deletedBwGroup = BwSharedGroupMng.getDeletedGroups(newBwGroup, oldBwGroup);
        ListenableFuture<PceResult> calcMasterFuture = calcSinglePathAsync(path, true, teArg, newBwGroup,
                                                                           deletedBwGroup);
        CalSlaveAsyncFunction calSlaveAsyncFunction =
                new CalSlaveAsyncFunction(teArg, slaveTeArg, newBwGroup, deletedBwGroup);
        return Futures.transform(calcMasterFuture, calSlaveAsyncFunction);
    }

    @SuppressWarnings("unchecked")
    private ListenableFuture<PceResult> calcSinglePathAsync(
            List<Link> oldPath, boolean isMaster,
            TeArgumentBean teArgBean, BwSharedGroupContainer bwSharedGroups,
            BwSharedGroupContainer deletedBwSharedGroups) {
        TunnelUnifyKey tunnelKey = isMaster ? tunnelUnifyKey : slaveTunnelUnifyKey;

        PathProvider<MetricTransformer> pathProvider = generateCommonRealtimePathProvider(tunnelKey, teArgBean);
        pathProvider.setBwSharedGroups(bwSharedGroups, deletedBwSharedGroups);

        if (isMaster) {
            return calcMasterPathAsync(pathProvider);
        } else {
            pathProvider.clearTryToAvoidLinks();
            PriorityAvoidLinks priorityAvoidLinks = generatePriorityAvoidLinks(teArgBean);
            if (!CollectionUtils.isNullOrEmpty(path)) {
                pathProvider.setRecalc(true);
            }
            return calcSlavePathAsync(oldPath, pathProvider, priorityAvoidLinks);
        }
    }

    private ListenableFuture<PceResult> calcMasterPathAsync(PathProvider<MetricTransformer> pathProvider) {
        return Futures.transform(pathProvider.calcPathAsync(), (AsyncFunction<PceResult, PceResult>) calResult -> {
            setMasterLspInfo(pathProvider);
            calResult.setCalcFail(CollectionUtils.isNullOrEmpty(path));
            return Futures.immediateFuture(calResult);
        });
    }

    private PriorityAvoidLinks generatePriorityAvoidLinks(TeArgumentBean teArgBean) {
        List<Link> avoidMasterPath = ComUtility.getTryToAvoidLinkForHsb(path);
        List<Link> manualAvoidLinks = teArgBean.getTryToAvoidLinkInLinks();

        PriorityAvoidLinks priorityAvoidLinks = new PriorityAvoidLinks();
        if (isSrlgEnabled) {
            List<Link> srlgAvoidLinks = PceUtil.getSrlgAvoidLinks(
                    path,
                    TopoServiceAdapter.getInstance().getPceTopoProvider()
                            .getTopoGraph(ComUtility.getSimulateFlag(tunnelUnifyKey), topoId));
            priorityAvoidLinks.addAvoidLinks(new AovidLinks(srlgAvoidLinks));
        }
        priorityAvoidLinks.addAvoidLinks(new AovidLinks(manualAvoidLinks));
        priorityAvoidLinks.addAvoidLinks(new AovidLinks(avoidMasterPath));
        return priorityAvoidLinks;
    }

    private ListenableFuture<PceResult> calcSlavePathAsync(
            List<Link> oldPath,
            PathProvider<MetricTransformer> pathProvider, PriorityAvoidLinks priorityAvoidLinks) {
        return Futures.transform(
                priorityAvoidLinks.calcPathByAvoidPriorityAsync(pathProvider, slaveTunnelUnifyKey, oldPath),
                (AsyncFunction<PceResult, PceResult>) calResult -> {
                    setSlaveLspInfo(pathProvider);
                    return Futures.immediateFuture(calResult);
                });
    }

    private void setMasterLspInfo(PathProvider<MetricTransformer> pathProvider) {
        path = pathProvider.getPath();
        lspMetric = pathProvider.getLspMetric();
        lspDelay = pathProvider.getLspDelay();
    }

    private void setSlaveLspInfo(PathProvider<MetricTransformer> pathProvider) {
        slavePath = pathProvider.getPath();
        slaveLspMetric = pathProvider.getLspMetric();
        slaveLspDelay = pathProvider.getLspDelay();
    }

    long getSlaveLspMetric() {
        return slaveLspMetric;
    }

    long getSlaveLspDelay() {
        return slaveLspDelay;
    }

    public List<Link> getSlavePath() {
        return slavePath;
    }

    private class CalSlaveAsyncFunction implements AsyncFunction<PceResult, PceResult> {
        CalSlaveAsyncFunction(
                TeArgumentBean newTeArg,
                TeArgumentBeanLsp newSlaveTeArg, BwSharedGroupContainer bwGroups,
                BwSharedGroupContainer deletedBwSharedGroups) {
            this.masterTeArg = newTeArg;
            this.slaveTeArgBeanLsp = newSlaveTeArg;
            this.delBwGroups = deletedBwSharedGroups;
            this.bwGroups = bwGroups;
        }

        TeArgumentBean masterTeArg;
        TeArgumentBeanLsp slaveTeArgBeanLsp;
        BwSharedGroupContainer bwGroups;
        BwSharedGroupContainer delBwGroups;

        @Override
        public ListenableFuture<PceResult> apply(PceResult calcMasterResult) throws Exception {

            TeArgumentBean newSlaveTeArg = PceUtil.generatorSlaveTeArg(masterTeArg, slaveTeArgBeanLsp);
            return Futures.transform(
                    calcSinglePathAsync(slavePath, false, newSlaveTeArg, bwGroups, delBwGroups),
                    (AsyncFunction<PceResult, PceResult>) calcSlaveResult -> {
                        calcMasterResult.merge(calcSlaveResult);
                        return Futures.immediateFuture(calcMasterResult);
                    });
        }
    }
}
