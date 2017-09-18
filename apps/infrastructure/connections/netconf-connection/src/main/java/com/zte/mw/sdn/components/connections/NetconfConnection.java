/*
 * Copyright (c) 2017 ZTE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.connections;

import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.connection.Connection;

public class NetconfConnection implements AutoCloseable, ConnectionProvider {
    public NetconfConnection(final MountPointService mountPointService) {
        this.mountPointService = mountPointService;
        LOG.info("mountPointService is " + mountPointService.toString());
    }

    private static final Logger LOG = LoggerFactory.getLogger(NetconfConnection.class);
    private final MountPointService mountPointService;

    @Override
    public void close() throws Exception {
        LOG.info("NetconfConnection Closed");
    }

    @Override
    public Connection createConnection(final String neIdentity) {
        return new MountPointConnection(mountPointService, neIdentity);
    }
}
