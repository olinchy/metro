/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.command;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;

/**
 * Created by 10088483 on 12/3/15.
 */
@Command(scope = "pce-debug", name = "help", description = "Show framework debug help information.")
public class FrameworkDebugHelpProvider extends OsgiCommandSupport {
    @Override
    protected Object doExecute() throws Exception {
        ComUtility.debugInfoLog("pce-debug:print dci-pce tunnel [{node} {id}]              Show tunnel info.");
        ComUtility.debugInfoLog("pce-debug:print dci-pce tg [{node} {id}]              Show tunnel group info.");
        ComUtility.debugInfoLog("pce-debug:print dci-pce hsb [{node} {id}]         Show tunnel hotstanby info.");
        ComUtility.debugInfoLog("pce-debug:print dci-pce link-bandwidth {node} {interface} Show link bandwidth info.");
        ComUtility.debugInfoLog("pce-debug:print dci-pce topo                              Show topo info.");
        ComUtility.debugInfoLog("pce-debug:print dci-pce num                         Show tunnel num&topo num.");
        ComUtility.debugInfoLog("pce-debug:print dci-pce custom-properties           Show custom properties.");
        ComUtility.debugInfoLog("pce-debug:print dci-pce bsg                      Show bandwidth shared group.");
        return "";
    }
}
