/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth;

public class BandwidthAllocException extends Exception {
    public BandwidthAllocException() {
        super("Alloc bandwidth failed!");
    }

    private static final long serialVersionUID = 1L;
}
