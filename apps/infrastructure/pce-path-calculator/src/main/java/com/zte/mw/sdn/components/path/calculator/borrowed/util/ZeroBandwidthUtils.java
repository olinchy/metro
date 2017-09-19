/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.util;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.UpdateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.UpdateServiceInputBuilder;

import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;

/**
 * Created by 10204924 on 2017/8/9.
 */
public class ZeroBandwidthUtils {
    private ZeroBandwidthUtils() {
    }

    /**
     * generateZeroBandwidthTeArg.
     *
     * @param oldTeArg oldTeArg
     * @return zeroBandwidth TeArgumentBean
     */
    public static TeArgumentBean generateZeroBandwidthTeArg(TeArgumentBean oldTeArg) {
        TeArgumentBean newTeArg;
        newTeArg = new TeArgumentBean(oldTeArg);
        newTeArg.setBandWidth(0);
        newTeArg.setForceCalcPathWithBandwidth(true);
        return newTeArg;
    }

    /**
     * generateZeroBandwidthUpdateServiceInput.
     *
     * @param oldInput oldInput
     * @return ZeroBandwidth UpdateServiceInput
     */
    public static UpdateServiceInput generateZeroBandwidthUpdateServiceInput(UpdateServiceInput oldInput) {
        UpdateServiceInputBuilder builder = new UpdateServiceInputBuilder(oldInput);
        builder.setBandwidth(0L);
        builder.setBwSharedGroupContainer(null);
        return builder.build();
    }

    public static boolean isZeroBandwidthPath(Boolean isZeroBandwidthPath) {
        return isZeroBandwidthPath != null && isZeroBandwidthPath;
    }
}
