/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath;

import java.util.function.Function;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;

/**
 * Created by 10204924 on 2017/7/3.
 */
class HsbPrintUtils {
    private HsbPrintUtils() {
    }

    private static final Function<HsbLspType, String> LSP_TYPE_STRING_FUNCTION = type -> {
        if (type == HsbLspType.MASTER) {
            return "Master";
        } else {
            return "Slave";
        }
    };

    static void printSummaryInfo(HsbPathInstance hsbPathInstance) {
        printTunnelBasicInfo(hsbPathInstance);
        printBwSharedGroupMember(hsbPathInstance);
        ComUtility.debugInfoLog("Bandwidth:" + hsbPathInstance.getTeArgumentBean().getBandWidth());
        if (hsbPathInstance.getBiDirect() != null) {
            ComUtility.debugInfoLog(hsbPathInstance.getBiDirect().toString());
        }
    }

    private static void printTunnelBasicInfo(HsbPathInstance tunnel) {
        ComUtility.debugInfoLog(getTunnelBasicInfoStr(tunnel));
    }

    private static void printBwSharedGroupMember(HsbPathInstance hsbPathInstance) {
        if (hsbPathInstance.getBwSharedGroups() != null
                && hsbPathInstance.getBwSharedGroups().getBwSharedGroupMember() != null) {
            hsbPathInstance.getBwSharedGroups().getBwSharedGroupMember()
                    .forEach(group -> ComUtility.debugInfoLog(group.toString()));
        }
    }

    private static String getTunnelBasicInfoStr(HsbPathInstance hsbPathInstance) {
        return "HeadNodeId:" + hsbPathInstance.getHeadNode() + "\n"
                + "TailNodeId:" + hsbPathInstance.getTailNode() + "\n"
                + "TopoId:" + hsbPathInstance.getTopoId() + "\n";
    }

    static void printDetailInfo(HsbPathInstance hsbPathInstance) {
        printTunnelBasicInfo(hsbPathInstance);
        printBwSharedGroupMember(hsbPathInstance);

        if (null != hsbPathInstance.getTeArgumentBean()) {
            ComUtility.debugInfoLog(hsbPathInstance.getTeArgumentBean().toString());
        }

        if (hsbPathInstance.getBiDirect() != null) {
            ComUtility.debugInfoLog(hsbPathInstance.getBiDirect().toString());
        }

        printHsbLspInfo(hsbPathInstance, HsbLspType.MASTER);
        printHsbLspInfo(hsbPathInstance, HsbLspType.SLAVE);
    }

    private static void printHsbLspInfo(HsbPathInstance hsbPathInstance, HsbLspType lspType) {
        ComUtility.debugInfoLog(getHsbLspInfoStr(hsbPathInstance, lspType));
    }

    private static String getHsbLspInfoStr(HsbPathInstance hsbPathInstance, HsbLspType lspType) {
        String str = LSP_TYPE_STRING_FUNCTION.apply(lspType) + "Path:\n";
        if (lspType == HsbLspType.MASTER) {
            str += ComUtility.pathToString(hsbPathInstance.getMasterPath());
            str += "MasterMetric:" + hsbPathInstance.getMasterMetric() + "\n";
        } else {
            str += ComUtility.pathToString(hsbPathInstance.getSlavePath());
            str += "SlaveMetric:" + hsbPathInstance.getSlaveMetric() + "\n";
        }
        str += LSP_TYPE_STRING_FUNCTION.apply(lspType) + "Srlg:" + hsbPathInstance.getSrlgAttr(HsbLspType.MASTER)
                + "\n";
        return str;
    }

    static String toString(HsbPathInstance hsbPathInstance) {
        String str = "";
        str += getTunnelBasicInfoStr(hsbPathInstance);
        if (hsbPathInstance.getTeArgumentBean() != null) {
            str += hsbPathInstance.getTeArgumentBean().toString();
        }

        if (hsbPathInstance.getBiDirect() != null) {
            str += hsbPathInstance.getBiDirect().toString();
        }
        str += "IsSimulate:" + hsbPathInstance.isSimulate() + "\n";
        str += getHsbLspInfoStr(hsbPathInstance, HsbLspType.MASTER);
        str += getHsbLspInfoStr(hsbPathInstance, HsbLspType.SLAVE);
        ComUtility.debugInfoLog("masterMetric:" + hsbPathInstance.getMasterMetric());
        ComUtility.debugInfoLog("masterSrlg:" + hsbPathInstance.getSrlgAttr(HsbLspType.MASTER));
        ComUtility.debugInfoLog("slaveMetric:" + hsbPathInstance.getSlaveMetric());
        ComUtility.debugInfoLog("masterSrlg:" + hsbPathInstance.getSrlgAttr(HsbLspType.SLAVE));
        return str;
    }
}
