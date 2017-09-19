/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;

import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.Conditions;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

public class DiffServBw {
    public DiffServBw(long maxBw) {
        for (int i = 0; i < priorityNum; ++i) {
            bws.addLast(new Bandwidth(maxBw));
        }
    }

    public DiffServBw(DiffServBw source) {
        source.bws.forEach(bandWidth -> this.bws.add(new Bandwidth(bandWidth)));
    }

    public static final byte HIGHEST_PRIORITY = 0;
    private static final Logger LOG = LoggerFactory.getLogger(DiffServBw.class);
    private static byte priorityNum = 8;
    public static final byte LOWEST_PRIORITY = (byte) (HIGHEST_PRIORITY + priorityNum - 1);
    private LinkedList<Bandwidth> bws = Lists.newLinkedList();

    private static byte rectifyPriority(byte priority) {
        if (priorityAboveHighestLevel(priority)) {
            return HIGHEST_PRIORITY;
        }

        if (priorityBelowLowestLevel(priority)) {
            return LOWEST_PRIORITY;
        }
        return priority;
    }

    private static boolean priorityAboveHighestLevel(byte priority) {
        return priority < HIGHEST_PRIORITY;
    }

    private static boolean priorityBelowLowestLevel(byte priority) {
        return priority > LOWEST_PRIORITY;
    }

    private static boolean priorityIllegal(byte validPreemptPriority, byte validHoldPriority) {
        return validPreemptPriority < validHoldPriority;
    }

    public boolean isOccupiedByTunnel(TunnelUnifyKey key) {
        for (Bandwidth bw : bws) {
            if (bw.isContainTunnel(key)) {
                return true;
            }
        }
        return false;
    }

    public long queryBwOccupiedByTunnel(TunnelUnifyKey key) {
        for (Bandwidth bw : bws) {
            if (bw.isContainTunnel(key)) {
                return bw.queryBwOccupiedByTunnel(key);
            }
        }
        return 0;
    }

    public void recoverPathBw(byte holdPriority, TunnelUnifyKey tunnelId, long demandBw) {
        if (0 != bws.get(holdPriority).getTunnelBw(tunnelId)) {
            LOG.error("recoverPathBw:tunnelId already exist{"
                              + "headnode:" + tunnelId.getHeadNode().toString()
                              + "tunnelid:" + tunnelId.getTunnelId()
                              + "tgid:" + tunnelId.getTgId()
                              + "}!");
            return;
        }

        bws.get(holdPriority).recoverPathBw(tunnelId, demandBw);
        if (holdPriority == LOWEST_PRIORITY) {
            return;
        }
        try {
            decreaseSomeMaxBw(LOWEST_PRIORITY, (byte) (holdPriority + 1), demandBw);
        } catch (BandwidthAllocException e) {
            LOG.error("", e);
            LOG.error("recoverPathBw:decreaseSomeMaxBw error{"
                              + "holdPriority:" + holdPriority + "}!");
        }
    }

    private void decreaseSomeMaxBw(byte lowPri, byte highPri, long bw) throws BandwidthAllocException {
        for (byte i = highPri; i <= lowPri; i++) {
            if (!bws.get(i).hasEnoughBw(bw)) {
                throw new BandwidthAllocException();
            }
            bws.get(i).decreaseMaxBw(bw);
        }
    }

    public long getReserveBandwidth() {
        return bws.get(LOWEST_PRIORITY).getReservedBw();
    }

    public List<TunnelUnifyKey> updateBandwidth(long newBw) {
        long oldBw = getBandwidth();
        List<TunnelUnifyKey> freeTunnels = new ArrayList<>();

        if (newBw < oldBw) {
            freeTunnels.addAll(bws.get(HIGHEST_PRIORITY).decreaseMaxBwFreeTunnelIfNecessary(oldBw - newBw));
            IntStream.rangeClosed(HIGHEST_PRIORITY + 1, LOWEST_PRIORITY).forEachOrdered(value -> bws.get(value)
                    .decreaseMaxBwFreeTunnelIfNecessary(
                            bws.get(value).getMaxBw() - getPrePriorityReserveBandwidth(value), freeTunnels));
        } else if (newBw > oldBw) {
            for (Bandwidth bw : bws) {
                bw.increaseMaxBw(newBw - oldBw);
            }
        }
        return freeTunnels;
    }

    public long getBandwidth() {
        return bws.get(HIGHEST_PRIORITY).getMaxBw();
    }

    private long getPrePriorityReserveBandwidth(int priority) {
        return priority == HIGHEST_PRIORITY ? getBandwidth() : getPriorityBandwidth((byte) (priority - 1));
    }

    public long getPriorityBandwidth(byte priority) {
        return bws.get(priority).getReservedBw();
    }

    public boolean hasEnoughBw(byte preemptPriority, byte holdPriority, long demandBw, TunnelUnifyKey tunnelUnifyKey) {
        byte validPreemptPriority = rectifyPriority(preemptPriority);
        byte validHoldPriority = rectifyPriority(holdPriority);
        long hasBw = bws.get(validHoldPriority).getTunnelBw(tunnelUnifyKey);
        if (hasBw >= demandBw) {
            return true;
        } else {
            return getBwByPriority(validPreemptPriority).hasEnoughBw(demandBw - hasBw);
        }
    }

    private Bandwidth getBwByPriority(byte priority) {
        return bws.get(rectifyPriority(priority));
    }

    public boolean hasEnoughBw(
            byte preemptPriority, byte holdPriority, List<TunnelKeyBw> tunnelKeyBws,
            List<TunnelKeyBw> deletedtunnelKeyBws) {
        final byte validPreemptPriority = rectifyPriority(preemptPriority);
        long increaseBw = getIncreaseBw(holdPriority, tunnelKeyBws, deletedtunnelKeyBws);
        if (increaseBw <= 0) {
            return true;
        }

        return getBwByPriority(validPreemptPriority).hasEnoughBw(increaseBw);
    }

    private long getIncreaseBw(
            byte holdPriority, List<TunnelKeyBw> tunnelKeyBws,
            List<TunnelKeyBw> deletedtunnelKeyBws) {
        final byte validHoldPriority = rectifyPriority(holdPriority);

        long increaseBw = 0;
        if (deletedtunnelKeyBws != null) {
            for (TunnelKeyBw tunnelKeyBw : deletedtunnelKeyBws) {
                increaseBw -= tunnelKeyBw.getBw();
            }
        }

        for (TunnelKeyBw tunnelKeyBw : tunnelKeyBws) {
            long hasBw = bws.get(validHoldPriority).getTunnelBw(tunnelKeyBw.getTunnelUnifyKey());
            long demandBw = tunnelKeyBw.getBw();

            if (hasBw >= demandBw) {
                increaseBw -= (hasBw - demandBw);
            } else {
                increaseBw += (demandBw - hasBw);
            }
        }

        return increaseBw;
    }

    public PceResult alloc(
            byte preemptPriority, byte holdPriority, TunnelUnifyKey tunnelKey,
            long demandBw) throws BandwidthAllocException {
        byte validPreemptPriority = rectifyPriority(preemptPriority);
        byte validHoldPriority = rectifyPriority(holdPriority);
        PceResult result = new PceResult();

        if (validPreemptPriority < validHoldPriority) {
            throw new BandwidthAllocException();
        }
        long allocatedBw = bws.get(validHoldPriority).getTunnelBw(tunnelKey);

        //alloc has BandwidthAllocException
        bws.get(validHoldPriority).alloc(tunnelKey, demandBw);

        if (allocatedBw == demandBw) {
            return result;
        }

        if (allocatedBw > demandBw) {
            free(validHoldPriority, allocatedBw - demandBw);
            result.enableBandwidthFreeFlag();
            return result;
        }

        decreaseSomeMaxBw(validPreemptPriority, (byte) (validHoldPriority + 1), demandBw - allocatedBw);

        List<TunnelUnifyKey> preemptedTunnels = preempt(validPreemptPriority, demandBw - allocatedBw);
        for (TunnelUnifyKey tunnel : preemptedTunnels) {
            result.addPreemptedTunnel(tunnel, tunnelKey);
        }
        return result;
    }

    private void free(byte priority, long freeBw) {
        byte validPriority = rectifyPriority(priority);

        for (byte i = (byte) (validPriority + 1); i <= LOWEST_PRIORITY; i++) {
            Bandwidth bw = bws.get(i);
            bw.increaseMaxBw(freeBw);
        }
    }

    private List<TunnelUnifyKey> preempt(byte validPreemptPriority, long preemptBw) {
        List<TunnelUnifyKey> allTunnelNeed2Release = new ArrayList<>();
        List<TunnelUnifyKey> tunnelNeed2Release = new ArrayList<>();

        for (byte i = (byte) (validPreemptPriority + 1); i <= LOWEST_PRIORITY; i++) {
            long freeBw = bws.get(i).preempt(preemptBw, tunnelNeed2Release);
            if (0 != freeBw) {
                for (byte j = (byte) (i + 1); j <= LOWEST_PRIORITY; j++) {
                    bws.get(j).increaseMaxBw(freeBw);
                }
            }
            allTunnelNeed2Release.addAll(tunnelNeed2Release);
            tunnelNeed2Release.clear();
        }
        return allTunnelNeed2Release;
    }

    public PceResult alloc(
            byte preemptPriority, byte holdPriority, List<TunnelKeyBw> tunnelKeyList,
            TunnelUnifyKey establishTunnel) {
        byte validPreemptPriority = rectifyPriority(preemptPriority);
        byte validHoldPriority = rectifyPriority(holdPriority);
        PceResult result = new PceResult();

        boolean priorityIllegal = priorityIllegal(validPreemptPriority, validHoldPriority);
        Conditions.ifTrue(priorityIllegal, () -> Logs
                .error(LOG, "validPreemptPriority < validHoldPriority, preemptPriority={} holdPriority={}",
                       preemptPriority, holdPriority));

        if (priorityIllegal || CollectionUtils.isNullOrEmpty(tunnelKeyList)) {
            Logs.error(LOG, "priority error {} or tunnel list is error {}", priorityIllegal, tunnelKeyList);
            return result;
        }

        long allocatedBw = 0;
        long newBw = 0;

        for (TunnelKeyBw keyBw : tunnelKeyList) {
            allocatedBw += bws.get(validHoldPriority).getTunnelBw(keyBw.getTunnelUnifyKey());
            newBw += keyBw.demandBw;
        }
        if (!bws.get(validHoldPriority).hasEnoughBw(newBw - allocatedBw)) {
            Logs.error(LOG, "bandWidth has no enough bw for {},only has {}", newBw - allocatedBw,
                       bws.get(validHoldPriority).getReservedBw());
            return result;
        }

        bws.get(validHoldPriority).alloc(tunnelKeyList);

        if (allocatedBw > newBw) {
            free(validHoldPriority, allocatedBw - newBw);
            result.enableBandwidthFreeFlag();
        } else if (allocatedBw < newBw) {

            try {
                decreaseSomeMaxBw(validPreemptPriority, (byte) (validHoldPriority + 1), newBw - allocatedBw);
            } catch (BandwidthAllocException errorInfo) {
                LOG.error("", errorInfo);
            }

            List<TunnelUnifyKey> preemptedTunnels = preempt(validPreemptPriority, newBw - allocatedBw);
            preemptedTunnels.forEach(tunnel -> result.addPreemptedTunnel(tunnel, establishTunnel));
        }
        return result;
    }

    public void decreaseBw(byte holdPriority, TunnelUnifyKey tunnelKey, long newBw) {
        byte validHoldPriority = rectifyPriority(holdPriority);

        long allocatedBw = bws.get(validHoldPriority).getTunnelBw(tunnelKey);

        if (allocatedBw <= newBw) {
            LOG.error("decreaseBw error!" + tunnelKey.toString() + " newBw-" + newBw);
            return;
        }

        try {
            decreaseSomeMaxBw(LOWEST_PRIORITY, (byte) (validHoldPriority + 1), newBw - allocatedBw);
            bws.get(validHoldPriority).alloc(tunnelKey, newBw);
        } catch (BandwidthAllocException error) {
            LOG.error("", error);
        }
    }

    public void updateUnifyKey(
            TunnelUnifyKey tunnelKeyOld, TunnelUnifyKey tunnelKeyNew,
            byte holdPriority) {
        byte validHoldPriority = rectifyPriority(holdPriority);

        bws.get(validHoldPriority).updateUnifyKey(tunnelKeyOld, tunnelKeyNew);
    }

    public long free(byte priority, TunnelUnifyKey tunnelId) {
        byte validPriority = rectifyPriority(priority);
        Bandwidth freeBw = getBwByPriority(priority);

        long bwValue = freeBw.free(tunnelId);

        for (byte i = (byte) (validPriority + 1); i <= LOWEST_PRIORITY; i++) {
            Bandwidth bw = bws.get(i);
            bw.increaseMaxBw(bwValue);
        }
        return bwValue;
    }

    public long queryReservedBw(byte priority) {
        return getBwByPriority(priority).getReservedBw();
    }

    public long queryMaxBw(byte priority) {
        return getBwByPriority(priority).getMaxBw();
    }

    public List<TunnelUnifyKey> getAllTunnelThrough() {
        List<TunnelUnifyKey> tunnelIds = Lists.newArrayList();
        for (Bandwidth bw : bws) {
            tunnelIds.addAll(bw.getTunnelSet());
        }
        return tunnelIds;
    }

    public long bwNeeded(
            byte preemptPriority, byte holdPriority, List<TunnelKeyBw> tunnelKeyBws,
            List<TunnelKeyBw> deletedtunnelKeyBws) {
        final byte validPreemptPriority = rectifyPriority(preemptPriority);
        long increaseBw = getIncreaseBw(holdPriority, tunnelKeyBws, deletedtunnelKeyBws);
        if (increaseBw <= 0) {
            return 0;
        }

        return getBwByPriority(validPreemptPriority).bwNeeded(increaseBw);
    }

    public long bwNeeded(byte preemptPriority, byte holdPriority, long demandBw, TunnelUnifyKey tunnelUnifyKey) {
        byte validPreemptPriority = rectifyPriority(preemptPriority);
        byte validHoldPriority = rectifyPriority(holdPriority);
        long hasBw = bws.get(validHoldPriority).getTunnelBw(tunnelUnifyKey);
        if (hasBw >= demandBw) {
            return 0;
        } else {
            return getBwByPriority(validPreemptPriority).bwNeeded(demandBw - hasBw);
        }
    }

    @Override
    public String toString() {
        String str = "";
        byte priority = HIGHEST_PRIORITY;

        for (Bandwidth bw : bws) {
            str += ("[priority:" + priority + "] " + bw.toString());
            priority++;
        }
        return str;
    }
}
