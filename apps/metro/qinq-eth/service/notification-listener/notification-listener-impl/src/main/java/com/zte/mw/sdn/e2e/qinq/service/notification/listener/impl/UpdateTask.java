/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.e2e.qinq.service.notification.listener.impl;

import java.util.ArrayList;
import java.util.Map;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.zte.mw.sdn.Result;
import com.zte.mw.sdn.e2e.runtime.MicrowaveRuntime;
import com.zte.mw.sdn.infrastructure.task.SelfScheduledTask;

public class UpdateTask extends SelfScheduledTask {
    public UpdateTask(
            final Map<InstanceIdentifier<?>, DataObject> originalData,
            final Map<InstanceIdentifier<?>, DataObject> updatedData,
            MicrowaveRuntime runtime) {
        super(runtime.getConfigurationPool());
    }

    @Override
    protected void postWithResults(final ArrayList<Result> results) {

    }

    @Override
    protected void execute() {

    }

    @Override
    protected void postException(final Exception exception) {

    }
}
