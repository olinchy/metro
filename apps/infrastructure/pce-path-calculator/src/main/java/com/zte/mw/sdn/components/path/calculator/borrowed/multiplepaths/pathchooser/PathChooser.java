/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser;

import java.util.List;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.MultiplePathResult;

/**
 * Created by 10204924 on 2017/2/8.
 */
public interface PathChooser<T> {
    /**
     * choose the pathResult if it is satisfied our condition and add it into chosenList.
     *
     * @param result the pathResult
     * @return true if the chosenList isFull, or false otherwise
     */
    boolean choose(T result);

    List<T> getChosenList(MultiplePathResult<T> originalResult);
}
