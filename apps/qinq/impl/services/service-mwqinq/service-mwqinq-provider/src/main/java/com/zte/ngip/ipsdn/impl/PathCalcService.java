/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.ngip.ipsdn.impl;

import com.zte.ngip.ipsdn.impl.utils.DataBrokerProvider;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.function.mw.path.function.rev170911.CreatePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.function.mw.path.function.rev170911.DeletePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.function.mw.path.function.rev170911.MwPathFunctionService;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.MwL2vpn;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.QinqService;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.mw.l2vpn.QinqServiceInstance;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.service.mwqinq.qinq.service.model.rev170905.mw.l2vpn.QinqServiceInstanceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

/**
 * Created by root on 17-9-13.
 */
public class PathCalcService implements MwPathFunctionService{
    private static final Logger LOG = LoggerFactory.getLogger(PathCalcService.class);
    private static PathCalcService instance = new PathCalcService();

    private PathCalcService() {
    }

    public static PathCalcService getInstance() {
        return instance;
    }

    @Override
    public Future<RpcResult<Void>> deletePath(DeletePathInput input) {

        InstanceIdentifier<QinqServiceInstance> path = InstanceIdentifier.create(MwL2vpn.class)
                .child(QinqServiceInstance.class, new QinqServiceInstanceKey(input.getName(), input.getTxid()));
        QinqServiceInstance qinqServiceInstance = DataBrokerProvider.getInstance().readConfiguration(path);


        return null;
    }

    @Override
    public Future<RpcResult<Void>> createPath(CreatePathInput input) {
        return null;
    }
}
