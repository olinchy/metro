/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.qinq.service.notification.listener.impl;

import com.zte.mw.sdn.Model;
import com.zte.mw.sdn.Result;
import com.zte.mw.sdn.infrastructure.task.MonitoredTask;
import com.zte.mw.sdn.infrastructure.task.SelfScheduledTask;
import com.zte.sdn.mw.e2e.runtime.MicrowaveRuntime;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.l2vpn.svc.rev170622.l2vpn.svc.vpn.services.VpnSvc;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateTask extends SelfScheduledTask {
    public CreateTask(final Map<InstanceIdentifier<?>, DataObject> createdData, MicrowaveRuntime runtime) {
        super(runtime.getConfigurationPool());
        this.runtime = runtime;
        this.createData = createdData;
    }

    private static final Logger LOG = LoggerFactory.getLogger(CreateTask.class);
    private final MicrowaveRuntime runtime;
    private final Map<InstanceIdentifier<?>, DataObject> createData;

    @Override
    protected void execute() {
        List<VpnSvc> toBeCreate =
                createData.entrySet().stream().map(Map.Entry::getValue).filter(value -> value instanceof VpnSvc)
                        .peek(vpn -> LOG.info("create vpn service of microwave", vpn)).map(
                        dataLink -> (VpnSvc) dataLink)
                        .collect(Collectors.toList());
        for (VpnSvc vpnSvc : toBeCreate) {
            subTasks.addAll(toDeviceTasks(vpnSvc));
        }
    }

    private Collection<? extends MonitoredTask> toDeviceTasks(final VpnSvc vpnSvc) {

        ArrayList<SouthTask> southTasks = new ArrayList<>();
        List<Model> modelOnNodes = toNodeModel(vpnSvc);

        for (Model model : modelOnNodes) {
            southTasks.add(new SouthTask(observer, model, runtime));
        }

        return southTasks;
    }

    private List<Model> toNodeModel(final VpnSvc vpnSvc) {

        // TODO: 17-9-16 fuction --> vpnservice ==> node
        // create model of nodes
        // path calculation
        // persist
        return null;
    }

    @Override
    protected void postException(final Exception e) {
        LOG.warn("create " + createData + " caught exception", e);
    }

    @Override
    protected void postWithResults(final ArrayList<Result> results) {
        LOG.info(String.valueOf(results));
    }
}
