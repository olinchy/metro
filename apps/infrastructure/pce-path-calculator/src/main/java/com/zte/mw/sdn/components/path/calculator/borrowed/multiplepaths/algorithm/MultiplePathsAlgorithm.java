/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.algorithm;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser.PathChooser;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.MultiplePathResult;

/**
 * Created by 10204924 on 2017/2/3.
 */
@FunctionalInterface
public interface MultiplePathsAlgorithm<T> {
    MultiplePathResult<T> calcMultiplePaths(final int maxK, PathChooser<T> chooser);
}
