/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn;

import com.zte.mw.sdn.exceptions.SdnException;

/**
 * Created by odl on 17-9-11.
 */
public class Result {
    public Result(final SdnException ex) {
        this.exception = ex;
    }

    public Result() {
    }

    public Result(final Exception exception) {
        if (exception instanceof SdnException) {
            this.exception = (SdnException) exception;
        } else {
            this.exception = new SdnException(exception);
        }
    }

    private SdnException exception;

    public SdnException getException() {
        return exception;
    }

    public boolean isSuccess() {
        return exception != null;
    }
}
