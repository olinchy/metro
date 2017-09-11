/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure.task;

import com.zte.mw.sdn.infrastructure.Observer;

import java.util.ArrayList;

/**
 * Created by odl on 17-9-11.
 */
public class TaskObserver implements Observer<Result> {
    public TaskObserver(final SelfScheduledTask owner) {
        this.owner = owner;
    }

    private final SelfScheduledTask owner;
    private int taskCount = 0;
    private ArrayList<Result> results = new ArrayList<>();

    public void setTaskCount(final int taskCount) {
        this.taskCount = taskCount;
    }

    public void update(final Result result) {
        results.add(result);
        if (results.size() == taskCount) {
            synchronized (owner) {
                owner.notifyAll();
            }
        }
    }

    public ArrayList<Result> getResults() {
        return results;
    }
}
