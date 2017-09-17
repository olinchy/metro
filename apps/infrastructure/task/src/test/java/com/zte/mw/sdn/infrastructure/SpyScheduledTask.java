/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure;

import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;

import com.zte.mw.sdn.Result;
import com.zte.mw.sdn.infrastructure.task.SelfScheduledTask;

/**
 * Created by odl on 17-9-11.
 */
public class SpyScheduledTask extends SelfScheduledTask {
    public SpyScheduledTask(
            final ThreadPoolExecutor pool, Spy spy, int index, final SpyObserver observer) {
        super(pool);
        this.spy = spy;
        this.index = index;
        this.spyObserver = observer;
    }

    private final Spy spy;
    private final int index;
    private final SpyObserver spyObserver;

    @Override
    protected void execute() {
        for (int i = 0; i < subTasks.size(); i++) {
            subTasks.add(new StubMonitoredTask(observer, i, index));
        }
        spy.addTimeMark(System.currentTimeMillis());
    }

    @Override
    protected void postException(final Exception exception) {

    }

    @Override
    protected void postWithResults(final ArrayList<Result> results) {
        spy.watch(results);
        spyObserver.update(results.size());
    }
}
