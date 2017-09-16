/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.qinq.service.notification.listener.impl;

import com.zte.mw.sdn.Result;
import com.zte.mw.sdn.infrastructure.task.SelfScheduledTask;
import com.zte.sdn.mw.e2e.runtime.MicrowaveRuntime;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class DeleteTask extends SelfScheduledTask {
    public DeleteTask(
            final Map<InstanceIdentifier<?>, DataObject> originalData, final Set<InstanceIdentifier<?>> removedPaths,
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
    protected void postException(final Exception e) {

    }
}
