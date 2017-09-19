/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;

import com.zte.ngip.ipsdn.pce.path.api.util.BwSharedGroupKey;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.PortKey;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

public class BwSharedGroup {
    public BwSharedGroup(BwSharedGroupKey groupKey, long bw, byte preemptPriority, byte holdPriority) {
        this.groupKey = groupKey;
        this.unifyKey = new TunnelUnifyKey(groupKey);
        this.bandWidth = bw;
        this.preemptPriority = preemptPriority;
        this.holdPriority = holdPriority;
    }

    public BwSharedGroup(
            BwSharedGroupKey groupKey, long bw, byte preemptPriority, byte holdPriority,
            boolean isSimulate) {
        this.groupKey = groupKey;
        this.unifyKey = new TunnelUnifyKey(groupKey, isSimulate);
        this.bandWidth = bw;
        this.preemptPriority = preemptPriority;
        this.holdPriority = holdPriority;
    }

    public BwSharedGroup(BwSharedGroup source) {
        this.bandWidth = source.bandWidth;
        this.preemptPriority = source.preemptPriority;
        this.holdPriority = source.holdPriority;
        copyParameters(source.groupKey, source.unifyKey, source.members, source.portMap);
    }

    private static final Logger LOG = LoggerFactory.getLogger(BwSharedGroup.class);
    byte preemptPriority = 7;
    byte holdPriority = 7;
    private BwSharedGroupKey groupKey;
    private TunnelUnifyKey unifyKey;
    private long bandWidth = 0;
    private Map<PortKey, List<TunnelUnifyKey>> portMap = new ConcurrentHashMap<>();
    private List<TunnelUnifyKey> members = Collections.synchronizedList(new ArrayList<>());

    /**
     * copy all parameters from old.
     *
     * @param bwSharedGroupKey      group key
     * @param groupKey              key
     * @param vectorTunnelUnifyKeys keyMaps
     * @param portKeyListMap        portKeyMaps
     */
    public void copyParameters(
            BwSharedGroupKey bwSharedGroupKey, TunnelUnifyKey groupKey,
            List<TunnelUnifyKey> vectorTunnelUnifyKeys, Map<PortKey, List<TunnelUnifyKey>> portKeyListMap) {
        setGroupKey(new BwSharedGroupKey(bwSharedGroupKey.getGroupId()));
        vectorTunnelUnifyKeys.forEach(group -> {
            TunnelUnifyKey groupTunnelUnifyKey = new TunnelUnifyKey(group);
            groupTunnelUnifyKey.setSimulateFlag(true);
            this.members.add(groupTunnelUnifyKey);
        });
        setUnifyKey(new TunnelUnifyKey(groupKey));
        unifyKey.setSimulateFlag(true);
        Function<TunnelUnifyKey, TunnelUnifyKey> transFormValue = key -> {
            LOG.info("add element {} to {}", key, bwSharedGroupKey);
            return new TunnelUnifyKey(key).setSimulateFlag(true);
        };
        BiConsumer<PortKey, List<TunnelUnifyKey>> transFormMap = (key, value) -> this.portMap
                .put(
                        new PortKey(key.getNode(), key.getTp()),
                        value.stream().map(transFormValue).collect(Collectors.toList()));
        portKeyListMap.forEach(transFormMap);
    }

    /**
     * set groupKey for bwshareGroup.
     *
     * @param groupKey groupKey of bwshareGroup
     */
    public void setGroupKey(BwSharedGroupKey groupKey) {
        this.groupKey = groupKey;
    }

    /**
     * set unifyKey for bwshareGroup.
     *
     * @param unifyKey unifyKey of bwshareGroup
     */
    public void setUnifyKey(TunnelUnifyKey unifyKey) {
        this.unifyKey = unifyKey;
    }

    public void addTunnelKey(TunnelUnifyKey tunnelKey) {
        if (!members.contains(tunnelKey)) {
            members.add(tunnelKey);
        }
    }

    public List<TunnelUnifyKey> getMemberTunnelKey() {
        List<TunnelUnifyKey> tunnelKeyList = new ArrayList<>();
        if (!members.isEmpty()) {
            tunnelKeyList.addAll(members);
        }

        return tunnelKeyList;
    }

    public void updateBw(long bw) {
        if (bw != bandWidth) {
            bandWidth = bw;
        }
    }

    public void delTunnelAndFreeBw(
            List<Link> path, TunnelUnifyKey tunnelKey, boolean isBiDirect,
            PceResult pceResult) {
        if (members.contains(tunnelKey)) {
            members.remove(tunnelKey);
        }
        freeBw(path, tunnelKey, isBiDirect, pceResult);
    }

    public void freeBw(
            List<Link> path, TunnelUnifyKey tunnel,
            boolean biDirect, PceResult result) {
        if (path == null) {
            return;
        }
        for (Link link : path) {
            freeBwOnPort(new PortKey(link), tunnel, result);
        }

        if (!biDirect) {
            return;
        }

        for (Link link : path) {
            freeBwOnPort(ComUtility.getLinkDestPort(link), tunnel, result);
        }
    }

    private void freeBwOnPort(PortKey portKey, TunnelUnifyKey tunnelKey, PceResult result) {
        if (!portMap.containsKey(portKey)) {
            LOG.error("delPath error," + portKey.toString());
            return;
        }
        List<TunnelUnifyKey> tunnelList = portMap.get(portKey);
        if (tunnelList == null) {
            LOG.error("delPath error,tunnelList null," + portKey.toString());
            return;
        }
        tunnelList.remove(tunnelKey);
        if (tunnelList.isEmpty()) {
            portMap.remove(portKey);
            BandWidthMng.getInstance().free(portKey, holdPriority, unifyKey, result);
        }
    }

    public void decreaseBw(long newBw) {
        if (newBw > bandWidth) {
            LOG.error("decreaseBw error: bandWidth-" + bandWidth + " newBw-" + newBw);
            return;
        }

        if (newBw == bandWidth) {
            return;
        }

        for (Map.Entry<PortKey, List<TunnelUnifyKey>> entry : portMap.entrySet()) {
            BandWidthMng.getInstance().decreasePathBw(entry.getKey(), newBw, holdPriority, unifyKey);
        }
    }

    public boolean groupIsEmpty() {
        return (members.isEmpty()) && (portMap.isEmpty());
    }

    public int getTunnelNumOnPort(PortKey port) {
        List<TunnelUnifyKey> tunnels = portMap.get(port);
        if ((tunnels != null) && (!tunnels.isEmpty())) {
            return tunnels.size();
        }
        return 0;
    }

    public Collection<TunnelUnifyKey> getTunnelOnPort(PortKey port) {
        List<TunnelUnifyKey> tunnels = portMap.get(port);
        if ((tunnels != null) && (!tunnels.isEmpty())) {
            return tunnels;
        }
        return Collections.emptyList();
    }

    public long getBandWidth() {
        return bandWidth;
    }

    public TunnelUnifyKey getTunnelUnifyKey() {
        return unifyKey;
    }

    public BwSharedGroupKey getBwSharedGroupKey() {
        return groupKey;
    }

    public void allocBw(List<Link> path, TunnelUnifyKey tunnel, boolean biDirect) throws BandwidthAllocException {
        for (Link link : path) {
            allocBwOnPort(new PortKey(link), tunnel);
        }

        if (!biDirect) {
            return;
        }

        for (Link link : path) {
            allocBwOnPort(ComUtility.getLinkDestPort(link), tunnel);
        }
    }

    private void allocBwOnPort(PortKey portKey, TunnelUnifyKey tunnel) throws BandwidthAllocException {
        List<TunnelUnifyKey> tunnelList = portMap.get(portKey);
        if (tunnelList == null) {
            tunnelList = new ArrayList<>();
            portMap.put(portKey, tunnelList);
        }

        if (!tunnelList.contains(tunnel)) {
            tunnelList.add(tunnel);
        }
    }

    public void freeBw(
            Link link, TunnelUnifyKey tunnel,
            boolean biDirect, PceResult result) {
        freeBwOnPort(new PortKey(link), tunnel, result);

        if (!biDirect) {
            return;
        }

        freeBwOnPort(ComUtility.getLinkDestPort(link), tunnel, result);
    }

    public boolean isBwShareGroupOccupyLink(PortKey portKey) {
        return BandWidthMng.getInstance().isTunnelOccupyLink(unifyKey, portKey);
    }

    public void recoverBw(
            List<Link> path, long demandBw, TunnelUnifyKey tunnel,
            boolean biDirect) {
        if (path == null || path.isEmpty()) {
            return;
        }

        for (Link link : path) {
            recoverBwOnPort(new PortKey(link), demandBw, tunnel);
        }

        if (!biDirect) {
            return;
        }

        for (Link link : path) {
            recoverBwOnPort(ComUtility.getLinkDestPort(link), demandBw, tunnel);
        }
    }

    private void recoverBwOnPort(PortKey portKey, long demandBw, TunnelUnifyKey tunnelId) {
        List<TunnelUnifyKey> tunnelList = portMap.get(portKey);
        if (tunnelList == null) {
            tunnelList = new ArrayList<>();
            portMap.put(portKey, tunnelList);
            this.bandWidth = demandBw;
            BandWidthMng.getInstance().recoverPathBw(portKey, holdPriority, demandBw, unifyKey);
        }

        if (!tunnelList.contains(tunnelId)) {
            tunnelList.add(tunnelId);
        }
    }

    public List<TunnelUnifyKey> getMembersOnPort(PortKey portKey) {
        return portMap.get(portKey);
    }

    public long getMonopolizeBandWidth(PortKey portKey, TunnelUnifyKey tunnel) {
        List<TunnelUnifyKey> tunnelListOnPort = portMap.get(portKey);
        if (tunnelListOnPort == null || tunnelListOnPort.isEmpty()) {
            return 0;
        }

        if (members.contains(tunnel) && tunnelListOnPort.contains(tunnel) && tunnelListOnPort.size() == 1) {
            return bandWidth;
        }
        return 0;
    }

    @Override
    public String toString() {
        String str = "";
        str += "unifyKey: " + unifyKey.toString();
        str += "bandWidth: " + bandWidth + "\n";
        str += "preemptPriority: " + preemptPriority + "\n";
        str += "holdPriority: " + holdPriority + "\n";
        str += "members: num-" + members.size() + "\n";
        for (TunnelUnifyKey tunnel : members) {
            str += tunnel.toString();
        }

        for (Map.Entry<PortKey, List<TunnelUnifyKey>> entry : portMap.entrySet()) {
            str += "--------------------" + "\n";
            str += "Port:" + entry.getKey().getNode().toString() + entry.getKey().getTp().toString() + "\n";
            for (TunnelUnifyKey tunnel : entry.getValue()) {
                str += "tunnel: " + tunnel.toString();
            }
        }

        return str;
    }

    /**
     * set port map for bwshareGroup.
     *
     * @param portMap portMap of bwshareGroup
     */
    public void setPortMap(Map<PortKey, List<TunnelUnifyKey>> portMap) {
        this.portMap = portMap;
    }

    /**
     * set members for bwshareGroup.
     *
     * @param members members of bwshareGroup
     */
    public void setMembers(List<TunnelUnifyKey> members) {
        if (members instanceof Vector) {
            this.members = (Vector) members;
        }
    }
}