/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.ngip.ipsdn.impl;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.l2vpn.svc.rev170622.l2vpn.svc.vpn.services.VpnSvc;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Created by xudong on 17-9-13.
 */
public class IetfL2vpnSvcDataChangeListener implements DataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(IetfL2vpnSvcDataChangeListener.class);
    private static IetfL2vpnSvcDataChangeListener instance = new IetfL2vpnSvcDataChangeListener();

    private IetfL2vpnSvcDataChangeListener() {
    }

    public static IetfL2vpnSvcDataChangeListener getInstance() {
        return instance;
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        if (null == change) {
            return;
        }

        onCreatedData(change.getCreatedData());
        onUpdatedData(change.getUpdatedData());
        onRemovedData(change.getRemovedPaths(), change.getOriginalData());
    }

    private void onCreatedData(Map<InstanceIdentifier<?>, DataObject> createdData) {
        if (null == createdData) {
            return;
        }

        for (Map.Entry<?, ?> entry : createdData.entrySet()) {
            if (entry.getValue() instanceof VpnSvc) {
                //addDevice((Device) entry.getValue());
            }
        }
    }

    private void onUpdatedData(Map<InstanceIdentifier<?>, DataObject> updatedData) {
        if (null == updatedData) {
            return;
        }

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : updatedData.entrySet()) {
            if (entry.getValue() instanceof VpnSvc) {
                //updateDevice((Device) entry.getValue());
            }
        }
    }

    private void onRemovedData(Set<InstanceIdentifier<?>> removedPaths,
                               Map<InstanceIdentifier<?>, DataObject> originalData) {
        if ((null == removedPaths) || (removedPaths.isEmpty()) || (null == originalData)) {
            return;
        }
        for (InstanceIdentifier<?> key : removedPaths) {
            if (originalData.get(key) instanceof VpnSvc) {
                //removeDevice((Device) originalData.get(key));
            }
        }
    }
}
