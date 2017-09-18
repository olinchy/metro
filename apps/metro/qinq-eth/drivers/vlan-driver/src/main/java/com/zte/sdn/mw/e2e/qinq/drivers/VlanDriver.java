/*
 * Copyright (c) 2017 ZTE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.qinq.drivers;

import org.opendaylight.yang.gen.v1.http.www.zte.com.cn.zxr10.netconf.schema.rosng.switchvlan.rev160711.Configuration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.Model;
import com.zte.mw.sdn.connection.Connection;
import com.zte.mw.sdn.connection.Driver;

public class VlanDriver implements Driver, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(VlanDriver.class);

    @Override
    public void close() throws Exception {
        LOG.info("VlanDriverProvider Closed");
    }

    @Override
    public void config(final Model model, final Connection connection) {
        InstanceIdentifier<Configuration> identifier = InstanceIdentifier.builder(Configuration.class).build();
        connection.config(identifier, model.get(Configuration.class), Model.OperationType.CREATE);
    }
}
