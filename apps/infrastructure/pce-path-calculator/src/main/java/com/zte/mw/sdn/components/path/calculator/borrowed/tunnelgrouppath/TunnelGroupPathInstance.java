/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath;

import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tnnlgroupdata.MasterPathsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tnnlgroupdata.MasterTunnelUnifyKey;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tnnlgroupdata.MasterTunnelUnifyKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tnnlgroupdata.SlavePathsBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tnnlgroupdata.SlaveTunnelUnifyKey;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tnnlgroupdata.SlaveTunnelUnifyKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelgrouppathinstancedata.TunnelGroupsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelgrouppathinstancedata.TunnelGroupsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelgrouppathinstancedata.TunnelGroupsDataKey;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelGroupPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TunnelGroupPathUpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.tunnel.group.path.update.TgMasterPathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.tunnel.group.path.update.TgSlavePathBuilder;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.NotificationProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathDb;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.srlg.PriorityAvoidLinks;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.DataBrokerDelegate;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.MplsLinkTools;

import com.zte.ngip.ipsdn.pce.path.api.srlg.AovidLinks;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBeanLsp;

public class TunnelGroupPathInstance extends HsbPathInstance {
    /**
     * TunnelGroupPathInstance.
     *
     * @param input input
     */
    public TunnelGroupPathInstance(CreateTunnelGroupPathInput input) {
        super(input.getHeadNodeId(), input.getTailNodeId(),
              ComUtility.getTopoId(input.getTopologyId()),
              getMasterTunnelUnifyKey(input.getHeadNodeId(), input.getTunnelGroupId().intValue()),
              getSlaveTunnelUnifyKey(input.getHeadNodeId(), input.getTunnelGroupId().intValue()),
              new TeArgumentBean(input, ComUtility.getTopoId(input.getTopologyId())));
        this.tunnelGroupId = input.getTunnelGroupId();
        this.teArgSlave = new TeArgumentBeanLsp();
        setCalculateStrategy(null);
        setRecalcWithoutDelay(null);
        calcPath();
    }

    /**
     * TunnelGroupPathInstance.
     *
     * @param dbData dbData
     */
    public TunnelGroupPathInstance(TunnelGroupsData dbData) {
        this.topoId = dbData.getTopologyId();
        this.headNodeId = dbData.getHeadNodeId();
        this.tailNodeId = dbData.getTailNodeId();
        this.tunnelGroupId = dbData.getTunnelGroupId();
        this.masterPath = PcePathDb.getInstance().pathLinks2Links(topoId, false, dbData.getMasterPaths().getPathLink());
        this.slavePath = PcePathDb.getInstance().pathLinks2Links(topoId, false, dbData.getSlavePaths().getPathLink());
        this.teArg = setTeArgInfo(dbData);
        this.masterTunnelUnifyKey =
                getMasterTunnelUnifyKey(dbData.getHeadNodeId(), dbData.getTunnelGroupId().intValue());
        this.slaveTunnelUnifyKey = getSlaveTunnelUnifyKey(dbData.getHeadNodeId(), dbData.getTunnelGroupId().intValue());
        this.calculateStrategy = dbData.getCalculateStrategyContainer();
        this.recalcWithoutDelay = dbData.isRecalcWithoutDelay();
    }

    private Long tunnelGroupId;

    private static TunnelUnifyKey getMasterTunnelUnifyKey(NodeId nodeId, int tunnelId) {
        return new TunnelUnifyKey(nodeId, tunnelId, true, true, false);
    }

    private static TunnelUnifyKey getSlaveTunnelUnifyKey(NodeId nodeId, int tunnelId) {
        return new TunnelUnifyKey(nodeId, tunnelId, true, false, false);
    }

    private static TeArgumentBean setTeArgInfo(TunnelGroupsData dbData) {
        TeArgumentBean teAttr = new TeArgumentBean(dbData.getTeArgCommonData());
        teAttr.setExcludedNodes(dbData.getExcludingNode());
        teAttr.setExcludedPorts(dbData.getExcludingPort());
        teAttr.setNextAddress(dbData.getNextAddress());
        teAttr.setTryToAvoidLink(dbData.getTryToAvoidLink());
        teAttr.setContrainedAddress(dbData.getContrainedAddress(), dbData.getTopologyId());
        return teAttr;
    }

    @Override
    public void printSummaryInfo() {
        ComUtility.debugInfoLog("tunnelGroupId:" + tunnelGroupId);
        super.printSummaryInfo();
    }

    @Override
    public void printDetailInfo() {
        ComUtility.debugInfoLog("tunnelGroupId:" + tunnelGroupId);
        super.printDetailInfo();
    }

    @Override
    public boolean isSlaveIndependent() {
        return true;
    }

    @Override
    public PriorityAvoidLinks generatePriorityAvoidLinks(TeArgumentBean teArgBean) {
        List<Link> avoidMasterPath = ComUtility.getTryToAvoidLinkForHsb(masterPath);
        List<Link> manualAvoidLinks = teArgBean.getTryToAvoidLinkInLinks();

        PriorityAvoidLinks priorityAvoidLinks = new PriorityAvoidLinks();
        priorityAvoidLinks.addAvoidLinks(new AovidLinks(manualAvoidLinks));
        priorityAvoidLinks.addAvoidLinks(new AovidLinks(avoidMasterPath));
        return priorityAvoidLinks;
    }

    @Override
    public void notifyPathChange() {
        TunnelGroupPathUpdateBuilder builder = new TunnelGroupPathUpdateBuilder()
                .setHeadNodeId(headNodeId)
                .setTunnelGroupId(tunnelGroupId)
                .setTgMasterPath(new TgMasterPathBuilder()
                                         .setPathLink(MplsLinkTools.getMplsLinkPath(masterPath)).build())
                .setTgSlavePath(new TgSlavePathBuilder().setPathLink(MplsLinkTools.getMplsLinkPath(slavePath)).build());

        NotificationProvider.getInstance().notify(builder.build());
    }

    @Override
    public void writeDb() {
        DataBrokerDelegate.getInstance().put(
                LogicalDatastoreType.CONFIGURATION,
                PcePathDb.buildTgDbPath(getHeadNodeId(), getTunnelGroupId()),
                tunnelGroupsDataCreate());
    }

    @Override
    public void removeDb() {
        DataBrokerDelegate.getInstance().delete(
                LogicalDatastoreType.CONFIGURATION,
                PcePathDb.buildTgDbPath(getHeadNodeId(), getTunnelGroupId()));
    }

    @Override
    public void writeMemory() {
        PcePathProvider.getInstance().updateTunnel(this);
    }

    @Override
    public void notifySegmentsChange() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSimulate() {
        return false;
    }

    public Long getTunnelGroupId() {
        return this.tunnelGroupId;
    }

    private TunnelGroupsData tunnelGroupsDataCreate() {

        MasterTunnelUnifyKey masterTunnelUnifyKey = new MasterTunnelUnifyKeyBuilder()
                .setIsMaster(getMasterTunnelUnifyKey().isMaster())
                .setIsTg(getMasterTunnelUnifyKey().isTg())
                .build();

        SlaveTunnelUnifyKey slaveTunnelUnifyKey = new SlaveTunnelUnifyKeyBuilder()
                .setIsMaster(getSlaveTunnelUnifyKey().isMaster())
                .setIsTg(getSlaveTunnelUnifyKey().isTg())
                .build();

        return new TunnelGroupsDataBuilder()
                .setKey(new TunnelGroupsDataKey(getHeadNodeId(), getTunnelGroupId()))
                .setTunnelGroupId(getTunnelGroupId())
                .setTryToAvoidLink(PcePathDb.tryToAvoidLinkConvert(getTeArgumentBean().getTryToAvoidLink()))
                .setTeArgCommonData(getTeArgumentBean().getArgComm())
                .setHeadNodeId(getHeadNodeId())
                .setTailNodeId(getTailNodeId())
                .setMasterPaths(new MasterPathsBuilder()
                                        .setPathLink(PcePathDb.pathLinkToLinkConvert(getMasterLsp())).build())
                .setSlavePaths(new SlavePathsBuilder()
                                       .setPathLink(PcePathDb.pathLinkToLinkConvert(getSlaveLsp())).build())
                .setMasterTunnelUnifyKey(masterTunnelUnifyKey)
                .setSlaveTunnelUnifyKey(slaveTunnelUnifyKey)
                .setExcludingNode(PcePathDb.excludeNodeConvert(getTeArgumentBean().getExcludedNodes()))
                .setExcludingPort(PcePathDb.excludePortConvert(getTeArgumentBean().getExcludedPorts()))
                .setNextAddress(PcePathDb.nextAddressConvert(getTeArgumentBean().getNextAddress()))
                .setTopologyId(getTopoId())
                .setCalculateStrategyContainer(getCalculateStrategy())
                .setRecalcWithoutDelay(recalcWithoutDelay)
                .build();
    }

    public boolean isMatched(NodeId headNodeId, int groupId) {
        return (this.headNodeId.equals(headNodeId)) && (this.tunnelGroupId == groupId);
    }
}
