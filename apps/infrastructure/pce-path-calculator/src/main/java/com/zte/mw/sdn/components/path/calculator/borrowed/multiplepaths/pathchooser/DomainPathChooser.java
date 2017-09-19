/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.MultiplePathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;

import com.zte.ngip.ipsdn.pce.path.api.util.Logs;

/**
 * Created by 10204924 on 2017/8/9.
 */
public class DomainPathChooser extends AbstractPathChooser {
    public DomainPathChooser(int chooseNum) {
        super(chooseNum);
    }

    private static final Logger LOG = LoggerFactory.getLogger(DomainPathChooser.class);

    @Override
    protected List<PathResult> getDefaultPathResults(MultiplePathResult<PathResult> originalResult) {
        PathResult defaultResult = new PathResult();
        defaultResult.getPceResult().setCalcFailType(CalcFailType.NoPath);
        defaultResult.getPceResult().setCalcFail(true);
        Logs.debug(getLogger(), "getDefaultPathResults calcFailType {}", CalcFailType.NoPath);
        return Lists.newArrayList(defaultResult);
    }

    @Override
    protected boolean check(PathResult result) {
        return true;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
