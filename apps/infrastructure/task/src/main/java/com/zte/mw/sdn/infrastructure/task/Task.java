/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure.task;

import com.zte.mw.sdn.exceptions.SdnException;

/**
 * Created by odl on 17-9-11.
 */
public abstract class Task implements Runnable {
    public void run() {
        try {
            execute();
        } catch (SdnException e) {
            postException(e);
        } finally {
            post();
        }
    }

    protected abstract void execute() throws SdnException;

    protected abstract void postException(final Exception exception);

    protected abstract void post();
}
