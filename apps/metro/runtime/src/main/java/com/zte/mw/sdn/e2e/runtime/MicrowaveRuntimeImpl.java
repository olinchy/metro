/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.e2e.runtime;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.DriverRegister;
import com.zte.mw.sdn.components.connections.ConnectionProvider;
import com.zte.mw.sdn.connection.Connection;
import com.zte.mw.sdn.connection.Driver;

public class MicrowaveRuntimeImpl implements MicrowaveRuntime, AutoCloseable {
    public MicrowaveRuntimeImpl(
            final DriverRegister driverRegister, final ConnectionProvider provider) {
        this.driverRegister = driverRegister;
        this.provider = provider;

        LOG.info("connection provider is " + this.provider.toString());
    }

    private static final Logger LOG = LoggerFactory.getLogger(MicrowaveRuntimeImpl.class);
    private static ThreadPoolExecutor dispatchPool = new ThreadPoolExecutor(
            3, 3, 3, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private static ThreadPoolExecutor configurationPool = new ThreadPoolExecutor(
            3, 3, 3, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    static {

        LOG.info("initialized");
        LOG.info("dispatch pool is " + dispatchPool.toString());
        LOG.info("configuration pool is " + configurationPool.toString());
    }

    private final DriverRegister driverRegister;
    private final ConnectionProvider provider;

    @Override
    public ThreadPoolExecutor getDispatchPool() {
        return dispatchPool;
    }

    @Override
    public ThreadPoolExecutor getConfigurationPool() {
        return configurationPool;
    }

    @Override
    public Connection createSouthConnection(final String neIdentity) {
        return provider.createConnection(neIdentity);
    }

    @Override
    public Driver[] getDrivers() {
        return driverRegister.getRegistered();
    }

    @Override
    public void close() throws Exception {

    }
}
