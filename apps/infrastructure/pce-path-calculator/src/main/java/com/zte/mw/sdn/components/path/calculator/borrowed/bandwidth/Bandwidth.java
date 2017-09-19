/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

public class Bandwidth {
    public Bandwidth(long bandwidth) {
        this.maxBw = bandwidth;
        this.reservedBw = bandwidth;
        bwOrderedMap = new TreeMap<>(new BwComparator());
    }

    public Bandwidth(Bandwidth source) {
        this.maxBw = source.maxBw;
        this.reservedBw = source.reservedBw;
        bwOrderedMap = new TreeMap<>(new BwComparator());
        copyMapFromOld(source.tunnelBwMap, source.bwOrderedMap);
    }

    private static final Logger LOG = LoggerFactory.getLogger(Bandwidth.class);
    private long maxBw;
    private long reservedBw;
    private Map<TunnelUnifyKey, Long> tunnelBwMap = new HashMap<>();
    private TreeMap<BwTreeKey, Long> bwOrderedMap;

    public void copyMapFromOld(Map<TunnelUnifyKey, Long> oldTunnelBwMap, Map<BwTreeKey, Long> oldBwTreeMap) {
        Function<Map.Entry<TunnelUnifyKey, Long>, TunnelUnifyKey> tranFormKey = entry -> {
            TunnelUnifyKey tunnelUnifyKey = entry.getKey();
            return new TunnelUnifyKey(tunnelUnifyKey).setSimulateFlag(true);
        };
        this.tunnelBwMap =
                oldTunnelBwMap.entrySet().stream().collect(Collectors.toMap(tranFormKey, Map.Entry::getValue));
        BiConsumer<BwTreeKey, Long> transFormFunction = (key, value) -> {
            TunnelUnifyKey tunnelId = new TunnelUnifyKey(key.getTunnelKey());
            tunnelId.setSimulateFlag(true);
            BwTreeKey bwTreeKey = new BwTreeKey(key.getBw(), tunnelId);
            this.bwOrderedMap.put(bwTreeKey, value);
        };
        oldBwTreeMap.forEach(transFormFunction);
    }

    public synchronized void recoverPathBw(TunnelUnifyKey tunnelId, long demandBw) {
        Long bw = tunnelBwMap.get(tunnelId);
        if (bw != null) {
            LOG.error("recoverPathBw:bw is already exist{" + "headnode:" + tunnelId.getHeadNode().toString() + "tgid:"
                              + tunnelId.getTgId() + "tunnelid" + tunnelId.getTunnelId() + "}!");
            return;
        }
        try {
            allocBw(tunnelId, demandBw);
        } catch (BandwidthAllocException e) {
            LOG.error("", e);
            LOG.error(
                    "recoverPathBw:allocBw error{" + "tgid:" + tunnelId.getTgId() + "tunnelId:" + tunnelId.getTunnelId()
                            + "maxBw:" + maxBw + "reserveBw:" + reservedBw + "}!");
        }
    }

    private synchronized void allocBw(TunnelUnifyKey tunnelId, long demandBw) throws BandwidthAllocException {
        if (demandBw == 0) {
            return;
        }
        if (demandBw > reservedBw) {
            throw new BandwidthAllocException();
        }
        reservedBw -= demandBw;
        addRecord(tunnelId, demandBw);
    }

    private synchronized void addRecord(TunnelUnifyKey tunnelId, long allocBw) {
        BwTreeKey bwTreeKey;
        TunnelUnifyKey addKey = tunnelId;

        if (tunnelBwMap.containsKey(tunnelId)) {
            TunnelUnifyKey oldKey = null;
            bwOrderedMap.remove(new BwTreeKey(tunnelBwMap.get(tunnelId), tunnelId));
            for (TunnelUnifyKey key : tunnelBwMap.keySet()) {
                if (key.equals(tunnelId)) {
                    oldKey = key;
                    break;
                }
            }
            if ((oldKey != null) && (!oldKey.isHsbFlag())) {
                addKey = oldKey;
            } else {
                tunnelBwMap.remove(oldKey);
            }
            bwTreeKey = new BwTreeKey(allocBw, addKey);
        } else {
            bwTreeKey = new BwTreeKey(allocBw, addKey);
        }
        tunnelBwMap.put(addKey, allocBw);
        bwOrderedMap.put(bwTreeKey, allocBw);
    }

    public synchronized void alloc(TunnelUnifyKey tunnelId, long demandBw) throws BandwidthAllocException {
        Long bw = tunnelBwMap.get(tunnelId);
        if (bw != null) {
            reallocBw(tunnelId, bw, demandBw);
            return;
        }
        allocBw(tunnelId, demandBw);
    }

    private synchronized void reallocBw(TunnelUnifyKey tunnelId, long oldBw, long newBw)
            throws BandwidthAllocException {
        reservedBw += oldBw;

        if (reservedBw < newBw) {
            throw new BandwidthAllocException();
        }
        if (newBw == 0) {
            delRecord(tunnelId, oldBw);
            return;
        }
        reservedBw -= newBw;
        addRecord(tunnelId, newBw);
    }

    private synchronized void delRecord(TunnelUnifyKey tunnelId, Long allocBw) {
        BwTreeKey bwTreeKey = new BwTreeKey(allocBw, tunnelId);
        bwOrderedMap.remove(bwTreeKey);
        tunnelBwMap.remove(tunnelId);
    }

    public synchronized void alloc(List<TunnelKeyBw> keyList) {
        for (TunnelKeyBw keyBw : keyList) {
            Long bw = tunnelBwMap.get(keyBw.getTunnelUnifyKey());
            if (bw != null) {
                reservedBw += bw;
                delRecord(keyBw.getTunnelUnifyKey(), bw);
            }
            if (keyBw.demandBw == 0) {
                return;
            }
            reservedBw -= keyBw.demandBw;
            addRecord(keyBw.getTunnelUnifyKey(), keyBw.demandBw);
        }
    }

    public synchronized void updateUnifyKey(TunnelUnifyKey tunnelKeyOld, TunnelUnifyKey tunnelKeyNew) {
        Long bw = tunnelBwMap.get(tunnelKeyOld);
        if (bw == null) {
            LOG.error("updateUnifyKey error! there is no tunnelKey:" + tunnelKeyOld);
            return;
        }

        delRecord(tunnelKeyOld, bw);
        addRecord(tunnelKeyNew, bw);
    }

    public synchronized long preempt(long demandBw, List<TunnelUnifyKey> tunnelsNeed2Release) {
        long needPreemptTunnelBw;
        long freeTunnelBw = 0;

        if (reservedBw >= demandBw) {
            decreaseMaxBw(demandBw);
            return freeTunnelBw;
        }

        needPreemptTunnelBw = demandBw - reservedBw;
        freeTunnelBw = freeTunnelBandwidth(needPreemptTunnelBw, tunnelsNeed2Release);
        decreaseMaxBw(demandBw);
        return freeTunnelBw;
    }

    public synchronized void decreaseMaxBw(long decreaseBw) {
        if (reservedBw < decreaseBw) {
            Logs.error(LOG, "bandwidth exception!reservedBw:{},decreaseBw:{}", reservedBw, decreaseBw);
            return;
        }

        maxBw -= decreaseBw;
        reservedBw -= decreaseBw;
    }

    private synchronized long freeTunnelBandwidth(long demandBw, List<TunnelUnifyKey> freeTunnels) {
        long freeTunnelBw = 0;
        long needPreemptTunnelBw = demandBw;

        while (true) {
            TunnelUnifyKey tunnelKey = findMatchedTunnel(needPreemptTunnelBw);
            if (null == tunnelKey) {
                return freeTunnelBw;
            }

            Long tunnelBw = tunnelBwMap.get(tunnelKey);
            freeTunnelBw += free(tunnelKey);
            freeTunnels.add(tunnelKey);

            if (tunnelBw >= needPreemptTunnelBw) {
                return freeTunnelBw;
            }
            needPreemptTunnelBw -= tunnelBw;
        }
    }

    /*get equal one,if not exist,get higher one,if not exist ,get lower one */
    private synchronized TunnelUnifyKey findMatchedTunnel(long bandwidth) {
        BwTreeKey bwTreeKey = new BwTreeKey(bandwidth);

        BwTreeKey bwTreeKeyFound = bwOrderedMap.ceilingKey(bwTreeKey);
        if (bwTreeKeyFound == null) {
            bwTreeKeyFound = bwOrderedMap.floorKey(bwTreeKey);
        }

        if (bwTreeKeyFound != null) {
            return bwTreeKeyFound.tunnelKey;
        }
        return null;
    }

    public synchronized long free(TunnelUnifyKey tunnelId) {
        Long bw = tunnelBwMap.get(tunnelId);
        if (bw != null) {
            TunnelUnifyKey oldKey = null;
            for (TunnelUnifyKey key : tunnelBwMap.keySet()) {
                if (key.equals(tunnelId)) {
                    oldKey = key;
                    break;
                }
            }
            if (oldKey != null && !oldKey.isHsbFlag() && tunnelId.isHsbFlag()) {
                return 0;
            }
            delRecord(tunnelId, bw);
            reservedBw += bw;
            return bw;
        }
        return 0;
    }

    public synchronized boolean hasEnoughBw(long demandBw) {
        return demandBw <= reservedBw;
    }

    public synchronized long bwNeeded(long demandBw) {
        return demandBw <= reservedBw ? 0 : demandBw - reservedBw;
    }

    public synchronized void increaseMaxBw(long increaseBw) {
        maxBw += increaseBw;
        reservedBw += increaseBw;
    }

    public synchronized long getMaxBw() {
        return maxBw;
    }

    public synchronized long decreaseMaxBwFreeTunnelIfNecessary(long decreaseBw, List<TunnelUnifyKey> allTunnels) {
        if (maxBw < decreaseBw) {
            LOG.error("bandwidth exception! maxBw:" + maxBw + ",decreaseBw:" + decreaseBw);
            return reservedBw;
        }
        allTunnels.addAll(decreaseMaxBwFreeTunnelIfNecessary(decreaseBw));
        return reservedBw;
    }

    public synchronized List<TunnelUnifyKey> decreaseMaxBwFreeTunnelIfNecessary(long decreaseBw) {
        List<TunnelUnifyKey> freeTunnels = new ArrayList<>();

        if (maxBw < decreaseBw) {
            LOG.error("bandwidth exception! maxBw:" + maxBw + ",decreaseBw:" + decreaseBw);
            return freeTunnels;
        }
        // 如果剩余带宽不够减少，则要释放一部分隧道带宽
        if (reservedBw < decreaseBw) {
            long freeTunnelBw = decreaseBw - reservedBw;
            freeTunnelBandwidth(freeTunnelBw, freeTunnels);
        }
        decreaseMaxBw(decreaseBw);
        return freeTunnels;
    }

    public synchronized long getReservedBw() {
        return reservedBw;
    }

    public synchronized Set<TunnelUnifyKey> getTunnelSet() {
        return tunnelBwMap.keySet();
    }

    public synchronized long getTunnelBw(TunnelUnifyKey tunnelId) {
        if (tunnelBwMap == null) {
            return 0;
        }
        Long bw = tunnelBwMap.get(tunnelId);
        if (bw == null) {
            return 0;
        } else {
            return bw;
        }
    }

    public synchronized boolean isContainTunnel(TunnelUnifyKey tunnelId) {
        if (tunnelBwMap == null) {
            return false;
        }
        Long bw = tunnelBwMap.get(tunnelId);
        if (bw == null) {
            return false;
        }
        return true;
    }

    public synchronized long queryBwOccupiedByTunnel(TunnelUnifyKey tunnelId) {
        if (tunnelBwMap == null) {
            return 0;
        }
        Long bw = tunnelBwMap.get(tunnelId);
        if (bw == null) {
            return 0;
        }
        return bw;
    }

    private class BwComparator implements Comparator<BwTreeKey> {
        @Override
        public int compare(BwTreeKey o1, BwTreeKey o2) {

            if (o1.bw != o2.bw) {
                return (o1.bw > o2.bw) ? 1 : -1;
            }

            //tunnelKey is null only if find is executed.
            if ((o1.tunnelKey == null) || (o2.tunnelKey == null)) {
                return 0;
            }

            return (o1.hashCode() > o2.hashCode()) ? 1 : (o1.hashCode() == o2.hashCode()) ? 0 : -1;
        }
    }

    private class BwTreeKey {
        BwTreeKey(long bwIn) {
            this.bw = bwIn;
        }

        BwTreeKey(long bwIn, TunnelUnifyKey tunnelKeyIn) {
            this.bw = bwIn;
            this.tunnelKey = tunnelKeyIn;
        }

        private long bw;
        private TunnelUnifyKey tunnelKey;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + (int) (bw ^ (bw >>> 32));
            result = prime * result + ((tunnelKey == null) ? 0 : tunnelKey.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            return isBwTreeKeyEqual((BwTreeKey) obj);
        }

        private boolean isBwTreeKeyEqual(BwTreeKey other) {
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (bw != other.bw) {
                return false;
            }

            if (!isTunnelKeyEqual(other)) {
                return false;
            }
            return true;
        }

        private boolean isTunnelKeyEqual(BwTreeKey other) {
            if (tunnelKey == null) {
                if (other.tunnelKey != null) {
                    return false;
                }
            } else if (!tunnelKey.equals(other.tunnelKey)) {
                return false;
            }
            return true;
        }

        private Bandwidth getOuterType() {
            return Bandwidth.this;
        }

        public long getBw() {
            return bw;
        }

        public TunnelUnifyKey getTunnelKey() {
            return tunnelKey;
        }
    }

    @Override
    public synchronized String toString() {
        String str = "maxBandwidth:" + maxBw + "," + "reservedBandwidth:" + reservedBw + "\n";

        Set<BwTreeKey> keys = bwOrderedMap.keySet();
        for (BwTreeKey key : keys) {
            str += "key:" + "\n" + key.tunnelKey + "\n" + "bandwidth:" + bwOrderedMap.get(key) + "\n\n";
        }

        return str;
    }
}
