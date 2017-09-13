/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.ngip.ipsdn.impl;

import com.zte.mw.sdn.infrastructure.task.Result;
import com.zte.mw.sdn.infrastructure.task.SelfScheduledTask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.l2vpn.svc.rev170622.l2vpn.svc.vpn.services.VpnSvc;

import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by xudong on 17-9-13.
 */
public class CreateVpnSvcTask extends SelfScheduledTask {
    private final VpnSvc vpnSvc;

    public CreateVpnSvcTask(ThreadPoolExecutor pool, VpnSvc vpnSvc) {
        super(pool);
        this.vpnSvc = vpnSvc;
    }

    @Override
    protected void pre() {

    }

    @Override
    protected void execute() {

    }

    @Override
    protected void postException(Exception e) {

    }

    @Override
    protected void postWithResults(ArrayList<Result> results) {

    }
}
