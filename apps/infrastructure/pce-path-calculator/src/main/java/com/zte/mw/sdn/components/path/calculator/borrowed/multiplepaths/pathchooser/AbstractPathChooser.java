/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.MultiplePathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;

/**
 * Created by 10204924 on 2017/2/22.
 */
public abstract class AbstractPathChooser implements PathChooser<PathResult> {
    public AbstractPathChooser(int chooseNum) {
        this.chooseNum = chooseNum;
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPathChooser.class);
    protected final int chooseNum;
    protected final List<PathResult> chosenList = new LinkedList<>();

    @Override
    public boolean choose(PathResult result) {
        if (!isFull()) {
            Optional.ofNullable(result).filter(this::check).ifPresent(this.chosenList::add);
        }
        return isFull();
    }

    @Override
    public List<PathResult> getChosenList(final MultiplePathResult<PathResult> originalResult) {
        if (this.chosenList.isEmpty()) {
            return getDefaultPathResults(originalResult);
        }
        return new ArrayList<>(chosenList);
    }

    private boolean isFull() {
        return chosenList.size() >= chooseNum;
    }

    /**
     * When chosenList is invalid, get default results from this method.
     *
     * @param originalResult originalList
     * @return default results
     */
    protected abstract List<PathResult> getDefaultPathResults(final MultiplePathResult<PathResult> originalResult);

    /**
     * Check if the pathResult can be added to chosenList.
     *
     * @param result pathResult
     * @return true or false
     */
    protected abstract boolean check(PathResult result);

    protected Logger getLogger() {
        return LOG;
    }
}
