/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath;

import java.util.List;
import java.util.Optional;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroupMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;

import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;

/**
 * Created by 10204924 on 2017/7/5.
 */
public class HsbBwMngUtils {
    private HsbBwMngUtils() {
    }

    static void recoverHsbPathBw(HsbPathInstance hsb) {
        if (hsb.getBiDirect() != null && hsb.getBiDirect().isReverse()) {
            return;
        }

        if (hsb.getBwSharedGroups() != null) {
            BwSharedGroupMng.getInstance().recoverBw(
                    hsb.getMasterPath(),
                    hsb.getTeArgumentBean().getPreemptPriority(),
                    hsb.getTeArgumentBean().getHoldPriority(),
                    hsb.getMasterTunnelUnifyKey(),
                    BiDirect.isBiDirect(hsb.getBiDirect()),
                    hsb.getBwSharedGroups());
            if (isNeedRecoverSlavePathBw(hsb)) {
                BwSharedGroupMng.getInstance().recoverBw(
                        hsb.getSlavePath(),
                        hsb.getTeArgumentBean().getPreemptPriority(),
                        hsb.getTeArgumentBean().getHoldPriority(),
                        hsb.getSlaveTunnelUnifyKey(),
                        BiDirect.isBiDirect(hsb.getBiDirect()),
                        hsb.getBwSharedGroups());
            }
            return;
        }
        if (!CollectionUtils.isNullOrEmpty(hsb.getMasterPath())) {
            BandWidthMng.getInstance().recoverPathBw(hsb.getMasterPath(), hsb.getTeArgumentBean().getHoldPriority(),
                                                     hsb.getTeArgumentBean().getBandWidth(),
                                                     hsb.getMasterTunnelUnifyKey(),
                                                     BiDirect.isBiDirect(hsb.getBiDirect()));
        }

        if (isNeedRecoverSlavePathBw(hsb)) {
            BandWidthMng.getInstance().recoverPathBw(hsb.getSlavePath(), hsb.getTeArgumentBean().getHoldPriority(),
                                                     hsb.getTeArgumentBean().getBandWidth(),
                                                     hsb.getSlaveTunnelUnifyKey(),
                                                     BiDirect.isBiDirect(hsb.getBiDirect()));
        }
    }

    private static boolean isNeedRecoverSlavePathBw(HsbPathInstance hsb) {
        return !CollectionUtils.isNullOrEmpty(hsb.getSlavePath())
                && hsb.getTeArgumentBean().isComputeLspWithBandWidth();
    }

    public static BwSharedGroupContainer decreaseBw(
            HsbPathInstance hsb, long newBandwidth,
            BwSharedGroupContainer bwContainer) {

        BwSharedGroupContainer newGroups;
        if ((bwContainer == null || CollectionUtils.isNullOrEmpty(bwContainer.getBwSharedGroupMember()))
                && hsb.getBwSharedGroups() == null) {
            newGroups = null;
            BandWidthMng.getInstance().decreasePathBw(hsb.getMasterPath(), newBandwidth, hsb.getHoldPriority(),
                                                      hsb.getMasterTunnelUnifyKey(), hsb.isBiDirect());
            BandWidthMng.getInstance().decreasePathBw(hsb.getSlavePath(), newBandwidth, hsb.getHoldPriority(),
                                                      hsb.getSlaveTunnelUnifyKey(), hsb.isBiDirect());
        } else {
            newGroups = bwContainer;
            BwSharedGroupMng.getInstance().decreaseGroupBw(newGroups, hsb.isSimulate());
        }
        return newGroups;
    }

    public static void releaseHsbBandWidth(HsbPathInstance hsb) {
        PceResult pceResult = new PceResult();
        if (hsb.teArg != null && hsb.teArg.getBandWidth() != 0 && hsb.getBwSharedGroups() == null) {
            //refresh after both master lsp and slave lsp are freed
            BandWidthMng.getInstance()
                    .free(hsb.getMasterLsp(), hsb.getHoldPriority(), hsb.getMasterTunnelUnifyKey(), pceResult,
                          hsb.isBiDirect());
            BandWidthMng.getInstance()
                    .free(hsb.getSlaveLsp(), hsb.getHoldPriority(), hsb.getSlaveTunnelUnifyKey(), pceResult,
                          hsb.isBiDirect());
        }

        if (hsb.getBwSharedGroups() != null) {
            BwSharedGroupMng.getInstance()
                    .delTunnelAndFreeBw(hsb.getMasterLsp(), hsb.getMasterTunnelUnifyKey(), hsb.getBwSharedGroups(),
                                        hsb.isBiDirect(), pceResult);
            BwSharedGroupMng.getInstance()
                    .delTunnelAndFreeBw(hsb.getSlaveLsp(), hsb.getSlaveTunnelUnifyKey(), hsb.getBwSharedGroups(),
                                        hsb.isBiDirect(), pceResult);
        }

        if (pceResult.isNeedRefreshUnestablishTunnels()) {
            PcePathProvider.getInstance()
                    .refreshUnestablishTunnels(hsb.isSimulate(), hsb.getTopoId(), hsb.getMasterTunnelUnifyKey(),
                                               hsb.getHoldPriority());
        }
    }

    public static boolean isPathHasEnoughBw(HsbPathInstance hsb, List<Link> pathLinks, TunnelUnifyKey tunnelUnifyKey) {
        long needBw;
        byte priority;

        if (hsb.teArg != null && !hsb.teArg.isComputeLspWithBandWidth()) {
            return true;
        }
        needBw = Optional.ofNullable(hsb.teArg).map(TeArgumentBean::getBandWidth).orElse(0L);
        priority = Optional.ofNullable(hsb.teArg).map(TeArgumentBean::getHoldPriority).orElse((byte) 7);
        return isHasEnoughBw(hsb.getBwSharedGroups(), needBw, priority, pathLinks, tunnelUnifyKey);
    }

    private static boolean isHasEnoughBw(
            BwSharedGroupContainer bwSharedGroups, long needBw, byte priority,
            List<Link> pathLinks, TunnelUnifyKey tunnelUnifyKey) {
        for (Link link : pathLinks) {
            boolean isTunnelOccupyLink;
            if (bwSharedGroups != null) {
                isTunnelOccupyLink =
                        BwSharedGroupMng.getInstance().isBwShareGroupOccupyLink(link, tunnelUnifyKey, bwSharedGroups);
            } else {
                isTunnelOccupyLink = BandWidthMng.getInstance().isTunnelOccupyLink(tunnelUnifyKey, link);
            }

            long reservedBw = BandWidthMng.getInstance().queryReservedBw(link, priority);
            if (!isTunnelOccupyLink && reservedBw < needBw) {
                return false;
            }
        }
        return true;
    }
}
