/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.ngip.ipsdn.impl;

import com.zte.ngip.ipsdn.impl.utils.DataBrokerProvider;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.function.mw.vlan.function.rev170911.CreateVlanInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.function.mw.vlan.function.rev170911.DeleteVlanInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.function.mw.vlan.function.rev170911.MwVlanFunctionService;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.MwL2vpn;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.mw.l2vpn.QinqServiceInstance;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.mw.l2vpn.QinqServiceInstanceKey;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.qinq.service.path.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by xudong on 17-9-12.
 */
public class ConfigVlanFunction implements MwVlanFunctionService {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigVlanFunction.class);
    private static ConfigVlanFunction instance = new ConfigVlanFunction();

    private ConfigVlanFunction() {
    }

    public static ConfigVlanFunction getInstance() {
        return instance;
    }

    @Override
    public Future<RpcResult<Void>> deleteVlan(DeleteVlanInput input) {

        return null;
    }

    @Override
    public Future<RpcResult<Void>> createVlan(CreateVlanInput input) {
        InstanceIdentifier<QinqServiceInstance> qinqServiceInstancePath = InstanceIdentifier.create(MwL2vpn.class)
                .child(QinqServiceInstance.class, new QinqServiceInstanceKey(input.getName(), input.getTxid()));
        QinqServiceInstance qinqServiceInstance = DataBrokerProvider.getInstance().readConfiguration(qinqServiceInstancePath);

        List<Link> linkList = qinqServiceInstance.getPath().getLink();
        for (Link link: linkList) {
            link.getDestination().getDestNode();
        }


        return null;
    }
}
