/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure.task;

import java.util.ArrayList;

import com.zte.mw.sdn.Result;
import com.zte.mw.sdn.infrastructure.Observer;

/**
 * Created by odl on 17-9-11.
 */
public class TaskObserver implements Observer<Result> {
    TaskObserver(final SelfScheduledTask owner) {
        this.owner = owner;
    }

    private final SelfScheduledTask owner;
    private int taskCount = 0;
    private ArrayList<Result> results = new ArrayList<>();

    void setTaskCount(final int taskCount) {
        this.taskCount = taskCount;
    }

    public void update(final Result target) {
        results.add(target);
        if (results.size() == taskCount) {
            synchronized (owner) {
                owner.notifyAll();
            }
        }
    }

    ArrayList<Result> getResults() {
        return results;
    }
}
