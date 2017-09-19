/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.command;

import java.util.LinkedList;

import org.apache.felix.gogo.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroupMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.level.LevelProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;

import com.zte.ngip.ipsdn.pce.path.api.PceTopoProvider;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;

@Command(scope = "pce-debug", name = "print", description = "print framework debug information")
public class FrameworkDebugCommandProvider extends OsgiCommandSupport {
    private static final String PARAM_ERROR = "param error.";
    private static final String FINISH = "print finish.";
    private static LinkedList<String> cmdParams = new LinkedList<>();
    private static PcePathProvider pcePathProvider = null;
    private static BandWidthMng bandWidthMng = null;
    private static PceTopoProvider topologyProvider = null;
    private static BwSharedGroupMng bwSharedGroupMng = null;
    private static LevelProvider levelProvider = null;
    @Argument(index = 0, name = "module", description = "The debug module name.", required = true, multiValued = false)
    String module = null;
    @Argument(index = 1, name = "type",
            description = "The info type of the data.", required = true, multiValued = false)
    String type = null;
    @Argument(index = 2, name = "param1", description = "The 1st parameter.", required = false, multiValued = false)
    String param1 = null;
    @Argument(index = 3, name = "param2", description = "The 2st parameter.", required = false, multiValued = false)
    String param2 = null;

    public static void init(
            PcePathProvider pce, BandWidthMng bw, PceTopoProvider topo,
            BwSharedGroupMng bwShared, LevelProvider level) {
        pcePathProvider = pce;
        bandWidthMng = bw;
        topologyProvider = topo;
        bwSharedGroupMng = bwShared;
        levelProvider = level;
    }

    private static boolean pcePathModuleNotInit() {
        boolean result = pcePathProvider == null || bandWidthMng == null || topologyProvider == null;
        return result || bwSharedGroupMng == null || levelProvider == null;
    }

    private static String tunnelDebugCmdExecute(LinkedList<String> cmdParams) {
        String node = cmdParams.get(0);
        String id = cmdParams.get(1);

        if ((null == node) && (null == id)) {
            pcePathProvider.printAllTunnelInfo();
        } else if ((null != node) && (null != id)) {
            pcePathProvider.printTunnelInfo(new NodeId(node), Integer.parseInt(id));
        } else {
            return PARAM_ERROR;
        }
        return FINISH;
    }

    private static String tunnelGroupDebugCmdExecute(LinkedList<String> cmdParams) {
        String node = cmdParams.get(0);
        String id = cmdParams.get(1);

        if ((null == node) && (null == id)) {
            pcePathProvider.printAllTunnelGroupInfo();
        } else if ((null != node) && (null != id)) {
            pcePathProvider.printTunnelGroupInfo(new NodeId(node), Integer.parseInt(id));
        } else {
            return PARAM_ERROR;
        }
        return FINISH;
    }

    private static String tunnelHsbDebugCmdExecute(LinkedList<String> cmdParams) {
        String node = cmdParams.get(0);
        String id = cmdParams.get(1);

        if ((null == node) && (null == id)) {
            pcePathProvider.printAllTunnelHsbInfo();
        } else if ((null != node) && (null != id)) {
            pcePathProvider.printTunnelHsbInfo(new NodeId(node), Integer.parseInt(id));
        } else {
            return PARAM_ERROR;
        }
        return FINISH;
    }

    private static String topoDebugCmdExecute() {
        topologyProvider.topoGraphPrint();
        return FINISH;
    }

    private static String numDebugCmdExecute() {
        pcePathProvider.tunnelNumPrint();
        topologyProvider.topoNumPrint();
        return FINISH;
    }

    private static String customPropertiesDebugCmdExecute() {
        ComUtility.debugInfoLog("isTunnelDispatchBandwidth:" + PcePathProvider.isTunnelDispatchBandwidth());
        ComUtility.debugInfoLog("getBandWidthString:" + bandWidthMng.getBandWidthString());
        return FINISH;
    }

    private static String bwSharedGroupCmdExecute(LinkedList<String> cmdParams) {
        String id = cmdParams.get(0);

        if (null != id) {
            ComUtility.debugInfoLog(bwSharedGroupMng.toString(id));
        } else {
            ComUtility.debugInfoLog(bwSharedGroupMng.toString());
        }
        return FINISH;
    }

    private static String levelCmdExecute(LinkedList<String> cmdParams) {
        String id = cmdParams.get(0);

        if (null != id) {
            ComUtility.debugInfoLog(levelProvider.toString(id));
        } else {
            ComUtility.debugInfoLog(levelProvider.toString());
        }
        return FINISH;
    }

    @Override
    protected Object doExecute() throws Exception {
        cmdParams.clear();
        cmdParams.add(param1);
        cmdParams.add(param2);

        if ("dci-pce".equals(module)) {
            if (pcePathModuleNotInit()) {
                return "pce path module has not init!";
            }
            return executeByType(type);
        }
        return PARAM_ERROR;
    }

    private String executeByType(String type) {
        String result;
        switch (type) {
            case "tunnel":
                result = tunnelDebugCmdExecute(cmdParams);
                break;
            case "tg":
                result = tunnelGroupDebugCmdExecute(cmdParams);
                break;
            case "hsb":
                result = tunnelHsbDebugCmdExecute(cmdParams);
                break;
            case "link-bandwidth":
                result = linkBandWidthDebugCmdExecute();
                break;
            case "topo":
                result = topoDebugCmdExecute();
                break;
            case "num":
                result = numDebugCmdExecute();
                break;
            case "custom-properties":
                result = customPropertiesDebugCmdExecute();
                break;
            case "bsg":
                result = bwSharedGroupCmdExecute(cmdParams);
                break;
            case "level":
                result = levelCmdExecute(cmdParams);
                break;
            default:
                result = PARAM_ERROR;
                break;
        }
        return result;
    }

    private String linkBandWidthDebugCmdExecute() {
        String node = param1;
        String interfaceName = param2;
        if ((null == node) && (null == interfaceName)) {
            ComUtility.debugInfoLog(bandWidthMng.getBandWidthString());
            return FINISH;
        } else if ((null != node) && (null != interfaceName)) {
            bandWidthMng.printPortBandWidth(new NodeId(node), new TpId(interfaceName));
            return FINISH;
        }
        return PARAM_ERROR;
    }
}



