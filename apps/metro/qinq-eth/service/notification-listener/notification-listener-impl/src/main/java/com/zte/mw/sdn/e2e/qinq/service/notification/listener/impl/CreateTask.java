/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.e2e.qinq.service.notification.listener.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.l2vpn.svc.rev170622.SvcId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.l2vpn.svc.rev170622.l2vpn.svc.Sites;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.l2vpn.svc.rev170622.l2vpn.svc.sites.Site;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.l2vpn.svc.rev170622.l2vpn.svc.sites.site.site.network.accesses.SiteNetworkAccesse;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.l2vpn.svc.rev170622.l2vpn.svc.vpn.services.VpnSvc;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.l2vpn.svc.rev170622.vpn.attachment.grouping.vpn.attachment.attachment.flavor.VpnFlavor;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.EvcType;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.MwL2vpn;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.MwL2vpnBuilder;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.mw.l2vpn.QinqServiceInstance;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.mw.l2vpn.QinqServiceInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.mw.l2vpn.QinqServiceInstanceKey;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.qinq.service.Ac;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.qinq.service.AcBuilder;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.qinq.service.Tunnel;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.qinq.service.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.qinq.service.path.Link;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.qinq.service.path.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.qinq.service.path.LinkKey;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.service.identifier.access.action.Push;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.service.identifier.access.action.PushBuilder;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.service.identifier.access.type.Vlanrange;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.service.identifier.access.type.VlanrangeBuilder;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.service.identifier.access.type.vlanrange.VlanRange;
import org.opendaylight.yang.gen.v1.urn.mw.metro.qinq.service.mwqinq.qinq.service.model.rev170905.service.identifier.access.type.vlanrange.VlanRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.links.PathLink;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.Model;
import com.zte.mw.sdn.Result;
import com.zte.mw.sdn.components.databroker.DataBrokerProvider;
import com.zte.mw.sdn.e2e.runtime.MicrowaveRuntime;
import com.zte.mw.sdn.infrastructure.task.MonitoredTask;
import com.zte.mw.sdn.infrastructure.task.SelfScheduledTask;

public class CreateTask extends SelfScheduledTask {
    public CreateTask(final Map<InstanceIdentifier<?>, DataObject> createdData, MicrowaveRuntime runtime) {
        super(runtime.getConfigurationPool());
        this.runtime = runtime;
        this.createData = createdData;
    }

    private static final Logger LOG = LoggerFactory.getLogger(CreateTask.class);
    private final MicrowaveRuntime runtime;
    private final Map<InstanceIdentifier<?>, DataObject> createData;

    @Override
    protected void execute() {
        List<VpnSvc> toBeCreate =
                createData.entrySet().stream().map(Map.Entry::getValue).filter(value -> value instanceof VpnSvc)
                        .peek(vpn -> LOG.info("create vpn service of microwave", vpn))
                        .map(dataLink -> (VpnSvc) dataLink).collect(Collectors.toList());
        for (VpnSvc vpnSvc : toBeCreate) {
            subTasks.addAll(toDeviceTasks(vpnSvc));
        }
    }

    @Override
    protected void postException(final Exception exception) {
        LOG.warn("create " + createData + " caught exception", exception);
    }

    private Collection<? extends MonitoredTask> toDeviceTasks(final VpnSvc vpnSvc) {

        ArrayList<SouthTask> southTasks = new ArrayList<>();
        List<Model> modelOnNodes = toNodeModel(vpnSvc);

        for (Model model : modelOnNodes) {
            southTasks.add(new SouthTask(observer, model, runtime));
        }

        return southTasks;
    }

    private List<Model> toNodeModel(final VpnSvc vpnSvc) {

        // TODO: 17-9-16 fuction --> vpnservice ==> node
        // create model of nodes
        // path calculation
        // persist

        toQiniServiceModel(vpnSvc);

        return null;
    }

    public List<Model> toQiniServiceModel(VpnSvc vpnSvc) {

        persistQinqServiceModel(vpnSvc);

        List<Model> models = new ArrayList<Model>();
        return models;
    }

    public void persistQinqServiceModel(VpnSvc vpnSvc) {
        List<QinqServiceInstance> qinqServiceInstances = new ArrayList<QinqServiceInstance>();

        SvcId vpnId = vpnSvc.getVpnId();
        //Ovc number is 1 as default
        String svlan = vpnSvc.getOvc().getOvcList().get(0).getSvlanIdEthernetTag();

        Tunnel tunnel = new TunnelBuilder()
                .setTunnelId(Long.valueOf(svlan))
                .setEvcSvlan(Integer.valueOf(svlan))
                .build();

        List<Ac> acs = constructAcList(vpnId, svlan);
        List<Link> links = null;

        String vpnType = vpnSvc.getVpnType().getSimpleName();
        EvcType evcType = null;
        if (vpnType.equals("Epl")) {
            evcType = EvcType.EPLAN;
        } else if (vpnType.equals("Evpl")) {
            evcType = EvcType.EVPLINE;
            //links = constructEvplLinkList(acs);
        } else if (vpnType.equals("EpLan")) {
            evcType = EvcType.EPLAN;
        } else if (vpnType.equals("EvpLan")) {
            evcType = EvcType.EVPLAN;
        }

        QinqServiceInstance qinqServiceInstance = new QinqServiceInstanceBuilder()
                .setName(svlan)
                .setTxid(svlan)
                .setDescription("")
                .setEvcType(evcType)
                .setAc(acs)
                //.setPath(new PathBuilder().setLink(links).build())
                .setTunnel(tunnel)
                .setKey(new QinqServiceInstanceKey(svlan, svlan))
                .build();

        qinqServiceInstances.add(qinqServiceInstance);

        MwL2vpn mwL2vpn = new MwL2vpnBuilder()
                .setQinqServiceInstance(qinqServiceInstances)
                .build();

        InstanceIdentifier<MwL2vpn> mwL2vpnInstanceIdentifier = InstanceIdentifier.create(MwL2vpn.class);
        DataBrokerProvider.getInstance().mergeConfiguration(mwL2vpnInstanceIdentifier, mwL2vpn);
    }

    public List<Ac> constructAcList(SvcId vpnId, String svlan) {
        List<Ac> acs = new ArrayList<Ac>();

        InstanceIdentifier<Sites> sitesInstanceIdentifier = InstanceIdentifier.create(Sites.class);
        Sites sites = null;
        DataBrokerProvider.getInstance().readConfiguration(sitesInstanceIdentifier);
        for (Site site : sites.getSite()) {
            List<SiteNetworkAccesse> siteNetworkAccesses = site.getSiteNetworkAccesses().getSiteNetworkAccesse();
            for (SiteNetworkAccesse siteNetworkAccesse : siteNetworkAccesses) {
                VpnFlavor vpnFlavor = (VpnFlavor) siteNetworkAccesse.getVpnAttachment().getAttachmentFlavor();
                SvcId vpnIdRef = vpnFlavor.getVpnFlavor().get(0).getVpnId();

                if (vpnId.equals(vpnIdRef)) {
                    String portId = siteNetworkAccesse.getNetworkAccessId();
                    Integer vlan = Math.toIntExact(siteNetworkAccesse.getConnection().getVlan().getVlanId());

                    List<VlanRange> vlanRanges = new ArrayList<VlanRange>();

                    VlanRange vlanRange = new VlanRangeBuilder()
                            .setVlanBegin(vlan)
                            .setVlanEnd(vlan)
                            .build();
                    vlanRanges.add(vlanRange);

                    Vlanrange vlanrange = new VlanrangeBuilder()
                            .setVlanRange(vlanRanges)
                            .build();

                    Push push = new PushBuilder()
                            .setPushVlanId(Integer.valueOf(svlan))
                            .build();

                    Ac ac = new AcBuilder()
                            .setAcNodeid(new NodeId(site.getSiteId()))
                            .setPortId(portId)
                            .setName(portId)
                            .setAccessType(vlanrange)
                            .setAccessAction(push)
                            .build();

                    acs.add(ac);
                }
            }
        }

        return acs;
    }

    public List<Link> constructEvplLinkList(List<Ac> acs) {

        List<Link> links = new ArrayList<Link>();

        if (acs.size() != 2) {
            return null;
        }

        CreateTunnelPathInput tunnelPathInput = new CreateTunnelPathInputBuilder()
                .setHeadNodeId(acs.get(0).getAcNodeid())
                .setTailNodeId(acs.get(1).getAcNodeid())
                .setTunnelId(1L)
                .setTopologyId(TopologyId.getDefaultInstance("L2")) //TODO
                .build();

        List<PathLink> pathLinks = null;
        try {
            CreateTunnelPathOutput output = PcePathCalculatorHolder.instance().getCalculator().createTunnelPath(
                    tunnelPathInput);

            pathLinks = output.getTunnelPath().getPathLink();
        } catch (InterruptedException exp) {
            LOG.error("PathCalcService createPath InterruptedException ", exp);
        } catch (ExecutionException exp) {
            LOG.error("PathCalcService createPath ExecutionException ", exp);
        }

        for (PathLink pathLink : pathLinks) {
            Link link = new LinkBuilder()
                    .setSource(pathLink.getSource())
                    .setDestination(pathLink.getDestination())
                    .setLinkId(pathLink.getLinkId())
                    .setSupportingLink(pathLink.getSupportingLink())
                    .setKey(new LinkKey(pathLink.getLinkId()))
                    .build();

            links.add(link);
        }

        return links;
    }

    @Override
    protected void postWithResults(final ArrayList<Result> results) {
        LOG.info(String.valueOf(results));
    }
}