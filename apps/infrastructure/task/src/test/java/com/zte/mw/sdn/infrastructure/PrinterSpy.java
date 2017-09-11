/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure;

import com.zte.mw.sdn.infrastructure.task.Result;

import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by odl on 17-9-11.
 */
public class PrinterSpy implements Spy {
    public PrinterSpy(final int index) {
        this.index = index;
    }

    private int index = 0;
    private long timeMark;

    @Override
    public void watch(final ArrayList<Result> results) {
        final long current;
        System.out.println("all subTask of task " + index + " finished at " + (current = System.currentTimeMillis()));
        assertThat(results.size(), is(20));

        assertTrue(current - timeMark > 10000);
    }

    @Override
    public void addTimeMark(final long timeMark) {
        System.out.println("start waiting of task " + index + " at " + System.currentTimeMillis());
        this.timeMark = timeMark;
    }
}
