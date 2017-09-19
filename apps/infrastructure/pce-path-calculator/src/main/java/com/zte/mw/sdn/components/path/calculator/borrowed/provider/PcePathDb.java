/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import edu.uci.ics.jung.graph.Graph;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.ServiceHsbPathInstanceData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.ServiceHsbPathInstanceDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.ServicePathInstanceData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.ServicePathInstanceDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.TunnelGroupPathInstanceData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.TunnelGroupPathInstanceDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.TunnelHsbPathInstanceData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.TunnelHsbPathInstanceDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.TunnelPathInstanceData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.TunnelPathInstanceDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicehsbpathinstancedata.ServiceHsbPathsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicehsbpathinstancedata.ServiceHsbPathsDataKey;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicepathinstancedata.ServicePathsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.servicepathinstancedata.ServicePathsDataKey;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.lsp.data.ExcludingNode;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.lsp.data.ExcludingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.lsp.data.ExcludingPort;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.lsp.data.ExcludingPortBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.lsp.data.NextAddress;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.lsp.data.NextAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.lsp.data.TryToAvoidLink;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.lsp.data.TryToAvoidLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelgrouppathinstancedata.TunnelGroupsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelgrouppathinstancedata.TunnelGroupsDataKey;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelhsbpathinstancedata.TunnelHsbsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelhsbpathinstancedata.TunnelHsbsDataKey;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelpathinstancedata.TunnelPathsData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.tunnelpathinstancedata.TunnelPathsDataKey;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.links.PathLink;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.links.PathLinkBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.TunnelGroupPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelhsbpath.TunnelHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.DataBrokerDelegate;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.PortKey;
import com.zte.ngip.ipsdn.pce.path.core.topology.TopoServiceAdapter;

public class PcePathDb {
    public PcePathDb() {
        dataBroker = DataBrokerDelegate.getInstance();
    }

    private static final Logger LOG = LoggerFactory.getLogger(PcePathDb.class);
    private static PcePathDb instance = new PcePathDb();
    public final DataBrokerDelegate dataBroker;

    public static PcePathDb getInstance() {
        return instance;
    }

    public static List<PathLink> pathLinkToLinkConvert(List<Link> links) {

        List<PathLink> pathLinks = new ArrayList<>();
        if ((null == links) || (links.isEmpty())) {
            return pathLinks;
        }
        for (int i = 0; i < links.size(); i++) {
            PathLink pathLink = new PathLinkBuilder()
                    .setLinkId(links.get(i).getLinkId())
                    .setSupportingLink(links.get(i).getSupportingLink())
                    .setSource(links.get(i).getSource())
                    .setDestination(links.get(i).getDestination())
                    .build();
            pathLinks.add(i, pathLink);
        }
        return pathLinks;
    }

    public static List<ExcludingNode> excludeNodeConvert(Set<NodeId> nodes) {
        List<ExcludingNode> excludingNodes = new ArrayList<>();
        if ((null == nodes) || (nodes.isEmpty())) {
            return excludingNodes;
        }
        for (NodeId node : nodes) {
            ExcludingNode excludingNode = new ExcludingNodeBuilder()
                    .setNodeId(node)
                    .build();
            excludingNodes.add(excludingNode);
        }
        return excludingNodes;
    }

    public static List<ExcludingPort> excludePortConvert(Set<PortKey> ports) {
        List<ExcludingPort> excludingPorts = new ArrayList<>();
        if ((null == ports) || (ports.isEmpty())) {
            return excludingPorts;
        }
        for (PortKey portKey : ports) {
            ExcludingPort excludingPort = new ExcludingPortBuilder()
                    .setNodeId(portKey.getNode())
                    .setTpId(portKey.getTp())
                    .build();
            excludingPorts.add(excludingPort);
        }
        return excludingPorts;
    }

    public static List<NextAddress> nextAddressConvert(
            List<org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.te.argument.lsp.NextAddress>
                    addresses) {

        List<NextAddress> nextAddresses = new ArrayList<>();
        if ((null == addresses) || (addresses.isEmpty())) {
            return nextAddresses;
        }

        for (org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.te.argument.lsp.NextAddress
                nextAddress : addresses) {
            NextAddress address = new NextAddressBuilder()
                    .setDestination(nextAddress.getDestination())
                    .setLinkId(nextAddress.getLinkId())
                    .setSource(nextAddress.getSource())
                    .setStrict(nextAddress.isStrict())
                    .setSupportingLink(nextAddress.getSupportingLink())
                    .build();
            nextAddresses.add(address);
        }
        return nextAddresses;
    }

    public static List<TryToAvoidLink> tryToAvoidLinkConvert(
            List<org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.te.argument.lsp.TryToAvoidLink>
                    tryToAvoidLinks) {
        List<TryToAvoidLink> destList = new ArrayList<>();

        if ((null == tryToAvoidLinks) || (tryToAvoidLinks.isEmpty())) {
            return destList;
        }

        for (int i = 0; i < tryToAvoidLinks.size(); i++) {
            TryToAvoidLink builder = new TryToAvoidLinkBuilder()
                    .setDestination(tryToAvoidLinks.get(i).getDestination())
                    .setLinkId(tryToAvoidLinks.get(i).getLinkId())
                    .setSource(tryToAvoidLinks.get(i).getSource())
                    .setSupportingLink(tryToAvoidLinks.get(i).getSupportingLink())
                    .build();
            destList.add(i, builder);
        }
        return destList;
    }

    public static InstanceIdentifier<TunnelHsbsData> buildtunnelHsbDbPath(NodeId headNodeId, Long tunnelId) {
        return InstanceIdentifier
                .create(TunnelHsbPathInstanceData.class)
                .child(TunnelHsbsData.class, new TunnelHsbsDataKey(headNodeId, tunnelId));
    }

    public static InstanceIdentifier<ServicePathsData> buildServicePathDbPath(NodeId headNodeId, String serviceName) {
        return InstanceIdentifier.create(ServicePathInstanceData.class)
                .child(ServicePathsData.class, new ServicePathsDataKey(headNodeId, serviceName));
    }

    public static InstanceIdentifier<ServiceHsbPathsData> buildServiceHsbPathDbPath(
            NodeId headNodeId,
            String serviceName) {
        return InstanceIdentifier.create(ServiceHsbPathInstanceData.class)
                .child(ServiceHsbPathsData.class, new ServiceHsbPathsDataKey(headNodeId, serviceName));
    }

    public static InstanceIdentifier<TunnelGroupsData> buildTgDbPath(NodeId headNodeId, Long tunnelGroupId) {
        return InstanceIdentifier
                .create(TunnelGroupPathInstanceData.class)
                .child(TunnelGroupsData.class, new TunnelGroupsDataKey(headNodeId, tunnelGroupId));
    }

    public static InstanceIdentifier<TunnelPathsData> buildtunnelPathDbPath(NodeId headNodeId, int tunnelId) {
        return InstanceIdentifier
                .create(TunnelPathInstanceData.class)
                .child(TunnelPathsData.class, new TunnelPathsDataKey(headNodeId, tunnelId));
    }

    public static InstanceIdentifier<TunnelGroupPathInstanceData> buildTgsDbRootPath() {
        return InstanceIdentifier.create(TunnelGroupPathInstanceData.class);
    }

    public TunnelGroupsData tnnlGroupPathInstanceReadDb(NodeId nodeId, Long tunnelGroupId) {
        Optional<TunnelGroupsData> tunnelGroupsData;
        try {
            tunnelGroupsData = dataBroker.read(
                    LogicalDatastoreType.CONFIGURATION,
                    buildTgDbPath(nodeId, tunnelGroupId)).get();
            if (tunnelGroupsData.isPresent()) {
                return tunnelGroupsData.get();
            } else {
                LOG.debug(" tnnlGroupDbRecovery read db failed");
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.debug("tnnlGroupDbRecovery read db failed " + e);
            return null;
        }
        return null;
    }

    public TunnelPathsData tnnlPathInstanceReadDb(NodeId nodeId, int tunnelId) {
        Optional<TunnelPathsData> tunnelPathsData;
        try {
            tunnelPathsData = dataBroker.read(
                    LogicalDatastoreType.CONFIGURATION,
                    buildtunnelPathDbPath(nodeId, tunnelId)).get();
            if (tunnelPathsData.isPresent()) {
                return tunnelPathsData.get();
            } else {
                LOG.debug("tnnlPathDbRecovery read failed ");
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.debug("tnnlPathDbRecovery read failed " + e);
            return null;
        }
        return null;
    }

    public void tunnelGroupWriteDbRoot() {
        dataBroker.put(LogicalDatastoreType.CONFIGURATION, buildTgsDbRootPath(),
                       new TunnelGroupPathInstanceDataBuilder().build());
    }

    public void tunnelPathWriteDbRoot() {
        dataBroker.put(LogicalDatastoreType.CONFIGURATION, buildTnnlPathDbRootPath(),
                       new TunnelPathInstanceDataBuilder().build());
    }

    public InstanceIdentifier<TunnelPathInstanceData> buildTnnlPathDbRootPath() {
        return InstanceIdentifier.create(TunnelPathInstanceData.class);
    }

    public void tunnelHsbWriteDbRoot() {
        dataBroker.put(LogicalDatastoreType.CONFIGURATION, buildTunnelHsbDbRootPath(),
                       new TunnelHsbPathInstanceDataBuilder().build());
    }

    public InstanceIdentifier<TunnelHsbPathInstanceData> buildTunnelHsbDbRootPath() {
        return InstanceIdentifier.create(TunnelHsbPathInstanceData.class);
    }

    public void serviceWriteDbRoot() {
        dataBroker.put(LogicalDatastoreType.CONFIGURATION, buildServiceDbRootPath(),
                       new ServicePathInstanceDataBuilder().build());
    }

    public InstanceIdentifier<ServicePathInstanceData> buildServiceDbRootPath() {
        return InstanceIdentifier.create(ServicePathInstanceData.class);
    }

    public void serviceHsbWriteDbRoot() {
        dataBroker.put(LogicalDatastoreType.CONFIGURATION, buildServiceHsbDbRootPath(),
                       new ServiceHsbPathInstanceDataBuilder().build());
    }

    public InstanceIdentifier<ServiceHsbPathInstanceData> buildServiceHsbDbRootPath() {
        return InstanceIdentifier.create(ServiceHsbPathInstanceData.class);
    }

    public TunnelGroupPathInstance tunnelGroupPathConvert(TunnelGroupsData dbData) {
        if (null == dbData) {
            return null;
        }
        return new TunnelGroupPathInstance(dbData);
    }

    public TunnelPathInstance tunnelPathConvert(TunnelPathsData dbData) {
        if (null == dbData) {
            return null;
        }
        return new TunnelPathInstance(dbData);
    }

    public TunnelHsbPathInstance tunnelHsbPathConvert(TunnelHsbsData dbData) {
        if (null == dbData) {
            return null;
        }
        return new TunnelHsbPathInstance(dbData);
    }

    public List<Link> pathLinks2Links(TopologyId topoId, boolean isSimulate, List<PathLink> pathLinks) {
        List<Link> links = new LinkedList<>();
        if ((null == pathLinks) || (pathLinks.isEmpty())) {
            return links;
        }

        Graph<NodeId, Link> graph =
                TopoServiceAdapter.getInstance().getPceTopoProvider().getTopoGraph(isSimulate, topoId);
        for (PathLink link : pathLinks) {
            Link dstLink = ComUtility.getLink4Path(graph,
                                                   link.getSource().getSourceNode(), link.getSource().getSourceTp(),
                                                   link.getDestination().getDestNode(),
                                                   link.getDestination().getDestTp());
            if (dstLink == null) {
                LOG.error("pathLinks2Links error!" + link.toString());
                return links;
            }
            links.add(dstLink);
        }
        return links;
    }
}
