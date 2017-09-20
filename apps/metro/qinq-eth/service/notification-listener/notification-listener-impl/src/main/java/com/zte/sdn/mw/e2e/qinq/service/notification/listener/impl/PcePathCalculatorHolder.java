/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.qinq.service.notification.listener.impl;

import com.zte.ngip.ipsdn.pce.path.calculator.api.PcePathCalculator;

public class PcePathCalculatorHolder {
    private PcePathCalculatorHolder() {
    }

    private static PcePathCalculatorHolder self = new PcePathCalculatorHolder();
    private PcePathCalculator pathCalculator;

    public static PcePathCalculatorHolder instance() {
        return self;
    }

    public void setPathCalculator(final PcePathCalculator pathCalculator) {
        this.pathCalculator = pathCalculator;
    }

    public PcePathCalculator getCalculator() {
        return pathCalculator;
    }
}
