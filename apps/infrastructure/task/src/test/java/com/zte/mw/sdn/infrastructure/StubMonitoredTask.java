/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure;

import com.zte.mw.sdn.infrastructure.task.MonitoredTask;
import com.zte.mw.sdn.infrastructure.task.Result;
import com.zte.mw.sdn.infrastructure.task.TaskObserver;

/**
 * Created by odl on 17-9-11.
 */
public class StubMonitoredTask extends MonitoredTask {
    public StubMonitoredTask(final TaskObserver observer, final int index, final int parentIndex) {
        super(observer);
        this.index = index;
        this.parentIndex = parentIndex;
    }

    private final int index;
    private final int parentIndex;

    @Override
    protected void pre() {

    }

    @Override
    protected void execute() {
        System.out.println(
                "start to execute subTask " + parentIndex + "_" + index + " at " + System.currentTimeMillis());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {

        }
        this.result = new Result();
    }

    @Override
    protected void postException(final Exception e) {

    }
}
