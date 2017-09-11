/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure;

import com.zte.mw.sdn.infrastructure.task.Result;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by odl on 17-9-11.
 */
public class TestScheduledTask {
    private static ThreadPoolExecutor pool = new ThreadPoolExecutor(
            2, 3, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    @Test
    public void test() throws InterruptedException {

        SpyScheduledTask task = new SpyScheduledTask(pool, new Spy() {
            public long timeMark;

            @Override
            public void watch(final ArrayList<Result> results) {
                final long current;
                System.out.println("all subTask finished at " + (current = System.currentTimeMillis()));
                assertThat(results.size(), is(20));

                assertTrue(current - timeMark > 10000);
                System.exit(0);
            }

            @Override
            public void addTimeMark(final long timeMark) {
                System.out.println("start waiting at " + System.currentTimeMillis());
                this.timeMark = timeMark;
            }
        });

        new Thread(task).start();

        Thread.sleep(100000);
    }
}
