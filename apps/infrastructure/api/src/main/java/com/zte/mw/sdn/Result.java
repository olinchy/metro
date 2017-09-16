/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn;

import com.zte.mw.sdn.exceptions.SDNException;

/**
 * Created by odl on 17-9-11.
 */
public class Result {
    public Result(final SDNException e) {
        this.exception = e;
    }

    public Result() {
    }

    public Result(final Exception e) {
        if (e instanceof SDNException) {
            this.exception = (SDNException) e;
        } else {
            this.exception = new SDNException(e);
        }
    }

    private SDNException exception;

    public SDNException getException() {
        return exception;
    }

    public boolean isSuccess() {
        return exception != null;
    }
}
