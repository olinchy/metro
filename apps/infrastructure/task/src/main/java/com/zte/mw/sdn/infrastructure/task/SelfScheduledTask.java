/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import com.zte.mw.sdn.Result;

/**
 * Created by odl on 17-9-11.
 */
public abstract class SelfScheduledTask extends Task {
    public SelfScheduledTask(ThreadPoolExecutor pool) {
        this.pool = pool;
    }

    protected final ThreadPoolExecutor pool;
    protected List<MonitoredTask> subTasks = new ArrayList<>();
    protected TaskObserver observer = new TaskObserver(this);

    @Override
    protected final void post() {
        this.observer.setTaskCount(subTasks.size());
        for (MonitoredTask task : subTasks) {
            pool.execute(task);
        }
        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException e) {
            // TODO: 17-9-11 need handle
        }
        postWithResults(observer.getResults());
    }

    protected abstract void postWithResults(final ArrayList<Result> results);
}
