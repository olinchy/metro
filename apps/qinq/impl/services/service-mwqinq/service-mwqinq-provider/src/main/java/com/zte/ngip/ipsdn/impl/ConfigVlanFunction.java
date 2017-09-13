/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.ngip.ipsdn.impl;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.function.mw.vlan.function.rev170911.CreateVlanInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.function.mw.vlan.function.rev170911.DeleteVlanInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.function.mw.vlan.function.rev170911.MwVlanFunctionService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        return null;
    }
}
