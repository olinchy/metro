/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.pathcore;

import java.util.Objects;

public class ConstraintsVerifyResult implements Comparable<ConstraintsVerifyResult> {
    /**
     * Constructor.
     *
     * @param isPathValid       is the link satisfy the constraints of path
     * @param isBandwidthEnough is the link satisfy the constraints of bandwidth
     * @param isDelayEligible   is the link satisfy the constraints of delay
     */
    public ConstraintsVerifyResult(boolean isPathValid, boolean isBandwidthEnough, boolean isDelayEligible) {
        this.isPathValid = isPathValid;
        this.isBandwidthEnough = isBandwidthEnough;
        this.isDelayEligible = isDelayEligible;
    }

    private boolean isPathValid;
    private boolean isBandwidthEnough;
    private boolean isDelayEligible;

    /**
     * Compare if this result is better than that result by some strategy.
     *
     * @param that that {@link ConstraintsVerifyResult}
     * @return true if this result is better than that result or false otherwise
     */
    public boolean isBetterThan(ConstraintsVerifyResult that) {
        if (that == null) {
            return true;
        }
        return this.getScore() > that.getScore();
    }

    /**
     * get the score of a link.
     *
     * @return score
     */
    public int getScore() {
        int score = 0;
        score += this.isPathValid ? 5 : 0;
        score += this.isBandwidthEnough ? 2 : 0;
        score += this.isDelayEligible ? 1 : 0;
        return score;
    }

    public boolean isBandwidthEnough() {
        return isBandwidthEnough;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(ConstraintsVerifyResult that) {
        if (this.getScore() == that.getScore()) {
            return this.hashCode() - that.hashCode();
        }
        return this.getScore() - that.getScore();
    }

    public boolean isDelayEligible() {
        return isDelayEligible;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isPathValid, isBandwidthEnough, isDelayEligible);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ConstraintsVerifyResult that = (ConstraintsVerifyResult) obj;
        return this.compareTo(that) == 0;
    }
}
