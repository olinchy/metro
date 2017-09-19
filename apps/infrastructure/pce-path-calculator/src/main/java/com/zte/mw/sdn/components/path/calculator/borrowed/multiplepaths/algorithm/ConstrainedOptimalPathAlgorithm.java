/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.algorithm;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input.ConstrainedOptimalPathInput;

import com.zte.ngip.ipsdn.pce.path.core.result.Path;

/**
 * Created by 10204924 on 2017/6/2.
 */
public interface ConstrainedOptimalPathAlgorithm<V, E> {
    Path<E> calcConstrainedOptimalPath(ConstrainedOptimalPathInput<V, E> constrainedInput);
}
