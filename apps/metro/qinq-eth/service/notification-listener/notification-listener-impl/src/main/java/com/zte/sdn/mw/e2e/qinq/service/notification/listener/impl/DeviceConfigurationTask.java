/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.qinq.service.notification.listener.impl;

import com.zte.sdn.mw.e2e.runtime.MicrowaveRuntime;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class DeviceConfigurationTask implements Runnable {
    public DeviceConfigurationTask(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> asyncDataChangeEvent,
            final MicrowaveRuntime runtime) {

        this.changeEvent = asyncDataChangeEvent;
        this.runtime = runtime;
    }

    private static final Logger LOG = LoggerFactory.getLogger(DeviceConfigurationTask.class);
    private final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent;
    private final MicrowaveRuntime runtime;

    @Override
    public void run() {
        LOG.info("start to run Device configuration task with " + changeEvent);
        CompletableFuture
                .runAsync(new CreateTask(changeEvent.getCreatedData(), runtime), runtime.getDispatchPool())
                .thenRunAsync(
                        new UpdateTask(changeEvent.getOriginalData(), changeEvent.getUpdatedData(), runtime),
                        runtime.getDispatchPool())
                .thenRunAsync(
                        new DeleteTask(changeEvent.getOriginalData(), changeEvent.getRemovedPaths(), runtime),
                        runtime.getDispatchPool())
                .exceptionally(this::postException);
    }

    private Void postException(final Throwable e) {
        LOG.warn("execute Device configuration task caught exception", e);
        return null;
    }
}
