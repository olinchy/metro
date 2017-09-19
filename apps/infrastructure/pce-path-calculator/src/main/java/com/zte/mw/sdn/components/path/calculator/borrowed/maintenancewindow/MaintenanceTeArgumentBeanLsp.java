/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.maintenancewindow;

import java.util.Optional;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TeArgumentLsp;

import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBeanLsp;

/**
 * Created by 10204924 on 2016/12/27.
 */
public class MaintenanceTeArgumentBeanLsp extends TeArgumentBeanLsp {
    public MaintenanceTeArgumentBeanLsp(TeArgumentLsp teArgment, TopologyId topoId) {
        super(teArgment, topoId);
    }

    /**
     * master teArg need merge excludingAddress and  replace nextAddress.
     *
     * @param original to be merged
     */
    public void mergeToOriginalMaster(TeArgumentBeanLsp original) {
        if (original == null) {
            return;
        }
        mergeExcludingAddressToOriginal(original);
        original.replaceNextAddress(this.nextAddress);
    }

    private void mergeExcludingAddressToOriginal(TeArgumentBeanLsp original) {
        Optional.ofNullable(this.excludedNodes).ifPresent(original.getExcludedNodes()::addAll);
        Optional.ofNullable(this.excludedPorts).ifPresent(original.getExcludedPorts()::addAll);
    }

    /**
     * slave teArg need merge excludingAddress.
     *
     * @param original to be merged
     */
    public void mergeToOriginalSlave(TeArgumentBeanLsp original) {
        if (original == null) {
            return;
        }
        mergeExcludingAddressToOriginal(original);
    }
}
