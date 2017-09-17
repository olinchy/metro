/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure;

/**
 * Created by odl on 17-9-11.
 */
public class SpyObserver implements Observer<Integer> {
    public SpyObserver(final int taskCount, final int subTaskCount, TestScheduledTask test) {
        this.taskCount = taskCount;
        this.subTaskCount = subTaskCount;
        this.test = test;
    }

    private final int taskCount;
    private final int subTaskCount;
    private final TestScheduledTask test;
    private int finishedCount = 0;

    public int getTaskCount() {
        return taskCount;
    }

    public int getSubTaskCount() {
        return subTaskCount;
    }

    @Override
    public void update(final Integer target) {
        synchronized (this) {
            finishedCount += target;
            if (finishedCount == taskCount * subTaskCount) {
                synchronized (test) {
                    test.notifyAll();
                }
            }
        }
    }
}
