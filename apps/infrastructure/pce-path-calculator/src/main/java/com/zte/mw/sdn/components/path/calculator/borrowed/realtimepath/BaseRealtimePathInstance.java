/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.realtimepath;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.CalculateStrategyContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.CalculateStrategyContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.calculate.strategy.container.strategy.type.DelayStrategyBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.calculate.strategy.container.strategy.type.MetricStrategyBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.multiple.paths.param.grouping.MultiplePathsParam;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser.PathChooserFactory;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.PathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.servicepath.DefaultBidirectImpl;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.ITunnel;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.strategy.ICalcStrategy;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformerFactory;
import com.zte.ngip.ipsdn.pce.path.core.transformer.MetricTransformer;

/**
 * Created by 10204924 on 2017/8/30.
 */
public abstract class BaseRealtimePathInstance {
    BaseRealtimePathInstance() {
    }

    BaseRealtimePathInstance(GetRealtimePathInput input) {
        this.headNodeId = NodeId.getDefaultInstance(input.getHeadNodeId().getValue());
        this.tailNodeId = NodeId.getDefaultInstance(input.getTailNodeId().getValue());
        this.topoId = (input.getTopologyId() != null)
                ? input.getTopologyId() : TopologyId.getDefaultInstance(ComUtility.DEFAULT_TOPO_ID_STRING);
        this.teArg = new TeArgumentBean(input, this.topoId);
        setCalculateStrategy(input.getCalculateStrategyContainer());
        setRecalcWithoutDelay(input.isRecalcWithoutDelay());
        this.multiplePathsParam = input.getMultiplePathsParam();
        this.biDirect = input.getBiDirectContainer() == null ? null : new DefaultBidirectImpl(input);
    }

    BaseRealtimePathInstance(GetRealtimePathInput input, ITunnel existedTunnel) {
        this.headNodeId = NodeId.getDefaultInstance(input.getHeadNodeId().getValue());
        this.tailNodeId = NodeId.getDefaultInstance(existedTunnel.getTailNode().getValue());
        this.tunnelUnifyKey = existedTunnel.getTunnelUnifyKey();
        this.topoId = existedTunnel.getTopoId();
        setRealtimePathTeArgBean(input, existedTunnel);
        setCalculateStrategy(input.getCalculateStrategyContainer());
        setRecalcWithoutDelay(input.isRecalcWithoutDelay());
        this.multiplePathsParam = input.getMultiplePathsParam();
        this.biDirect = input.getBiDirectContainer() == null ? null : new DefaultBidirectImpl(input);
        this.path = existedTunnel.getMasterPath();
    }

    protected NodeId headNodeId;
    protected NodeId tailNodeId;
    protected TopologyId topoId;
    protected TeArgumentBean teArg;
    protected TunnelUnifyKey tunnelUnifyKey;
    protected CalculateStrategyContainer calculateStrategy;
    protected boolean recalcWithoutDelay;
    protected BiDirect biDirect;
    protected List<Link> path;
    protected long lspMetric;
    protected long lspDelay;
    MultiplePathsParam multiplePathsParam;
    boolean enableZeroBandwidth;
    boolean zeroBandwidthPath;
    PathChooserFactory.PathChooserName pathChooserName;

    public void setCalculateStrategy(CalculateStrategyContainer calculateStrategy) {
        if (calculateStrategy == null || calculateStrategy.getStrategyType() == null) {
            this.calculateStrategy = new CalculateStrategyContainerBuilder()
                    .setStrategyType(new MetricStrategyBuilder()
                                             .setMetricStrategy(true)
                                             .build())
                    .build();
            return;
        }
        if (calculateStrategy.getStrategyType()
                instanceof org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                .calculate.strategy.calculate.strategy.container.strategy.type.MetricStrategy) {
            this.calculateStrategy = new CalculateStrategyContainerBuilder()
                    .setStrategyType(new MetricStrategyBuilder()
                                             .setMetricStrategy(true)
                                             .build())
                    .build();
        } else {
            this.calculateStrategy = new CalculateStrategyContainerBuilder()
                    .setStrategyType(new DelayStrategyBuilder()
                                             .setDelayStrategy(true)
                                             .build())
                    .build();
        }
    }

    public void setRecalcWithoutDelay(Boolean recalcWithoutDelayFlag) {
        if (recalcWithoutDelayFlag == null) {
            if (isMetricStrategy()) {
                this.recalcWithoutDelay = true;
            } else {
                this.recalcWithoutDelay = false;
            }
        } else {
            this.recalcWithoutDelay = recalcWithoutDelayFlag;
        }
    }

    private boolean isMetricStrategy() {
        return calculateStrategy.getStrategyType()
                instanceof org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                .calculate.strategy.calculate.strategy.container.strategy.type.MetricStrategy;
    }

    abstract void setRealtimePathTeArgBean(GetRealtimePathInput input, ITunnel existedTunnel);

    @SuppressWarnings("unchecked")
    PathProvider<MetricTransformer> generateCommonRealtimePathProvider(
            TunnelUnifyKey tunnelUnifyKey, TeArgumentBean teArgumentBean) {
        PathProvider<MetricTransformer> pathProvider = new PathProvider(headNodeId, tunnelUnifyKey, tailNodeId, topoId,
                                                                        generateCalculateStrategy(),
                                                                        generateTransformerFactory());
        pathProvider.setTeArgWithBuildNew(teArgumentBean);
        pathProvider.setIsRealTimePath(true);
        pathProvider.setRecalc(recalcWithoutDelay);
        pathProvider.setMultiplePathsParam(this.multiplePathsParam);
        pathProvider.setBiDirect(biDirect);
        pathProvider.setPathChooserName(pathChooserName);
        return pathProvider;
    }

    ICalcStrategy<NodeId, Link> generateCalculateStrategy() {
        return PceUtil.createCalcStrategy(calculateStrategy, BiDirect.isBiDirect(biDirect), topoId);
    }

    ITransformerFactory generateTransformerFactory() {
        return PceUtil.createTransformerFactory(calculateStrategy);
    }

    public long getLspMetric() {
        return lspMetric;
    }

    public long getLspDelay() {
        return lspDelay;
    }

    public List<Link> getPath() {
        return path;
    }
}
