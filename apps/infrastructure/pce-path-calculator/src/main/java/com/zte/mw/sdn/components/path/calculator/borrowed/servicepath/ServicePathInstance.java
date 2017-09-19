/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.servicepath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.ServicePathData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicepathinstancedata.ServicePathsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicepathinstancedata.ServicePathsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.CreateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.UpdateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bi.direct.argument.BiDirectContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.calculate.strategy.CalculateStrategyContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroupMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.PathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelUnifyRecordKey;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelsRecordPerPort;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathDb;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.topology.TunnelsRecordPerTopology;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.AbstractTunnelPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.DataBrokerDelegate;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.ZeroBandwidthUtils;

import com.zte.ngip.ipsdn.pce.path.api.srlg.SrlgAttribute;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;
import com.zte.ngip.ipsdn.pce.path.core.transformer.MetricTransformer;

public class ServicePathInstance extends AbstractTunnelPathInstance {
    /**
     * TunnelPathInstance.
     *
     * @param dbData dbData
     */
    public ServicePathInstance(ServicePathData dbData) {
        super(dbData.getHeadNodeId(), dbData.getServiceName());
        this.headNodeId = dbData.getHeadNodeId();
        this.tailNodeId = dbData.getTailNodeId();
        this.topoId = dbData.getTopologyId();
        this.tunnelUnifyKey =
                generaTunnelUnifyKey(headNodeId, dbData.getServiceName(), dbData.getBiDirectContainer(), false);
        this.teArg = setTeArgInfo(dbData);
        this.path = PcePathDb.getInstance().pathLinks2Links(topoId, false, dbData.getPathLink());
        this.lspAttributes.setLspMetric(dbData.getLspMetric());
        this.lspAttributes.setLspDelay(dbData.getLspDelay());
        this.lspAttributes.setSrlgAttr(Optional.ofNullable(dbData.getSrlgs()).map(ArrayList::new)
                                               .map(SrlgAttribute::new).orElse(new SrlgAttribute()));
        setBwSharedGroup(dbData.getBwSharedGroupContainer());
        this.biDirect = dbData.getBiDirectContainer() == null ? null : new DefaultBidirectImpl(dbData);
        this.calculateStrategy = dbData.getCalculateStrategyContainer();
        this.recalcWithoutDelay = dbData.isRecalcWithoutDelay() == null ? false : dbData.isRecalcWithoutDelay();
    }

    public ServicePathInstance(CreateServiceInput input) {
        super(input.getHeadNodeId(), input.getServiceName());
        this.headNodeId = NodeId.getDefaultInstance(input.getHeadNodeId().getValue());
        this.tailNodeId = NodeId.getDefaultInstance(input.getTailNodeId().getValue());
        this.topoId = ComUtility.getTopoId(input.getTopologyId());
        this.teArg = new TeArgumentBean(input, this.topoId);
        setCalculateStrategy(input.getCalculateStrategyContainer());
        setRecalcWithoutDelay(input.isRecalcWithoutDelay());
        this.tunnelUnifyKey =
                generaTunnelUnifyKey(headNodeId, input.getServiceName(), input.getBiDirectContainer(), false);
        setBwSharedGroup(input.getBwSharedGroupContainer());
        setBiDirect(input.getBiDirectContainer() == null ? null : new DefaultBidirectImpl(input));
        TunnelsRecordPerTopology.getInstance().add(topoId, tunnelUnifyKey);
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServicePathInstance.class);

    private static TunnelUnifyKey generaTunnelUnifyKey(
            NodeId nodeId, String serviceName, BiDirectContainer biDirect,
            boolean isSimulate) {
        return new TunnelUnifyKey(nodeId, serviceName, null, PceUtil.isBiDirectional(biDirect), isSimulate);
    }

    private static TeArgumentBean setTeArgInfo(ServicePathData dbData) {
        TeArgumentBean teAttr = new TeArgumentBean(dbData.getTeArgCommonData());
        teAttr.setExcludedNodes(dbData.getExcludingNode());
        teAttr.setExcludedPorts(dbData.getExcludingPort());
        teAttr.setNextAddress(dbData.getNextAddress());
        teAttr.setTryToAvoidLink(dbData.getTryToAvoidLink());
        teAttr.setContrainedAddress(dbData.getContrainedAddress(), dbData.getTopologyId());
        teAttr.setAffinityStrategy(dbData.getAffinityStrategy());
        return teAttr;
    }

    @Override
    public NodeId getTailNode() {
        return tailNodeId;
    }

    @Override
    public TopologyId getTopoId() {
        return topoId;
    }

    @Override
    public long getOccupyBw() {
        return teArg.getBandWidth();
    }

    @Override
    public byte getHoldPriority() {
        return teArg.getHoldPriority();
    }

    @Override
    public TunnelUnifyKey getTunnelUnifyKey() {
        return tunnelUnifyKey;
    }

    @Override
    public List<Link> getMasterPath() {
        return path;
    }

    @Override
    public List<Link> getSlavePath() {
        return Collections.emptyList();
    }

    @Override
    public void writeDb() {
        if (getTunnelState() == TUNNEL_INVALID) {
            Logs.info(LOG, "tunnel state is invalid can not write db {}", tunnelUnifyKey);
            return;
        }
        if (!getTunnelUnifyKey().isSimulate()) {
            DataBrokerDelegate.getInstance().put(
                    LogicalDatastoreType.CONFIGURATION,
                    PcePathDb.buildServicePathDbPath(headNodeId, getServiceName()),
                    servicePathsInstanceCreate());
        }
    }

    @Override
    public void removeDb() {
        if (!getTunnelUnifyKey().isSimulate()) {
            DataBrokerDelegate.getInstance().delete(
                    LogicalDatastoreType.CONFIGURATION,
                    PcePathDb.buildServicePathDbPath(headNodeId, getServiceName()));
        }
    }

    @Override
    public void writeMemory() {
        if (getTunnelState() == TUNNEL_INVALID) {
            Logs.info(LOG, "tunnel state is invalid can not write memory {}", tunnelUnifyKey);
            return;
        }
        PcePathProvider.getInstance().updateTunnel(this);
    }

    @Override
    public void refreshSegments() {
        // do nothing
    }

    @Override
    public BiDirect getBiDirect() {
        return biDirect;
    }

    @Override
    public TeArgumentBean getTeArgumentBean() {
        return this.teArg;
    }

    @Override
    public boolean isSimulate() {
        return false;
    }

    private ServicePathsData servicePathsInstanceCreate() {
        return new ServicePathsDataBuilder()
                .setTopologyId(getTopoId())
                .setTeArgCommonData(getTeArg().getArgComm())
                .setNextAddress(PcePathDb.nextAddressConvert(getTeArg().getNextAddress()))
                .setExcludingPort(PcePathDb.excludePortConvert(getTeArg().getExcludedPorts()))
                .setExcludingNode(PcePathDb.excludeNodeConvert(getTeArg().getExcludedNodes()))
                .setServiceName(getServiceName())
                .setPathLink(PcePathDb.pathLinkToLinkConvert(path))
                .setHeadNodeId(headNodeId)
                .setTailNodeId(tailNodeId)
                .setLspMetric(lspAttributes.getLspMetric())
                .setLspDelay(lspAttributes.getLspDelay())
                .setSrlgs(transSrlgAttr(lspAttributes.getSrlgAttr()))
                .setCalculateStrategyContainer(getCalculateStrategy())
                .setBiDirectContainer(biDirect == null ? null : biDirect.getBiDirectContainer())
                .setAffinityStrategy(teArg.getAffinityStrategy())
                .setRecalcWithoutDelay(recalcWithoutDelay)
                .build();
    }

    public TeArgumentBean getTeArg() {
        return this.teArg;
    }

    public CalculateStrategyContainer getCalculateStrategy() {
        return calculateStrategy;
    }

    public ListenableFuture<PceResult> calcPathAsync(boolean failRollback) {
        return calcPathAsync(failRollback, teArg, null, bwSharedGroups, null);
    }

    public ListenableFuture<PceResult> calcPathAsync(
            boolean failRollback, TeArgumentBean newArg,
            List<Link> overlapPath, BwSharedGroupContainer newBwSharedGroups,
            BwSharedGroupContainer deletedBwSharedGroups) {
        return calcPathAsync(failRollback, newArg, null, overlapPath, newBwSharedGroups, deletedBwSharedGroups);
    }

    @Override
    protected PathProvider<MetricTransformer> buildPathProvider(
            boolean failRollback, TeArgumentBean newArg,
            List<Link> masterPath, List<Link> overlapPath, BwSharedGroupContainer newBwSharedGroups,
            BwSharedGroupContainer deletedBwSharedGroups) {
        return buildBasePathProvider(failRollback, newArg, overlapPath, newBwSharedGroups, deletedBwSharedGroups);
    }

    /**
     * updateWithoutRollback.
     *
     * @param input input
     * @return PceResult
     */
    public PceResult updateWithoutRollback(UpdateServiceInput input) {
        PceResult pceResult = update(input, false);
        storeTeArguments(input);
        setChangeToZeroBandWidth(ZeroBandwidthUtils.isZeroBandwidthPath(input.isZeroBandwidthPath()));
        return pceResult;
    }

    private PceResult update(UpdateServiceInput input, boolean rollback) {
        final TeArgumentBean oldTeArg = teArg;

        UpdateServiceInput newInput = input;
        if (ZeroBandwidthUtils.isZeroBandwidthPath(input.isZeroBandwidthPath())) {
            newInput = ZeroBandwidthUtils.generateZeroBandwidthUpdateServiceInput(input);
        }
        TeArgumentBean newArgForCalcPath = PceUtil.getNewTeArg(oldTeArg, newInput, newInput, topoId);

        boolean forceCalcPathWithBandwidth = PceUtil.isTunnelBandwidthShrinkToZero(oldTeArg, newInput.getBandwidth());
        if (forceCalcPathWithBandwidth) {
            newArgForCalcPath.setForceCalcPathWithBandwidth(true);
        }

        BwSharedGroupContainer deletedBwGroups =
                BwSharedGroupMng.getDeletedGroups(newInput.getBwSharedGroupContainer(), bwSharedGroups);
        PceResult pceResult = PceResult.nullPceResult;
        try {
            pceResult = calcPathAsync(rollback, newArgForCalcPath, null, newInput.getBwSharedGroupContainer(),
                                      deletedBwGroups).get();
        } catch (InterruptedException | ExecutionException e) {
            Logs.info(LOG, "update{} face exception {}", rollback ? "WithRollback" : "WithoutRollback", e);
        }
        return pceResult;
    }

    private void storeTeArguments(UpdateServiceInput input) {
        teArg = PceUtil.getNewTeArg(teArg, input, input, topoId);
        teArg.setForceCalcPathWithBandwidth(false);
        bwSharedGroups = input.getBwSharedGroupContainer();
    }

    /**
     * updateWithRollback.
     *
     * @param input input
     * @return PceResult
     */
    public PceResult updateWithRollback(UpdateServiceInput input) {
        PceResult pceResult = update(input, true);
        if (!pceResult.isCalcFail()) {
            storeTeArguments(input);
            setChangeToZeroBandWidth(ZeroBandwidthUtils.isZeroBandwidthPath(input.isZeroBandwidthPath()));
        }
        return pceResult;
    }

    public void recoverTunnelPathBw() {
        List<Link> lspPath = getPath();
        TeArgumentBean lspTeArg = getTeArg();

        if (!CollectionUtils.isNullOrEmpty(lspPath)) {
            if (bwSharedGroups != null) {
                BwSharedGroupMng.getInstance()
                        .recoverBw(lspPath, teArg.getPreemptPriority(), teArg.getHoldPriority(), tunnelUnifyKey,
                                   isBiDirect(), bwSharedGroups);
            } else {
                BandWidthMng.getInstance()
                        .recoverPathBw(lspPath, lspTeArg.getHoldPriority(), lspTeArg.getBandWidth(), tunnelUnifyKey,
                                       isBiDirect());
            }
        }
    }

    public List<Link> getPath() {
        return this.path;
    }

    protected boolean isBiDirect() {
        return biDirect != null;
    }

    private void setBiDirect(BiDirect biDirect) {
        this.biDirect = biDirect;
    }

    @Override
    public void destroy() {
        super.destroy();
        clearPath();
    }

    @Override
    protected void notifyReverseTunnel(boolean isNeedNotify) {
        /*need do nothing*/
    }

    private void clearPath() {
        TunnelsRecordPerTopology.getInstance().remove(topoId, tunnelUnifyKey);
        TunnelsRecordPerPort.getInstance().update(new TunnelUnifyRecordKey(tunnelUnifyKey), path, null);

        PceResult result = new PceResult();
        if (teArg != null && teArg.getBandWidth() != 0) {
            BandWidthMng.getInstance().free(path, teArg.getHoldPriority(), tunnelUnifyKey, result, isBiDirect());
        }

        if (bwSharedGroups != null) {
            BwSharedGroupMng.getInstance()
                    .delTunnelAndFreeBw(path, tunnelUnifyKey, bwSharedGroups, isBiDirect(), result);
        }

        if (result.isNeedRefreshUnestablishTunnels()) {
            PcePathProvider.getInstance()
                    .refreshUnestablishTunnels(tunnelUnifyKey.isSimulate(), topoId, tunnelUnifyKey, getHoldPriority());
        }

        TopoServiceAdapter.getInstance().getPceTopoProvider().removeTunnelInstance(tunnelUnifyKey);
    }
}
