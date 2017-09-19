/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.preepted.tunnels.PreemptedTunnel;

/**
 * Created by 10204924 on 2017/5/15.
 */
public class CalcResult {
    private CalcFailType calcFailType;
    private List<PreemptedTunnel> tunnels = new ArrayList<>();

    CalcFailType getCalcFailType() {
        return calcFailType;
    }

    void setCalcFailType(CalcFailType calcFailType) {
        this.calcFailType = calcFailType;
    }

    public List<PreemptedTunnel> getTunnels() {
        return tunnels;
    }

    public void setTunnels(List<PreemptedTunnel> tunnels) {
        this.tunnels = tunnels;
    }
}
