/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.common.data.TeArgCommonData;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.te.argument.common.data.TeArgCommonDataBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TeArgument;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.TeArgumentLsp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.maintenancewindow.MaintenanceTeArgumentBeanLsp;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBeanLsp;

/**
 * Created by 10204924 on 2017/7/3.
 */
public class HsbNewTeArgBuilder {
    public HsbNewTeArgBuilder(TeArgumentBean hsbTeArg, TopologyId topoId) {
        this.hsbTeArg = hsbTeArg;
        this.topoId = topoId;
    }

    private static final Logger LOG = LoggerFactory.getLogger(HsbNewTeArgBuilder.class);
    private TeArgumentBean hsbTeArg;
    private TopologyId topoId;

    public TeArgumentBean buildNewTeArg(
            TeArgument newArgMent, TeArgumentLsp teArgumentLsp,
            TeArgumentLsp maintenanceLsp) {
        if (PceUtil.isMaintenance(maintenanceLsp)) {

            TeArgCommonData teArgComm = new TeArgCommonDataBuilder()
                    .setHoldPriority((short) hsbTeArg.getHoldPriority())
                    .setPreemptPriority((short) hsbTeArg.getPreemptPriority())
                    .setBandwidth(hsbTeArg.getBandWidth())
                    .setMaxDelay(hsbTeArg.getMaxDelay()).build();

            TeArgumentBeanLsp originalMasterLspBean = new TeArgumentBeanLsp(teArgumentLsp, topoId);
            MaintenanceTeArgumentBeanLsp maintenanceTeArgBeanLsp =
                    new MaintenanceTeArgumentBeanLsp(maintenanceLsp, topoId);
            maintenanceTeArgBeanLsp.mergeToOriginalMaster(originalMasterLspBean);

            TeArgumentBean teArgumentBean = new TeArgumentBean(teArgComm, originalMasterLspBean);
            teArgumentBean
                    .setComputeLspWithBandWidth(this.hsbTeArg == null || this.hsbTeArg.isComputeLspWithBandWidth());
            Logs.debug(LOG, "master teArg is merged with MaintenanceTeArgument: {}", teArgumentBean);
            return teArgumentBean;
        }
        return getNewTeArg(newArgMent, teArgumentLsp);
    }

    private TeArgumentBean getNewTeArg(TeArgument newArgMent, TeArgumentLsp teArgumentLsp) {
        //there isn't priority and priority can't change.
        TeArgCommonData teArgComm = new TeArgCommonDataBuilder().setHoldPriority((short) hsbTeArg.getHoldPriority())
                .setPreemptPriority((short) hsbTeArg.getPreemptPriority()).setBandwidth(newArgMent.getBandwidth())
                .setMaxDelay(newArgMent.getMaxDelay()).build();

        TeArgumentBeanLsp teArgLsp = new TeArgumentBeanLsp(teArgumentLsp, topoId);
        TeArgumentBean teArgumentBean = new TeArgumentBean(teArgComm, teArgLsp);
        teArgumentBean.setComputeLspWithBandWidth(this.hsbTeArg == null || this.hsbTeArg.isComputeLspWithBandWidth());

        return teArgumentBean;
    }

    public TeArgumentBeanLsp buildNewSlaveTeArg(TeArgumentLsp originalLsp, TeArgumentLsp maintenanceLsp) {
        TeArgumentBeanLsp originalSlaveTeArg = new TeArgumentBeanLsp(originalLsp, topoId);
        if (PceUtil.isMaintenance(maintenanceLsp)) {
            MaintenanceTeArgumentBeanLsp maintenanceTeArgBeanLsp =
                    new MaintenanceTeArgumentBeanLsp(maintenanceLsp, topoId);
            maintenanceTeArgBeanLsp.mergeToOriginalSlave(originalSlaveTeArg);
            LOG.debug("slave teArg is merged with MaintenanceTeArgument: {}", originalSlaveTeArg);
        }
        return originalSlaveTeArg;
    }
}
