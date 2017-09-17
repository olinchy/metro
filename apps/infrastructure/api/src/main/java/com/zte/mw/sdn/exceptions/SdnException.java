/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.exceptions;

public class SdnException extends Exception {
    public SdnException() {
        super();
    }

    public SdnException(final String message) {
        super(message);
    }

    public SdnException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SdnException(final Throwable cause) {
        super(cause);
    }

    protected SdnException(
            final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
