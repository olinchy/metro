/*
 * Copyright (c) 2017 ZTE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.qinq.service.notification.listener.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.l2vpn.svc.rev170622.L2vpnSvc;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.l2vpn.svc.rev170622.l2vpn.svc.VpnServices;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationListenerProvider implements AutoCloseable {
    public NotificationListenerProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        start();
    }

    private void start() {
        InstanceIdentifier<VpnServices> instanceIdentifier = InstanceIdentifier.builder(L2vpnSvc.class).child(
                VpnServices.class).build();
        dataBroker.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, instanceIdentifier, new IetfDataChangeListener(), AsyncDataBroker
                        .DataChangeScope.SUBTREE);
    }

    private static final Logger LOG = LoggerFactory.getLogger(NotificationListenerProvider.class);
    private final DataBroker dataBroker;

    @Override
    public void close() throws Exception {
        LOG.info("NotificationListenerProvider Closed");
    }
}
