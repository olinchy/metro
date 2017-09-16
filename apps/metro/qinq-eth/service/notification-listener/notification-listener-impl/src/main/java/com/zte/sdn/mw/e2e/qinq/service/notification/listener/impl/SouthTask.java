/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.qinq.service.notification.listener.impl;

import com.zte.mw.sdn.Model;
import com.zte.mw.sdn.Result;
import com.zte.mw.sdn.connection.Connection;
import com.zte.mw.sdn.connection.Driver;
import com.zte.mw.sdn.infrastructure.task.MonitoredTask;
import com.zte.mw.sdn.infrastructure.task.TaskObserver;
import com.zte.sdn.mw.e2e.runtime.MicrowaveRuntime;

public class SouthTask extends MonitoredTask {
    public SouthTask(final TaskObserver observer, Model model, MicrowaveRuntime runtime) {
        super(observer);
        this.model = model;
        this.runtime = runtime;
    }

    private final Model model;
    private final MicrowaveRuntime runtime;

    @Override
    protected void execute() {
        Connection connection = runtime.createSouthConnection(model.getNeIdentity());
        for (Driver driver : runtime.getDrivers()) {
            driver.config(model, connection);
        }
    }

    @Override
    protected void postException(final Exception e) {
        result = new Result(e);
    }
}
