/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result;

import java.util.Collections;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;

/**
 * Created by 10204924 on 2017/3/2.
 */
public final class MultiplePathResult<R> {
    public MultiplePathResult(List<R> pathResults) {
        this.pathResults = pathResults;
    }

    private List<R> pathResults = Collections.emptyList();
    private CalcFailType failType;

    public List<R> getPathResults() {
        return pathResults;
    }

    public CalcFailType getFailType() {
        return failType;
    }

    public void setFailType(CalcFailType failType) {
        this.failType = failType;
    }
}
