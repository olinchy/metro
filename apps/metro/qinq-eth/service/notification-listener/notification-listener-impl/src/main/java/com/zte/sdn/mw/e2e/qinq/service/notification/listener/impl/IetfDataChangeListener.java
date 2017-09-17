/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.qinq.service.notification.listener.impl;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.zte.sdn.mw.e2e.runtime.MicrowaveRuntime;

public class IetfDataChangeListener implements DataChangeListener {
    public IetfDataChangeListener(final MicrowaveRuntime runtime) {
        this.runtime = runtime;
    }

    private MicrowaveRuntime runtime = null;

    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> asyncDataChangeEvent) {
        runtime.getDispatchPool().execute(new DeviceConfigurationTask(asyncDataChangeEvent, runtime));
    }
}
