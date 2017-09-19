/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.bw.shared.group.container.BwSharedGroupMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;

import com.zte.ngip.ipsdn.pce.path.api.util.BwSharedGroupKey;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.PortKey;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

public class BwSharedGroupMng {
    private BwSharedGroupMng() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(BwSharedGroupMng.class);
    private static final BwSharedGroupMng instance = new BwSharedGroupMng();
    private final ConcurrentHashMap<BwSharedGroupKey, BwSharedGroup> bwSharedGroupMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BwSharedGroupKey, BwSharedGroup> bwSimuSharedGroupMap = new ConcurrentHashMap<>();

    public static BwSharedGroupMng getInstance() {
        return instance;
    }

    public static BwSharedGroupContainer getDeletedGroups(
            BwSharedGroupContainer newGroups,
            BwSharedGroupContainer oldGroups) {
        if (bwGroupsInvalid(oldGroups)) {
            return null;
        }

        if (bwGroupsInvalid(newGroups)) {
            return oldGroups;
        }
        List<BwSharedGroupMember> deletedGroups = getDeletedBwSharedGroupMembers(newGroups, oldGroups);

        if (deletedGroups.isEmpty()) {
            return null;
        } else {

            return new BwSharedGroupContainerBuilder().setBwSharedGroupMember(deletedGroups).build();
        }
    }

    private static boolean bwGroupsInvalid(BwSharedGroupContainer bwGroups) {
        return (bwGroups == null) || (bwGroups.getBwSharedGroupMember() == null)
                || (bwGroups.getBwSharedGroupMember().isEmpty());
    }

    private static List<BwSharedGroupMember> getDeletedBwSharedGroupMembers(
            BwSharedGroupContainer newGroups,
            BwSharedGroupContainer oldGroups) {
        List<BwSharedGroupMember> deletedGroups = new LinkedList<>();
        for (BwSharedGroupMember bwSharedGroup : oldGroups.getBwSharedGroupMember()) {
            if (containerContainsGroup(newGroups, bwSharedGroup)) {
                continue;
            }
            deletedGroups.add(bwSharedGroup);
        }
        return deletedGroups;
    }

    private static boolean containerContainsGroup(BwSharedGroupContainer groups, BwSharedGroupMember bwSharedGroup) {
        for (BwSharedGroupMember groupTemp : groups.getBwSharedGroupMember()) {
            if (groupTemp.getBwSharedGroupId().equals(bwSharedGroup.getBwSharedGroupId())) {
                return true;
            }
        }
        return false;
    }

    private static List<TunnelKeyBw> getTunnelBws(BwSharedGroupContainer bwSharedGroups, boolean isSimulate) {
        List<TunnelKeyBw> tunnelKeyBws = new LinkedList<>();
        if (bwSharedGroups == null) {
            return tunnelKeyBws;
        }
        for (BwSharedGroupMember bwSharedGroup : bwSharedGroups.getBwSharedGroupMember()) {
            BwSharedGroupKey key = new BwSharedGroupKey(bwSharedGroup.getBwSharedGroupId());
            long bw = 0;
            if (bwSharedGroup.getBandwidth() != null) {
                bw = bwSharedGroup.getBandwidth();
            }
            tunnelKeyBws.add(new TunnelKeyBw(new TunnelUnifyKey(key, isSimulate), bw));
        }
        return tunnelKeyBws;
    }

    public void destroy() {
        bwSharedGroupMap.clear();
        bwSimuSharedGroupMap.clear();
    }

    public int getGroupNum() {
        return bwSharedGroupMap.size();
    }

    public void delTunnelAndFreeBw(
            List<Link> path,
            TunnelUnifyKey tunnelKey, BwSharedGroupContainer bwSharedGroups,
            boolean isBiDirect, PceResult pceResult) {
        if (bwSharedGroups == null) {
            return;
        }
        for (BwSharedGroupMember bwSharedGroup : bwSharedGroups.getBwSharedGroupMember()) {
            BwSharedGroupKey key = new BwSharedGroupKey(bwSharedGroup.getBwSharedGroupId());
            ConcurrentHashMap<BwSharedGroupKey, BwSharedGroup> bwSharedGroupConcurrentHashMap =
                    tunnelKey.isSimulate() ? bwSimuSharedGroupMap : bwSharedGroupMap;
            BwSharedGroup group = bwSharedGroupConcurrentHashMap.get(key);
            if (group == null) {
                continue;
            }
            group.delTunnelAndFreeBw(path, tunnelKey, isBiDirect, pceResult);
            deleteGroup(group);
        }
    }

    private void deleteGroup(BwSharedGroup group) {
        ConcurrentHashMap<BwSharedGroupKey, BwSharedGroup> bwSharedGroupConcurrentHashMap =
                group.getTunnelUnifyKey().isSimulate() ? bwSimuSharedGroupMap : bwSharedGroupMap;
        if (group.groupIsEmpty()) {
            synchronized (this) {
                if (group.groupIsEmpty()) {
                    bwSharedGroupConcurrentHashMap.remove(group.getBwSharedGroupKey());
                }
            }
        }
    }

    public boolean isBwShareGroupOccupyLink(
            Link link, TunnelUnifyKey tunnelKey,
            BwSharedGroupContainer bwSharedGroups) {
        for (BwSharedGroupMember bwSharedGroup : bwSharedGroups.getBwSharedGroupMember()) {
            BwSharedGroupKey key = new BwSharedGroupKey(bwSharedGroup.getBwSharedGroupId());
            ConcurrentHashMap<BwSharedGroupKey, BwSharedGroup> bwSharedGroupConcurrentHashMap =
                    tunnelKey.isSimulate() ? bwSimuSharedGroupMap : bwSharedGroupMap;
            BwSharedGroup group = bwSharedGroupConcurrentHashMap.get(key);
            if (group == null) {
                continue;
            }
            if (!group.isBwShareGroupOccupyLink(new PortKey(link))) {
                return false;
            }
        }
        return true;
    }

    public List<TunnelUnifyKey> getMembersOnPort(BwSharedGroupKey groupKey, PortKey portKey, boolean isSimulate) {
        BwSharedGroup group = isSimulate ? bwSimuSharedGroupMap.get(groupKey) : bwSharedGroupMap.get(groupKey);
        if (group == null) {
            return Collections.emptyList();
        }

        return group.getMembersOnPort(portKey);
    }

    public void freeBw(
            Link link, TunnelUnifyKey tunnelId,
            BwSharedGroupContainer bwSharedGroups,
            PceResult result, boolean isBiDirect) {
        for (BwSharedGroupMember bwSharedGroup : bwSharedGroups.getBwSharedGroupMember()) {
            BwSharedGroupKey key = new BwSharedGroupKey(bwSharedGroup.getBwSharedGroupId());
            ConcurrentHashMap<BwSharedGroupKey, BwSharedGroup> bwSharedGroupConcurrentHashMap =
                    tunnelId.isSimulate() ? bwSimuSharedGroupMap : bwSharedGroupMap;
            BwSharedGroup group = bwSharedGroupConcurrentHashMap.get(key);
            if (group == null) {
                LOG.error("group doesn't exist", key.toString());
                continue;
            }
            group.freeBw(link, tunnelId, isBiDirect, result);
            deleteGroup(group);
        }
    }

    public PceResult allocBw(
            List<Link> path, byte preemptPriority, byte holdPriority,
            TunnelUnifyKey tunnel, BwSharedGroupContainer bwSharedGroups,
            boolean isBiDirect) throws BandwidthAllocException {
        if (path == null) {
            return new PceResult();
        }
        List<TunnelKeyBw> tunnelKeyBws = new LinkedList<>();
        for (BwSharedGroupMember bwSharedGroup : bwSharedGroups.getBwSharedGroupMember()) {
            BwSharedGroupKey key = new BwSharedGroupKey(bwSharedGroup.getBwSharedGroupId());
            long bw = 0;
            if (bwSharedGroup.getBandwidth() != null) {
                bw = bwSharedGroup.getBandwidth();
            }
            BwSharedGroup group = addTunnel(key, bw, tunnel, preemptPriority, holdPriority);
            group.allocBw(path, tunnel, isBiDirect);
            tunnelKeyBws.add(new TunnelKeyBw(group.getTunnelUnifyKey(), bw));
        }

        return BandWidthMng.getInstance().alloc(path, preemptPriority, holdPriority, tunnelKeyBws, tunnel, isBiDirect);
    }

    private BwSharedGroup addTunnel(
            BwSharedGroupKey key, long bw, TunnelUnifyKey tunnel,
            byte preemptPriority, byte holdPriority) {
        ConcurrentHashMap<BwSharedGroupKey, BwSharedGroup> bwSharedGroupConcurrentHashMap =
                tunnel.isSimulate() ? bwSimuSharedGroupMap : bwSharedGroupMap;
        BwSharedGroup group = bwSharedGroupConcurrentHashMap.get(key);
        if (group == null) {
            synchronized (this) {
                group = bwSharedGroupConcurrentHashMap.get(key);
                if (group == null) {
                    group = new BwSharedGroup(key, bw, preemptPriority, holdPriority, tunnel.isSimulate());
                    bwSharedGroupConcurrentHashMap.put(key, group);
                }
            }
        } else {
            group.updateBw(bw);
        }

        group.addTunnelKey(tunnel);
        return group;
    }

    public boolean hasEnoughBw(
            Link link, byte preemptPriority, byte holdPriority,
            BwSharedGroupContainer bwSharedGroups, BwSharedGroupContainer deletedBwSharedGroups, boolean isBidirect,
            TunnelUnifyKey tunnelKey) {
        boolean isSimulate = tunnelKey.isSimulate();
        PortKey portKey = new PortKey(link);
        PortKey reversePortKey = ComUtility.getLinkDestPort(link);
        if (isBidirect && !BandWidthMng.getInstance()
                .hasEnoughBw(reversePortKey, preemptPriority, holdPriority, getTunnelBws(bwSharedGroups, isSimulate),
                             getTunnelBws(deletedBwSharedGroups, tunnelKey, reversePortKey), tunnelKey.isSimulate())) {
            return false;
        }
        return BandWidthMng.getInstance()
                .hasEnoughBw(portKey, preemptPriority, holdPriority, getTunnelBws(bwSharedGroups, isSimulate),
                             getTunnelBws(deletedBwSharedGroups, tunnelKey, portKey), isSimulate);
    }

    private List<TunnelKeyBw> getTunnelBws(
            BwSharedGroupContainer bwSharedGroups, TunnelUnifyKey tunnelUnifyKey,
            PortKey portkey) {
        List<TunnelKeyBw> tunnelKeyBws = new LinkedList<>();
        if (bwSharedGroups == null) {
            return tunnelKeyBws;
        }
        final ConcurrentHashMap<BwSharedGroupKey, BwSharedGroup> bwSharedGroupConcurrentHashMap =
                tunnelUnifyKey.isSimulate() ? bwSimuSharedGroupMap : bwSharedGroupMap;
        for (BwSharedGroupMember bwSharedGroup : bwSharedGroups.getBwSharedGroupMember()) {
            BwSharedGroupKey key = new BwSharedGroupKey(bwSharedGroup.getBwSharedGroupId());
            BwSharedGroup group = bwSharedGroupConcurrentHashMap.get(key);
            if (group == null) {
                continue;
            }
            long bw = group.getMonopolizeBandWidth(portkey, tunnelUnifyKey);
            tunnelKeyBws.add(new TunnelKeyBw(new TunnelUnifyKey(key, tunnelUnifyKey.isSimulate()), bw));
        }
        return tunnelKeyBws;
    }

    public boolean hasEnoughBw(
            Link link, byte preemptPriority, byte holdPriority, long bandWidth,
            BwSharedGroupContainer deletedBwSharedGroups, boolean isBidirect, TunnelUnifyKey tunnelUnifyKey) {
        PortKey portKey = new PortKey(link);
        PortKey reversePortKey = ComUtility.getLinkDestPort(link);
        long tunnelDecreaseBandWidth = getDecreaseBandWidth(deletedBwSharedGroups, tunnelUnifyKey, portKey);
        long tunnelReverseDecreaseBandWidth =
                getDecreaseBandWidth(deletedBwSharedGroups, tunnelUnifyKey, reversePortKey);
        if (isBidirect && !BandWidthMng.getInstance()
                .hasEnoughBw(reversePortKey, preemptPriority, holdPriority, bandWidth - tunnelReverseDecreaseBandWidth,
                             tunnelUnifyKey)) {
            return false;
        }
        return BandWidthMng.getInstance()
                .hasEnoughBw(portKey, preemptPriority, holdPriority, bandWidth - tunnelDecreaseBandWidth,
                             tunnelUnifyKey);
    }

    private long getDecreaseBandWidth(
            BwSharedGroupContainer bwSharedGroups, TunnelUnifyKey tunnelUnifyKey,
            PortKey portkey) {
        if (bwSharedGroups == null || bwSharedGroups.getBwSharedGroupMember() == null) {
            return 0;
        }
        final ConcurrentHashMap<BwSharedGroupKey, BwSharedGroup> bwSharedGroupConcurrentHashMap =
                tunnelUnifyKey.isSimulate() ? bwSimuSharedGroupMap : bwSharedGroupMap;
        return bwSharedGroups.getBwSharedGroupMember().stream().map(BwSharedGroupMember::getBwSharedGroupId)
                .map(BwSharedGroupKey::new).map(bwSharedGroupConcurrentHashMap::get)
                .mapToLong(group -> {
                    if (group == null) {
                        return 0;
                    }
                    return group.getMonopolizeBandWidth(portkey, tunnelUnifyKey);
                }).sum();
    }

    public void recoverBw(
            List<Link> path, byte preemptPriority, byte holdPriority,
            TunnelUnifyKey tunnel, boolean biDirect,
            BwSharedGroupContainer bwSharedGroups) {
        for (BwSharedGroupMember bwSharedGroup : bwSharedGroups.getBwSharedGroupMember()) {
            BwSharedGroupKey key = new BwSharedGroupKey(bwSharedGroup.getBwSharedGroupId());
            long bw = 0;
            if (bwSharedGroup.getBandwidth() != null) {
                bw = bwSharedGroup.getBandwidth();
            }
            BwSharedGroup group = addTunnel(key, bw, tunnel, preemptPriority, holdPriority);
            group.recoverBw(path, bw, tunnel, biDirect);
        }
    }

    public void decreaseGroupBw(BwSharedGroupContainer bwSharedGroups, boolean isSimulate) {
        for (BwSharedGroupMember bwSharedGroup : bwSharedGroups.getBwSharedGroupMember()) {
            BwSharedGroupKey key = new BwSharedGroupKey(bwSharedGroup.getBwSharedGroupId());
            long newBw;
            if (bwSharedGroup.getBandwidth() == null) {
                newBw = 0;
            } else {
                newBw = bwSharedGroup.getBandwidth();
            }
            BwSharedGroup group = isSimulate ? bwSimuSharedGroupMap.get(key) : bwSharedGroupMap.get(key);
            if (group == null) {
                LOG.error("group is null!" + key.toString());
                continue;
            }
            group.decreaseBw(newBw);
        }
    }

    public BwSharedGroup getBwShareGroupByKey(TunnelUnifyKey key) {
        if (!key.isSimulate()) {
            return bwSharedGroupMap.get(key.getBwSharedGroupKey());
        } else {
            return bwSimuSharedGroupMap.get(key.getBwSharedGroupKey());
        }
    }

    public int getGroupTunnelNumOnPort(BwSharedGroupKey key, PortKey port) {
        BwSharedGroup group = bwSharedGroupMap.get(key);
        if (group == null) {
            return 0;
        }
        return group.getTunnelNumOnPort(port);
    }

    @Override
    public String toString() {
        String str = "";

        for (Map.Entry<BwSharedGroupKey, BwSharedGroup> entry : bwSharedGroupMap.entrySet()) {
            str += "========================" + "\n";
            str += entry.getKey().toString() + "\n";
            str += entry.getValue().toString() + "\n";
        }

        return str;
    }

    public String toString(String groupKey) {
        String str = "";

        if (groupKey == null) {
            return str;
        }

        BwSharedGroup group = bwSharedGroupMap.get(new BwSharedGroupKey(groupKey));
        if (group == null) {
            return str;
        }

        return group.toString();
    }

    /**
     * copy all bwGroups for calc offline tunnels.
     */
    public void copyBwSharedGroupMappMirror() {
        bwSharedGroupMap.forEach((key, value) -> bwSimuSharedGroupMap
                .put(new BwSharedGroupKey(key.getGroupId()), new BwSharedGroup(value)));
        LOG.info("bwSimuSharedGroupMap generator {}", bwSimuSharedGroupMap);
    }

    /**
     * destroy all bwGroups for calc offline tunnels.
     */
    public void destroyBwSharedGroupMapMirror() {
        bwSimuSharedGroupMap.clear();
        LOG.info("bwSimuSharedGroupMap clear {}", bwSimuSharedGroupMap);
    }

    public long bwNeedForTunnel(
            Link link, byte preemptPriority, byte holdPriority,
            BwSharedGroupContainer bwSharedGroups, BwSharedGroupContainer deletedBwSharedGroups, boolean isSimulate) {

        return BandWidthMng.getInstance().bwNeedForTunnel(link, preemptPriority, holdPriority,
                                                          getTunnelBws(bwSharedGroups, isSimulate),
                                                          getTunnelBws(deletedBwSharedGroups, isSimulate), isSimulate);
    }
}
