/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.runtime;

import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.connection.Connection;
import com.zte.mw.sdn.connection.Driver;

public class MicrowaveRuntimeImpl implements MicrowaveRuntime, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(MicrowaveRuntimeImpl.class);

    @Override
    public ThreadPoolExecutor getDispatchPool() {

        LOG.info("getDispatchPool had been called");
        return null;
    }

    @Override
    public ThreadPoolExecutor getConfigurationPool() {
        return null;
    }

    @Override
    public Connection createSouthConnection(final String neIdentity) {
        return null;
    }

    @Override
    public Driver[] getDrivers() {
        return new Driver[0];
    }

    @Override
    public void close() throws Exception {

    }
}
