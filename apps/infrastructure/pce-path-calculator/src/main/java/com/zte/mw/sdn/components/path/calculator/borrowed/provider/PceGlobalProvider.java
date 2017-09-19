/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.PceGlobalParameter;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pathdata.rev151125.PceGlobalParameterBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import com.zte.mw.sdn.components.path.calculator.borrowed.util.DataBrokerDelegate;

import com.zte.ngip.ipsdn.pce.path.api.util.Logs;

public class PceGlobalProvider {
    public PceGlobalProvider() {
        globalScaleBandWidthFlag = true;
    }

    private static final Logger LOG = LoggerFactory.getLogger(PceGlobalProvider.class);
    private boolean globalScaleBandWidthFlag;
    private boolean isCanZeroBandWidth = true;

    public void pceSetBandScaleGlobalFlag(boolean globalScaleBandWidthFlag) {
        if (this.globalScaleBandWidthFlag == globalScaleBandWidthFlag) {
            return;
        }
        this.globalScaleBandWidthFlag = globalScaleBandWidthFlag;
        LOG.info("destroy pceSetBandScaleGlobalFlag {}", globalScaleBandWidthFlag);
        DataBrokerDelegate.getInstance()
                .put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(PceGlobalParameter.class),
                     buildGlobalParameterDbData());
    }

    private PceGlobalParameter buildGlobalParameterDbData() {
        return new PceGlobalParameterBuilder().setCanScaleVteLink(globalScaleBandWidthFlag)
                .setCanZeroBandWidth(isCanZeroBandWidth).build();
    }

    public void registerDataTreeChangeListener() {
        DbProvider.getInstance().registerDataChangeListener(InstanceIdentifier.builder(
                PceGlobalParameter.class).build(), this::onBandwidthScaleChange);
    }

    public void onBandwidthScaleChange(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> arg0) {
        LOG.info("onBandwidthScaleChange {}", arg0);
        updateGlobalScaleBandWidthFlag(arg0.getUpdatedData());
        updateGlobalScaleBandWidthFlag(arg0.getCreatedData());
    }

    private void updateGlobalScaleBandWidthFlag(Map<InstanceIdentifier<?>, DataObject> data) {
        LOG.info("updateGlobalScaleBandWidthFlag data {}", data);
        for (Map.Entry<?, ?> entry : data.entrySet()) {
            if (entry.getValue() instanceof PceGlobalParameter) {
                this.globalScaleBandWidthFlag = ((PceGlobalParameter) entry.getValue()).isCanScaleVteLink();
            }
        }
    }

    public void pceGlobalDbRecovery() {
        this.globalScaleBandWidthFlag = pceGetBandScaleGlobalFlag();
        LOG.info("pceGlobalDbRecovery {}", globalScaleBandWidthFlag);
    }

    public boolean pceGetBandScaleGlobalFlag() {
        Optional<PceGlobalParameter> pceGlobalParameter;
        boolean flag;
        try {
            pceGlobalParameter = DataBrokerDelegate.getInstance()
                    .read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(PceGlobalParameter.class))
                    .get();
            LOG.info("pceGetBandScaleGlobalFlag pceGlobalParameter {}", pceGlobalParameter);
            if (pceGlobalParameter.isPresent()) {
                flag = pceGlobalParameter.get().isCanScaleVteLink() == null
                        ? true :
                        pceGlobalParameter.get().isCanScaleVteLink();
            } else {
                flag = true;
            }
        } catch (InterruptedException | ExecutionException e) {
            Logs.debug(LOG, "pceGetBandScaleGlobalFlag read failed " + e);
            flag = true;
        }
        return flag;
    }

    public boolean getGlobalBandScaleFlag() {
        return globalScaleBandWidthFlag;
    }

    public boolean isCanZeroBandWidth() {
        return isCanZeroBandWidth;
    }

    public void setCanZeroBandWidth(boolean canZeroBandWidth) {
        if (this.isCanZeroBandWidth == canZeroBandWidth) {
            return;
        }
        isCanZeroBandWidth = canZeroBandWidth;
        DataBrokerDelegate.getInstance()
                .put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(PceGlobalParameter.class),
                     buildGlobalParameterDbData());
        LOG.info("setCanZeroBandWidth {}", globalScaleBandWidthFlag);
    }
}
