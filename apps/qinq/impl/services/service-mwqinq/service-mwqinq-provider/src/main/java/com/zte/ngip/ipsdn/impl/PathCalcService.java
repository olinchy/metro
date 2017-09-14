/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.ngip.ipsdn.impl;

import com.zte.ngip.ipsdn.impl.utils.DataBrokerProvider;
import com.zte.ngip.ipsdn.pce.path.impl.provider.PcePathProvider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.function.mw.path.function.rev170911.CreatePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.function.mw.path.function.rev170911.DeletePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.function.mw.path.function.rev170911.MwPathFunctionService;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.links.PathLink;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.EvcType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.MwL2vpn;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.mw.l2vpn.QinqServiceInstance;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.mw.l2vpn.QinqServiceInstanceKey;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.qinq.service.Ac;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.qinq.service.Path;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.qinq.service.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.qinq.service.path.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.qinq.service.path.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.qinq.service.path.LinkKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by root on 17-9-13.
 */
public class PathCalcService implements MwPathFunctionService {
    private static final Logger LOG = LoggerFactory.getLogger(PathCalcService.class);
    private static PathCalcService instance = new PathCalcService();

    private PathCalcService() {
    }

    public static PathCalcService getInstance() {
        return instance;
    }

    @Override
    public Future<RpcResult<Void>> deletePath(DeletePathInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> createPath(CreatePathInput input) {

        InstanceIdentifier<QinqServiceInstance> qinqServiceInstancePath = InstanceIdentifier.create(MwL2vpn.class)
                .child(QinqServiceInstance.class, new QinqServiceInstanceKey(input.getName(), input.getTxid()));
        QinqServiceInstance qinqServiceInstance = DataBrokerProvider.getInstance().readConfiguration(qinqServiceInstancePath);

        List<Ac> acList = qinqServiceInstance.getAc();

        if (qinqServiceInstance.getEvcType() != EvcType.EVPLINE) {
            //TODO
            return null;
        }

        CreateTunnelPathInput tunnelPathInput = new CreateTunnelPathInputBuilder()
                .setHeadNodeId(acList.get(0).getAcNodeid())
                .setTailNodeId(acList.get(1).getAcNodeid())
                .setTunnelId(qinqServiceInstance.getTunnel().getTunnelId())
                .setTopologyId(TopologyId.getDefaultInstance("L2")) //TODO
                .build();


        List<PathLink> pathLinks = null;
        try {
            Future<RpcResult<CreateTunnelPathOutput>> output = PcePathProvider.getInstance().createTunnelPath(tunnelPathInput);

            pathLinks = output.get().getResult().getTunnelPath().getPathLink();
        } catch (InterruptedException exp) {
            LOG.error("PathCalcService createPath InterruptedException ", exp);
            exp.printStackTrace();
        } catch (ExecutionException exp) {
            LOG.error("PathCalcService createPath ExecutionException ", exp);
            exp.printStackTrace();
        }

        List<Link> linkToWrite = new ArrayList<Link>();
        for (PathLink pathLink : pathLinks) {
            Link link = new LinkBuilder()
                    .setSource(pathLink.getSource())
                    .setDestination(pathLink.getDestination())
                    .setLinkId(pathLink.getLinkId())
                    .setSupportingLink(pathLink.getSupportingLink())
                    .setKey(new LinkKey(pathLink.getLinkId()))
                    .build();

            linkToWrite.add(link);
        }

        InstanceIdentifier<Path> path = qinqServiceInstancePath.child(Path.class);
        Path pathToWrite = new PathBuilder().setLink(linkToWrite).build();

        DataBrokerProvider.getInstance().mergeConfiguration(path, pathToWrite);

        return null;
    }
}
