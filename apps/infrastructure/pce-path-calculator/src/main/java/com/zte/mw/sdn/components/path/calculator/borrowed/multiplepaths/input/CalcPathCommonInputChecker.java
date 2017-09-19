/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input;

import com.google.common.base.Preconditions;

import com.zte.ngip.ipsdn.pce.path.core.input.CalcPathCommonInput;

/**
 * Created by 10204924 on 2017/2/9.
 */
public class CalcPathCommonInputChecker {
    private CalcPathCommonInputChecker() {
    }

    public static void checkNotNull(CalcPathCommonInput input) {
        Preconditions.checkNotNull(input.getHeadNodeId(), "headNodeId is null!");
        Preconditions.checkNotNull(input.getTailNodeId(), "tailNodeId is null!");
    }
}
