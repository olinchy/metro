/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.util;

import org.osgi.framework.BundleContext;

/**
 * Created by 00070916 on 2/1/16.
 */
public class BundleProperty {
    private BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public byte getBandUtilization() {
        String utilizationString = getDefinedBundleProperty("zte.controller.bandwidth_utilization");
        if (null != utilizationString) {
            return Byte.parseByte(utilizationString);
        }
        return 100;
    }

    public String getDefinedBundleProperty(String defined) {
        return bundleContext.getProperty(defined);
    }

    public boolean getTunnelDispatchBandwidthFlag() {
        String isTunnelDispatchBandwidthString = getDefinedBundleProperty("zte.controller.download_bandwidth");
        if (null != isTunnelDispatchBandwidthString) {
            return Boolean.parseBoolean(isTunnelDispatchBandwidthString);
        }
        return true;
    }
}
