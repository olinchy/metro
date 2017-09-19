/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.BandwidthChangeType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.adjust.tunnel.bandwidth.input.PathAdjustRequest;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.CalculateStrategyContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.CalculateStrategyContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.calculate.strategy.container.strategy.type.DelayStrategyBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.calculate.strategy.container.strategy.type.MetricStrategyBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.sr.argument.SrArgument;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.ZeroBandwidthUtils;

import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;

public abstract class CommonTunnel implements ITunnel {
    protected CommonTunnel() {
        id = 0;
        headNode = null;
    }

    public CommonTunnel(NodeId headNode, int id) {
        this.id = id;
        this.headNode = headNode;
    }

    public CommonTunnel(NodeId headNode, String serviceName) {
        this.serviceName = serviceName;
        this.headNode = headNode;
    }

    public static final int NORMAL_STATE = 0;
    public static final int ACTIVE_WAIT_STATE = 1;
    public static final int TOPO_CHANGE_STATE = 2;
    public static final int TUNNEL_INVALID = 3;
    protected CalculateStrategyContainer calculateStrategy;
    protected boolean recalcWithoutDelay;
    protected BwSharedGroupContainer bwSharedGroups;
    protected SrArgument srArgument;
    protected boolean isChangeToZeroBandWidth = false;
    private int id;
    private NodeId headNode;
    private String serviceName;
    private AtomicInteger tunnelState = new AtomicInteger(NORMAL_STATE);

    public BwSharedGroupContainer getBwSharedGroups() {
        return bwSharedGroups;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public NodeId getHeadNode() {
        return headNode;
    }

    @Override
    public SrArgument getSrArgument() {
        return srArgument;
    }

    @Override
    public void destroy() {
        setTunnelState(TUNNEL_INVALID);
    }

    @Override
    public PceResult reCalcPath(TunnelUnifyKey path) {
        return PceResult.nullPceResult;
    }

    @Override
    public PceResult refreshPath(TunnelUnifyKey path) {
        return PceResult.nullPceResult;
    }

    @Override
    public int getTunnelState() {
        return tunnelState.get();
    }

    @Override
    public int setTunnelState(int newState) {
        return tunnelState.getAndSet(newState);
    }

    @Override
    public boolean comAndSetTunnelState(int oldState, int newState) {
        return tunnelState.compareAndSet(oldState, newState);
    }

    @Override
    public void reNotifyTunnelPath() {
        if (getTunnelState() == TOPO_CHANGE_STATE) {
            setTunnelState(NORMAL_STATE);
            reCalcPath(getTunnelUnifyKey());
            notifyPathChange();
            notifyReverseTunnel(true);
        } else if (getTunnelState() == ACTIVE_WAIT_STATE) {
            setTunnelState(NORMAL_STATE);
        }
    }

    @Override
    public boolean isDelayRestricted() {
        return Optional.ofNullable(getTeArgumentBean()).map(TeArgumentBean::getMaxDelay)
                .map(maxDelay -> maxDelay != ComUtility.INVALID_DELAY).orElse(false);
    }

    @Override
    public boolean isSrlgOverlap() {
        return false;
    }

    @Override
    public boolean isMetricStrategy() {
        return calculateStrategy.getStrategyType()
                instanceof org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                .calculate.strategy.calculate.strategy.container.strategy.type.MetricStrategy;
    }

    @Override
    public boolean isDelayStrategy() {
        return calculateStrategy.getStrategyType()
                instanceof org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814
                .calculate.strategy.calculate.strategy.container.strategy.type.DelayStrategy;
    }

    @Override
    public boolean isSrTunnel() {
        return srArgument != null;
    }

    @Override
    public boolean isChangeToZeroBandWidth() {
        return isChangeToZeroBandWidth;
    }

    public void setChangeToZeroBandWidth(boolean changeToZeroBandWidth) {
        isChangeToZeroBandWidth = changeToZeroBandWidth;
    }

    protected abstract void notifyReverseTunnel(boolean isNeedNotify);

    protected void setCalculateStrategy(CalculateStrategyContainer calculateStrategy) {
        if (calculateStrategy == null || calculateStrategy.getStrategyType() == null
                || calculateStrategy.getStrategyType()
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

    protected void setRecalcWithoutDelay(Boolean recalcWithoutDelayFlag) {
        if (recalcWithoutDelayFlag == null) {
            this.recalcWithoutDelay = isMetricStrategy();
        } else {
            this.recalcWithoutDelay = recalcWithoutDelayFlag;
        }
    }

    protected void setBwSharedGroup(BwSharedGroupContainer bwSharedGroups) {
        if (bwSharedGroups == null || CollectionUtils.isNullOrEmpty(bwSharedGroups.getBwSharedGroupMember())) {
            this.bwSharedGroups = null;
        } else {
            this.bwSharedGroups = bwSharedGroups;
        }
    }

    @Override
    public String toString() {
        return Optional.ofNullable(getTunnelUnifyKey()).map(TunnelUnifyKey::toString).orElse("");
    }

    protected long getNewAdjustBw(PathAdjustRequest input, long oldBw) {
        long newBw = 0;
        if (BandwidthChangeType.Increase.equals(input.getChangeType())) {
            newBw = oldBw + input.getBandwidth();
        }
        if (BandwidthChangeType.Decrease.equals(input.getChangeType())) {
            if (oldBw < input.getBandwidth()) {
                newBw = 0;
            } else {
                newBw = oldBw - input.getBandwidth();
            }
        }
        return newBw;
    }

    protected boolean isPathHasEnoughBw(
            TeArgumentBean teArgumentBean, long demandBw, List<Link> pathLinks,
            boolean isBiDirect) {
        for (Link link : pathLinks) {
            if (!BandWidthMng.getInstance()
                    .hasEnoughBw(link, teArgumentBean.getPreemptPriority(), getHoldPriority(), demandBw,
                                 getTunnelUnifyKey(), isBiDirect)) {
                return false;
            }
        }
        return true;
    }

    protected TeArgumentBean generatorZeroBandWidthTeArg(TeArgumentBean teArg) {
        if (!PcePathProvider.getInstance().getZeroBandWidthFlag()) {
            return null;
        }
        if (teArg.getBandWidth() == 0) {
            return null;
        }
        return ZeroBandwidthUtils.generateZeroBandwidthTeArg(teArg);
    }
}
