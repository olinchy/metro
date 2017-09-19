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
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;

/**
 * Created by 10204924 on 2017/2/14.
 */
public class MaxBandwidthPathChooser extends AbstractPathChooser {
    public MaxBandwidthPathChooser(int chooseNum) {
        super(chooseNum);
    }

    @Override
    protected List<PathResult> getDefaultPathResults(MultiplePathResult<PathResult> originalResult) {
        return chosenList;
    }

    @Override
    public boolean check(PathResult result) {
        return true;
    }
}
