/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure;

import com.zte.mw.sdn.infrastructure.task.Result;
import com.zte.mw.sdn.infrastructure.task.SelfScheduledTask;

import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by odl on 17-9-11.
 */
public class SpyScheduledTask extends SelfScheduledTask {
    public SpyScheduledTask(final ThreadPoolExecutor pool, Spy spy) {
        super(pool);
        this.spy = spy;
    }

    private final Spy spy;

    @Override
    protected void pre() {

    }

    @Override
    protected void execute() {
        this.subTasks = new StubObservedTask[20];
        for (int i = 0; i < subTasks.length; i++) {
            subTasks[i] = new StubObservedTask(observer);
        }
        spy.addTimeMark(System.currentTimeMillis());
    }

    @Override
    protected void postException(final Exception e) {

    }

    @Override
    protected void postWithResults(final ArrayList<Result> results) {
        spy.watch(results);
    }
}
