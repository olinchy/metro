/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure.e2e;

import com.zte.mw.sdn.infrastructure.SDNException;
import com.zte.mw.sdn.infrastructure.task.ObservedTask;
import com.zte.mw.sdn.infrastructure.task.Result;
import com.zte.mw.sdn.infrastructure.task.TaskObserver;

/**
 * Created by odl on 17-9-11.
 */
public abstract class DriverTask<T extends Driver> extends ObservedTask {
    public DriverTask(final TaskObserver observer, T driver) {
        super(observer);
        this.driver = driver;
    }

    private final T driver;
    protected Model model;

    @Override
    protected final void execute() {
        result = driver.commit(model);
    }

    @Override
    protected void postException(final Exception e) {
        driver.rollback(model);

        if (e instanceof SDNException) {
            result = new Result((SDNException) e);
        } else {
            result = new Result(new SDNException(e));
        }
    }
}
