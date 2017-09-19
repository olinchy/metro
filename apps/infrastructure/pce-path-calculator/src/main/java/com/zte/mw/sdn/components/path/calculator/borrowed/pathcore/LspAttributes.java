/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.pathcore;

import com.zte.ngip.ipsdn.pce.path.api.srlg.SrlgAttribute;

/**
 * Created by 10204924 on 2017/7/5.
 */
public class LspAttributes {
    private long lspMetric;
    private long lspDelay;
    private SrlgAttribute srlgAttr = new SrlgAttribute();

    public long getLspMetric() {
        return lspMetric;
    }

    public void setLspMetric(long lspMetric) {
        this.lspMetric = lspMetric;
    }

    public long getLspDelay() {
        return lspDelay;
    }

    public void setLspDelay(long lspDelay) {
        this.lspDelay = lspDelay;
    }

    public SrlgAttribute getSrlgAttr() {
        return srlgAttr;
    }

    public void setSrlgAttr(SrlgAttribute srlgAttr) {
        this.srlgAttr = srlgAttr != null ? srlgAttr : this.srlgAttr;
    }
}
