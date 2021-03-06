/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.connections.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.Model;
import com.zte.mw.sdn.connection.Connection;

public class MountPointConnection implements Connection {
    public MountPointConnection(MountPointService mountPointService, String neIdentity) {
        MountPoint mountPoint = mountPointService.getMountPoint(path(neIdentity)).get();
        this.dataBroker = mountPoint.getService(DataBroker.class).get();
    }

    private static final Logger LOG = LoggerFactory.getLogger(MountPointConnection.class);
    private final DataBroker dataBroker;

    private InstanceIdentifier<Node> path(final String neIdentity) {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())))
                .child(Node.class, new NodeKey(new NodeId(neIdentity)));
    }

    @Override
    public <T extends DataObject> void config(InstanceIdentifier<T> identifier, T data, Model.OperationType oper) {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        try {
            transaction.merge(LogicalDatastoreType.CONFIGURATION, identifier, data);
            transaction.submit().checkedGet();
        } catch (TransactionCommitFailedException | IllegalStateException e) {
            transaction.cancel();
            LOG.warn("config to device failed! caught ", e);
        }
    }
}
