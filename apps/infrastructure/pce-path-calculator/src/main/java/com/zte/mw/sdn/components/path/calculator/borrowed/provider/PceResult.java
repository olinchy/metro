/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcFailType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.preepted.tunnels.PreemptedTunnel;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.preepted.tunnels.PreemptedTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.preepted.tunnels.preempted.tunnel.MasterPathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.preepted.tunnels.preempted.tunnel.SlavePathBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.preepted.tunnels.preempted.tunnel.TriggerPreemptTunnel;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.preepted.tunnels.preempted.tunnel.TriggerPreemptTunnelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroup;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroupMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.ITunnel;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.util.PortKey;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.api.vtelink.BandWidthScale;

/**
 * Created by 10088483 on 1/25/16.
 */
public class PceResult {
    public static final PceResult nullPceResult = new PceResult();
    private static final Logger LOG = LoggerFactory.getLogger(PceResult.class);
    private List<RetPreemptedTunnel> tunnels = new ArrayList<>();
    private boolean isReservedBwEnlarged = false;
    private boolean calcFail = false;
    private CalcFailType calcFailType = null;
    private CalcFailType failReason = CalcFailType.Other;
    private List<BandWidthScale> bandWidthScaleList;

    public static PceResult create() {
        PceResult pceResult = new PceResult();
        pceResult.setCalcFail(true);
        return pceResult;
    }

    public boolean isNeedRefreshUnestablishTunnels() {
        return isReservedBwEnlarged;
    }

    public void reset() {
        tunnels.clear();
        isReservedBwEnlarged = false;
        if (null != bandWidthScaleList) {
            bandWidthScaleList.clear();
        }
    }

    public void enableBandwidthFreeFlag() {
        isReservedBwEnlarged = true;
    }

    public void addPreemptedTunnel(TunnelUnifyKey key, TunnelUnifyKey preemptingTunnel) {
        RetPreemptedTunnel tunnel;
        if (key.isBwSharedGroup()) {
            BwSharedGroup bwSharedGroup =
                    BwSharedGroupMng.getInstance().getBwShareGroupByKey(key);
            List<TunnelUnifyKey> memberTunnels = bwSharedGroup.getMemberTunnelKey();
            for (TunnelUnifyKey tunnelKey : memberTunnels) {
                tunnel = new RetPreemptedTunnel(tunnelKey, preemptingTunnel);
                tunnels.add(tunnel);
            }
        } else {
            tunnel = new RetPreemptedTunnel(key, preemptingTunnel);
            tunnels.add(tunnel);
        }
    }

    public void dealBwSharedGroupTunnel(PortKey portKey, boolean isSimulate) {
        if ((tunnels == null) || (tunnels.isEmpty())) {
            return;
        }

        ListIterator<RetPreemptedTunnel> iterator = tunnels.listIterator();
        while (iterator.hasNext()) {
            RetPreemptedTunnel tunnelSrc = iterator.next();
            if (tunnelSrc.preemptingTunnel.isBwSharedGroup()) {
                List<TunnelUnifyKey> tunnelsSrc = BwSharedGroupMng.getInstance()
                        .getMembersOnPort(tunnelSrc.preemptingTunnel.getBwSharedGroupKey(), portKey, isSimulate);
                iterator.remove();
                for (TunnelUnifyKey tunnelTemp : tunnelsSrc) {
                    iterator.add(new RetPreemptedTunnel(tunnelTemp, tunnelSrc.preemptingTunnel));
                }
            }
        }
    }

    public void merge(PceResult srcTunnels) {
        if (srcTunnels == null) {
            return;
        }
        this.tunnels.addAll(srcTunnels.tunnels);
        if (srcTunnels.isReservedBwEnlarged) {
            this.isReservedBwEnlarged = true;
        }
        if (srcTunnels.getCalcFailType() != null) {
            this.setCalcFailType(srcTunnels.getCalcFailType());
        }
    }

    public CalcFailType getCalcFailType() {
        return calcFailType;
    }

    public void setCalcFailType(CalcFailType calcFailType) {
        if (this.calcFailType != CalcFailType.DelayIneligible) {
            this.calcFailType = calcFailType;
        }
    }

    public int size() {
        return tunnels.size();
    }

    public boolean contains(TunnelUnifyKey key) {
        for (RetPreemptedTunnel tunnel : tunnels) {
            if (tunnel.isKeyEquals(key)) {
                return true;
            }
        }
        return false;
    }

    public void deleteSamePreemptedTunnels() {
        Set<RetPreemptedTunnel> filter = new HashSet<>();
        List<RetPreemptedTunnel> noSameTunels = new ArrayList<>();

        for (RetPreemptedTunnel tunnel : tunnels) {
            if (filter.add(tunnel)) {
                noSameTunels.add(tunnel);
            }
        }
        tunnels.clear();
        tunnels.addAll(noSameTunels);
    }

    public List<PreemptedTunnel> preemptedTunnelsProcess(boolean isNotifyPreemptTunnel) {
        List<PreemptedTunnel> outputPreemptedTunnels = new ArrayList<>();
        List<RetPreemptedTunnel> curLevelPreemptedTunnels = new ArrayList<>();
        List<RetPreemptedTunnel> newPreemptedTunnels = new ArrayList<>();

        if (null == tunnels) {
            return outputPreemptedTunnels;
        }

        curLevelPreemptedTunnels.addAll(tunnels);
        Long preemptLevel = 1L;
        while (!curLevelPreemptedTunnels.isEmpty()) {
            Iterator<RetPreemptedTunnel> tunnelItr = curLevelPreemptedTunnels.iterator();
            while (tunnelItr.hasNext()) {
                RetPreemptedTunnel tunnelKey = tunnelItr.next();
                TunnelUnifyKey pathKey = tunnelKey.getPathKey();
                ITunnel preemptedTunnel = PcePathProvider.getInstance().getTunnelInstance(pathKey);
                if (null == preemptedTunnel) {
                    continue;
                }
                LOG.info("preemptedTunnelsProcess reCalc {}", preemptedTunnel.getTunnelUnifyKey());
                newPreemptedTunnels.addAll(preemptedTunnel.reCalcPath(pathKey).tunnels);
                preemptedTunnel.writeDb();
                preemptedTunnel.writeMemory();
                TriggerPreemptTunnel tiggerTunnel = new TriggerPreemptTunnelBuilder()
                        .setTunnelId((long) tunnelKey.preemptingTunnel.getTunnelId())
                        .setHeadNodeId(tunnelKey.preemptingTunnel.getHeadNode())
                        .build();

                PreemptedTunnel outputPreemptedTunnel = new PreemptedTunnelBuilder()
                        .setTunnelId((long) preemptedTunnel.getId())
                        .setHeadNodeId(preemptedTunnel.getHeadNode())
                        .setMasterPath(new MasterPathBuilder()
                                               .setPathLink(PceUtil.transform2PathLink(preemptedTunnel.getMasterPath()))
                                               .build())
                        .setSlavePath(new SlavePathBuilder()
                                              .setPathLink(PceUtil.transform2PathLink(preemptedTunnel.getSlavePath()))
                                              .build())
                        .setTriggerPreemptTunnel(tiggerTunnel)
                        .setPreemptLevel(preemptLevel)
                        .build();
                outputPreemptedTunnels.add(outputPreemptedTunnel);
                if (isNotifyPreemptTunnel || !PcePathProvider.isTunnelDispatchBandwidth()) {
                    preemptedTunnel.notifyPathChange();
                }
            }
            preemptLevel++;
            curLevelPreemptedTunnels.clear();
            curLevelPreemptedTunnels.addAll(newPreemptedTunnels);
            newPreemptedTunnels.clear();
        }
        Collections.reverse(outputPreemptedTunnels);
        return outputPreemptedTunnels;
    }

    public boolean isCalcFail() {
        return this.calcFail;
    }

    public void setCalcFail(boolean calcFail) {
        this.calcFail = calcFail;
    }

    public CalcFailType getFailReason() {
        return failReason;
    }

    public void setFailReason(CalcFailType failReason) {
        this.failReason = failReason;
    }

    public List<BandWidthScale> getBandWidthScaleList() {
        return bandWidthScaleList;
    }

    public PceResult setBandWidthScaleList(List<BandWidthScale> bandWidthScaleList) {
        this.bandWidthScaleList = bandWidthScaleList;
        return this;
    }

    private class RetPreemptedTunnel {
        RetPreemptedTunnel(TunnelUnifyKey key, TunnelUnifyKey preemptingTunnel) {
            this.key = key;
            this.preemptingTunnel = preemptingTunnel;
        }

        TunnelUnifyKey preemptingTunnel;
        private TunnelUnifyKey key;

        TunnelUnifyKey getPathKey() {
            return key;
        }

        boolean isKeyEquals(TunnelUnifyKey key) {
            return this.key.equals(key);
        }

        @Override
        public int hashCode() {
            int result = key.hashCode();
            result = 31 * result + preemptingTunnel.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object ob) {
            if (this == ob) {
                return true;
            }
            if (ob == null || getClass() != ob.getClass()) {
                return false;
            }

            RetPreemptedTunnel that = (RetPreemptedTunnel) ob;

            if (!key.equals(that.key)) {
                return false;
            }

            return preemptingTunnel.equals(that.preemptingTunnel);
        }
    }
}
