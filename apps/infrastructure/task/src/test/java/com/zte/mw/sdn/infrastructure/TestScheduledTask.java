/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure;

import org.junit.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by odl on 17-9-11.
 */
public class TestScheduledTask {
    private static ThreadPoolExecutor pool = new ThreadPoolExecutor(
            2, 3, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private static ThreadPoolExecutor poolFunction = new ThreadPoolExecutor(
            3, 3, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    @Test
    public void test() throws InterruptedException {

        SpyObserver observer = new SpyObserver(5, 20, this);

        for (int i = 0; i < observer.getTaskCount(); i++) {
            SpyScheduledTask task = new SpyScheduledTask(pool, new PrinterSpy(i), i, observer);
            poolFunction.execute(task);
        }
        synchronized (this) {
            this.wait();
        }
    }
}
