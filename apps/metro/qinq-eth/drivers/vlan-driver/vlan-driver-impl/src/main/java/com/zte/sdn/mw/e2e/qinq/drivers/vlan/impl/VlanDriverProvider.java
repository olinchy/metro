/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.qinq.drivers.vlan.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VlanDriverProvider {
    public VlanDriverProvider(
            final DataBroker dataBroker, final RpcProviderRegistry rpcRegistry) {
        //, final MountPointService mountService) {
        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcRegistry;
        //        this.mountService = mountService;
    }

    private static final Logger LOG = LoggerFactory.getLogger(VlanDriverProvider.class);
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistry;
    //    private final MountPointService mountService;
    //    private BindingAwareBroker.RpcRegistration<RouteService> service;

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("VlanDriverProvider Session Initiated");
        throw new UnsupportedOperationException();
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("VlanDriverProvider Closed");
    }
}
