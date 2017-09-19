/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceBandWidthConsumer;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.PortKey;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

public class BandWidthMng {
    private BandWidthMng() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(BandWidthMng.class);
    private static final byte DEFAULT_BANDWIDTH_UTILIZATION = 100;
    private static BandWidthMng instance = new BandWidthMng();
    private byte bandwidthUtilization = DEFAULT_BANDWIDTH_UTILIZATION;
    private ExecutorService executor = Executors.newSingleThreadExecutor(PceUtil.getThreadFactory("BandWidthMng"));
    private ExecutorService executorSimuPath =
            Executors.newSingleThreadExecutor(PceUtil.getThreadFactory("BandWidthMngSimulate"));
    private Map<PortKey, DiffServBw> portMap = new ConcurrentHashMap<>();
    private Map<PortKey, DiffServBw> portSimuMap = new ConcurrentHashMap<>();

    public static BandWidthMng getInstance() {
        return instance;
    }

    private static Stream<TunnelUnifyKey> getTunnelKeyStream(TunnelUnifyKey tunnelUnifyKey, Link link) {
        if (tunnelUnifyKey.isBwSharedGroup()) {
            // 同一个带宽共享组下，所有隧道带宽要保持一致
            BwSharedGroup bwSharedGroup = BwSharedGroupMng.getInstance().getBwShareGroupByKey(tunnelUnifyKey);
            return bwSharedGroup.getTunnelOnPort(new PortKey(link)).stream();
        } else {
            return Stream.of(tunnelUnifyKey);
        }
    }

    public void setBandwidthUtilization(byte utilization) {
        bandwidthUtilization = utilization;
    }

    public synchronized void recoverAddPort(Link link, long maxBw) {
        long utilizableMaxBw = getUtilizableMaxBw(maxBw);
        PortKey key = new PortKey(link);
        DiffServBw bws = portMap.get(key);
        if (bws == null) {
            portMap.put(key, new DiffServBw(utilizableMaxBw));
        } else {
            //do nothing, because different topoid can have some same links.
        }
    }

    private long getUtilizableMaxBw(long maxBw) {
        return maxBw * bandwidthUtilization / 100;
    }

    public void recoverPathBw(
            List<Link> path, byte holdPriority, Long demandBw,
            TunnelUnifyKey tunnelId, boolean isBiDirect) {
        if ((path == null) || ((demandBw == null) || (demandBw == 0))) {
            return;
        }

        for (Link link : path) {
            LOG.debug("recoverPathBw:" + ComUtility.getLinkString(link));
            recoverPathBw(new PortKey(link), holdPriority, demandBw, tunnelId);
        }

        if (!isBiDirect) {
            return;
        }

        for (Link link : path) {
            recoverPathBw(ComUtility.getLinkDestPort(link), holdPriority, demandBw, tunnelId);
        }
    }

    public void recoverPathBw(PortKey portkey, byte holdPriority, Long demandBw, TunnelUnifyKey tunnelId) {
        if ((demandBw == null) || (demandBw == 0)) {
            return;
        }

        LOG.info("recoverPathBw:{" + portkey.getNode() + portkey.getTp() + "}!");

        DiffServBw bws = portMap.get(portkey);
        if (bws == null) {
            LOG.error("recoverPathBw:PortKey not exist{"
                              + portkey.getNode().toString()
                              + portkey.getTp().toString()
                              + "}!");
            return;
        }

        bws.recoverPathBw(holdPriority, tunnelId, demandBw);
    }

    public boolean hasEnoughBw(
            PortKey portkey, byte preemptPriority, byte holdPriority,
            List<TunnelKeyBw> tunnelKeyBws, List<TunnelKeyBw> deletedtunnelKeyBws, boolean isSimulate) {
        Map<PortKey, DiffServBw> diffServBwMap = isSimulate ? portSimuMap : portMap;
        DiffServBw bws = diffServBwMap.get(portkey);
        if (bws == null) {
            return false;
        }
        return bws.hasEnoughBw(preemptPriority, holdPriority, tunnelKeyBws, deletedtunnelKeyBws);
    }

    public boolean hasEnoughBw(
            Link link, byte preemptPriority, byte holdPriority, long demandBw,
            TunnelUnifyKey tunnelUnifyKey) {
        return hasEnoughBw(new PortKey(link), preemptPriority, holdPriority, demandBw, tunnelUnifyKey);
    }

    public boolean hasEnoughBw(
            PortKey portKey, byte preemptPriority, byte holdPriority, long demandBw,
            TunnelUnifyKey tunnelUnifyKey) {
        Map<PortKey, DiffServBw> diffServBwMap =
                (tunnelUnifyKey != null && tunnelUnifyKey.isSimulate()) ? portSimuMap : portMap;
        DiffServBw bws = diffServBwMap.get(portKey);
        if (bws == null) {
            return false;
        }

        return bws.hasEnoughBw(preemptPriority, holdPriority, demandBw, tunnelUnifyKey);
    }

    public boolean hasEnoughBw(
            Link link, byte preemptPriority, byte holdPriority, long demandBw,
            TunnelUnifyKey tunnelUnifyKey, boolean isBidirect) {
        if (isBidirect && (!hasEnoughBw(new PortKey(
                link.getDestination().getDestNode(),
                link.getDestination().getDestTp()), preemptPriority, holdPriority, demandBw, tunnelUnifyKey))) {
            return false;
        }
        return hasEnoughBw(new PortKey(link), preemptPriority, holdPriority, demandBw, tunnelUnifyKey);
    }

    public PceResult alloc(
            List<Link> path, byte preemptPriority, byte holdPriority, long demandBw,
            TunnelUnifyKey tunnelId, boolean isBiDirect) throws BandwidthAllocException {
        PceResult result = new PceResult();

        if (path == null) {
            return null;
        }

        for (Link link : path) {
            PceResult singleLinkResult = alloc(new PortKey(link), preemptPriority, holdPriority,
                                               demandBw, tunnelId);
            if (singleLinkResult != null) {
                result.merge(singleLinkResult);
            }
        }

        if (!isBiDirect) {
            return result;
        }

        for (Link link : path) {
            PceResult singleLinkResult = alloc(ComUtility.getLinkDestPort(link),
                                               preemptPriority, holdPriority, demandBw, tunnelId);
            if (singleLinkResult != null) {
                result.merge(singleLinkResult);
            }
        }

        return result;
    }

    public PceResult alloc(
            PortKey portKey, byte premmptPriority, byte holdPriority, long demandBw,
            TunnelUnifyKey tunnelId) throws BandwidthAllocException {
        Map<PortKey, DiffServBw> diffServBwMap = tunnelId.isSimulate() ? portSimuMap : portMap;
        DiffServBw bws = diffServBwMap.get(portKey);
        if (bws == null) {
            throw new BandwidthAllocException();
        }

        PceResult pceResult = bws.alloc(premmptPriority, holdPriority, tunnelId, demandBw);

        pceResult.dealBwSharedGroupTunnel(portKey, tunnelId.isSimulate());
        return pceResult;
    }

    public PceResult alloc(
            Link link, byte premmptPriority, byte holdPriority, long demandBw,
            TunnelUnifyKey tunnelId) throws BandwidthAllocException {
        return alloc(new PortKey(link), premmptPriority, holdPriority, demandBw, tunnelId);
    }

    public PceResult alloc(
            List<Link> path, byte preemptPriority, byte holdPriority,
            List<TunnelKeyBw> tunnelKeyList, TunnelUnifyKey establishTunnel,
            boolean isBiDirect) {
        PceResult result = new PceResult();

        if (path == null) {
            return null;
        }

        for (Link link : path) {
            PceResult singleLinkResult = alloc(new PortKey(link), preemptPriority, holdPriority,
                                               tunnelKeyList, establishTunnel);
            if (singleLinkResult != null) {
                result.merge(singleLinkResult);
            }
        }

        if (!isBiDirect) {
            return result;
        }

        for (Link link : path) {
            PceResult singleLinkResult = alloc(ComUtility.getLinkDestPort(link),
                                               preemptPriority, holdPriority, tunnelKeyList, establishTunnel);
            if (singleLinkResult != null) {
                result.merge(singleLinkResult);
            }
        }

        return result;
    }

    private PceResult alloc(
            PortKey port, byte preemptPriority, byte holdPriority, List<TunnelKeyBw> tunnelKeyList,
            TunnelUnifyKey establishTunnel) {
        Map<PortKey, DiffServBw> diffServBwMap = establishTunnel.isSimulate() ? portSimuMap : portMap;
        DiffServBw bws = diffServBwMap.get(port);
        if (bws == null) {
            LOG.error("bws is null! port:" + port.toString());
            return new PceResult();
        }

        PceResult pceResult = bws.alloc(preemptPriority, holdPriority, tunnelKeyList, establishTunnel);

        pceResult.dealBwSharedGroupTunnel(port, establishTunnel.isSimulate());
        return pceResult;
    }

    public void free(
            List<Link> links, byte holdPriority, TunnelUnifyKey tunnelId, PceResult result,
            boolean isBiDirect) {
        if (links == null) {
            return;
        }

        for (Link link : links) {
            free(link, holdPriority, tunnelId, result, isBiDirect);
        }
    }

    public void free(
            Link link, byte holdPriority, TunnelUnifyKey tunnelId, PceResult result,
            boolean isBiDirect) {
        free(new PortKey(link), holdPriority, tunnelId, result);
        if (isBiDirect) {
            free(ComUtility.getLinkDestPort(link), holdPriority, tunnelId, result);
        }
    }

    public void free(PortKey portKey, byte holdPriority, TunnelUnifyKey tunnelId, PceResult result) {
        Map<PortKey, DiffServBw> diffServBwMap = tunnelId.isSimulate() ? portSimuMap : portMap;
        DiffServBw bws = diffServBwMap.get(portKey);

        if (bws == null) {
            return;
        }

        result.enableBandwidthFreeFlag();
        bws.free(holdPriority, tunnelId);
    }

    public synchronized void free(List<Link> path, byte priority, TunnelUnifyKey tunnel, PceResult result) {
        if (path == null) {
            return;
        }

        for (Link link : path) {
            free(link, priority, tunnel, result);
        }
    }

    public void free(Link link, byte holdPriority, TunnelUnifyKey tunnelId, PceResult result) {
        free(new PortKey(link), holdPriority, tunnelId, result);
    }

    public synchronized void addPort(Link link, long maxBw) {
        updatePort(link, maxBw);
    }

    public synchronized List<TunnelUnifyKey> updatePort(Link link, long maxBw) {
        return updatePort(false, link, maxBw);
    }

    public synchronized List<TunnelUnifyKey> updatePort(boolean isSimulate, Link link, long maxBw) {
        Map<PortKey, DiffServBw> diffServBwMap = isSimulate ? portSimuMap : portMap;
        List<TunnelUnifyKey> freeTunnels = new ArrayList<>();
        long utilizableMaxBw = getUtilizableMaxBw(maxBw);
        PortKey key = new PortKey(link);
        DiffServBw bws = diffServBwMap.get(key);
        if (bws == null) {
            diffServBwMap.put(key, new DiffServBw(utilizableMaxBw));
            return freeTunnels;
        } else {
            return bws.updateBandwidth(utilizableMaxBw).stream()
                    .flatMap(tunnelUnifyKey -> getTunnelKeyStream(tunnelUnifyKey, link)).collect(Collectors.toList());
        }
    }

    public synchronized void addPort(boolean isSimulate, Link link, long maxBw) {
        updatePort(isSimulate, link, maxBw);
    }

    public synchronized void delPort(Link link) {
        portMap.remove(new PortKey(link));
    }

    public synchronized void delPort(boolean isSimulate, Link link) {
        Map<PortKey, DiffServBw> diffServBwMap = isSimulate ? portSimuMap : portMap;
        diffServBwMap.remove(new PortKey(link));
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    public <T> Future<T> submitSimuExcute(Callable<T> task) {
        LOG.info("add simulate task");
        return executorSimuPath.submit(task);
    }

    public long queryReservedBw(Link link, byte priority) {
        DiffServBw bws = portMap.get(new PortKey(link));
        if (bws == null) {
            return 0;
        }

        return bws.queryReservedBw(priority);
    }

    /**
     * query simulate map bandwidth.
     *
     * @param link     link info
     * @param priority priority
     * @return bandwidth
     */
    public long queryReservedSimulateBw(Link link, byte priority) {
        DiffServBw bws = portSimuMap.get(new PortKey(link));
        if (bws == null) {
            return 0;
        }

        return bws.queryReservedBw(priority);
    }

    public long queryLinkReservedBw(Link link) {
        DiffServBw bws = portMap.get(new PortKey(link));
        if (bws == null) {
            return 0;
        }

        return bws.getReserveBandwidth() + getUnusableBw(bws.getBandwidth());
    }

    private long getUnusableBw(long utilizableMaxBw) {
        long maxBw = utilizableMaxBw * 100 / bandwidthUtilization;
        return maxBw - utilizableMaxBw;
    }

    public long queryLinkReservedBw(boolean isSimulate, Link link) {
        Map<PortKey, DiffServBw> diffServBwMap = isSimulate ? portSimuMap : portMap;
        DiffServBw bws = diffServBwMap.get(new PortKey(link));
        if (bws == null) {
            return 0;
        }

        return bws.getReserveBandwidth() + getUnusableBw(bws.getBandwidth());
    }

    public long queryMaxBw(Link link, byte priority) {
        DiffServBw bws = portMap.get(new PortKey(link));
        if (bws == null) {
            return 0;
        }

        return bws.queryMaxBw(priority);
    }

    public List<TunnelUnifyKey> getAllTunnelThroughThePort(Link link) {
        DiffServBw bws = portMap.get(new PortKey(link));
        if (bws == null) {
            return Lists.newArrayList();
        }
        return bws.getAllTunnelThrough();
    }

    /**
     * getAllSimulateTunnelThroughThePort.
     *
     * @param link link
     * @return tunnel keys
     */
    public List<TunnelUnifyKey> getAllSimulateTunnelThroughThePort(Link link) {
        DiffServBw bws = portSimuMap.get(new PortKey(link));
        if (bws == null) {
            return Lists.newArrayList();
        }
        return bws.getAllTunnelThrough();
    }

    public long queryTunnelOccupyBwInLink(TunnelUnifyKey tunnel, Link link) {
        DiffServBw bws = portMap.get(new PortKey(link));
        if (bws == null) {
            return 0;
        }
        return bws.queryBwOccupiedByTunnel(tunnel);
    }

    public boolean isTunnelOccupyLink(TunnelUnifyKey tunnel, Link link) {
        DiffServBw bws = portMap.get(new PortKey(link));
        if (bws == null) {
            return false;
        }

        if (!bws.isOccupiedByTunnel(tunnel)) {
            return false;
        }
        return true;
    }

    public boolean isTunnelOccupyLink(TunnelUnifyKey tunnel, PortKey portKey) {
        DiffServBw bws = portMap.get(portKey);
        if (bws == null) {
            return false;
        }

        if (!bws.isOccupiedByTunnel(tunnel)) {
            return false;
        }
        return true;
    }

    public synchronized PceResult increasePathBw(
            List<Link> path, long newBw, byte preemptPriority, byte holdPriority,
            TunnelUnifyKey tunnelKey, boolean isBiDirect) {
        PceResult positiveResult = new PceResult();

        for (Link link : path) {
            positiveResult = increasePathBw(new PortKey(link), newBw, preemptPriority, holdPriority, tunnelKey);
        }

        if (!isBiDirect) {
            return positiveResult;
        }
        PceResult reverseResult = new PceResult();

        for (Link link : path) {
            reverseResult =
                    increasePathBw(ComUtility.getLinkDestPort(link), newBw, preemptPriority, holdPriority, tunnelKey);
        }
        reverseResult.merge(positiveResult);
        return reverseResult;
    }

    private PceResult increasePathBw(
            PortKey portKey, long newBw, byte preemptPriority, byte holdPriority,
            TunnelUnifyKey tunnelKey) {
        PceResult pceResult = new PceResult();
        Map<PortKey, DiffServBw> diffServBwMap = tunnelKey.isSimulate() ? portSimuMap : portMap;
        DiffServBw bws = diffServBwMap.get(portKey);
        if (bws == null) {
            LOG.error("bws null!" + portKey.toString());
            return pceResult;
        }
        try {
            pceResult = alloc(portKey, preemptPriority, holdPriority, newBw, tunnelKey);
        } catch (BandwidthAllocException e) {
            Logs.info(LOG, "increasePathBw face error {}", e);
        }
        return pceResult;
    }

    public synchronized PceResult decreasePathBw(
            List<Link> path, long newBw, byte holdPriority,
            TunnelUnifyKey tunnelKey, boolean isBiDirect) {
        PceResult pceResult = new PceResult();
        pceResult.enableBandwidthFreeFlag();
        for (Link link : path) {
            decreasePathBw(new PortKey(link), newBw, holdPriority, tunnelKey);
        }

        if (!isBiDirect) {
            return pceResult;
        }

        for (Link link : path) {
            decreasePathBw(ComUtility.getLinkDestPort(link), newBw, holdPriority, tunnelKey);
        }
        return pceResult;
    }

    public void decreasePathBw(PortKey portKey, long newBw, byte holdPriority, TunnelUnifyKey tunnelKey) {
        if (portKey == null) {
            return;
        }
        Map<PortKey, DiffServBw> diffServBwMap = tunnelKey.isSimulate() ? portSimuMap : portMap;
        DiffServBw bws = diffServBwMap.get(portKey);
        if (bws == null) {
            LOG.error("bws null!" + portKey.toString());
            return;
        }

        bws.decreaseBw(holdPriority, tunnelKey, newBw);
    }

    public void decreasePathBw(
            List<Link> path, long newBw, byte holdPriority,
            TunnelUnifyKey tunnelKey) throws BandwidthAllocException {
        if (path == null) {
            return;
        }

        for (Link link : path) {
            decreasePathBw(new PortKey(link), newBw, holdPriority, tunnelKey);
        }
    }

    //for passive hsb tunnel, who don't support bidirect
    //support bidirect 2016.09.26
    public void updateUnifyKey(
            List<Link> path,
            List<Link> reversePath,
            TunnelUnifyKey tunnelIdOld,
            TunnelUnifyKey tunnelIdNew,
            byte holdPriority) {
        for (Link link : path) {
            updateUnifyKey(new PortKey(link), tunnelIdOld, tunnelIdNew, holdPriority);
        }
        if (reversePath != null) {
            for (Link link : reversePath) {
                updateUnifyKey(new PortKey(link), tunnelIdOld, tunnelIdNew, holdPriority);
            }
        }
    }

    private void updateUnifyKey(
            PortKey portKey, TunnelUnifyKey tunnelIdOld, TunnelUnifyKey tunnelIdNew,
            byte holdPriority) {
        Map<PortKey, DiffServBw> diffServBwMap = tunnelIdOld.isSimulate() ? portSimuMap : portMap;
        DiffServBw bws = diffServBwMap.get(portKey);
        if (bws == null) {
            return;
        }

        bws.updateUnifyKey(tunnelIdOld, tunnelIdNew, holdPriority);
    }

    public void destroy() {
        portMap.clear();
        bandwidthUtilization = DEFAULT_BANDWIDTH_UTILIZATION;
    }

    public String getBandWidthString() {
        String rtnString = "";

        rtnString += "bandwidthUtilization:" + bandwidthUtilization + "\n";
        for (Map.Entry<PortKey, DiffServBw> entry : portMap.entrySet()) {
            rtnString += "\nPort:" + entry.getKey().getNode() + entry.getKey().getTp() + "\n";
            rtnString += entry.getValue();
        }
        return rtnString;
    }

    public String getSimulateBandWidthString() {
        String rtnString = "";

        rtnString += "bandwidthUtilization:" + bandwidthUtilization + "\n";
        for (Map.Entry<PortKey, DiffServBw> entry : portSimuMap.entrySet()) {
            rtnString += "Port:" + entry.getKey().getNode() + entry.getKey().getTp() + "\n";
            rtnString += entry.getValue();
        }
        return rtnString;
    }

    public void printPortBandWidth(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId nodeId,
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId tpId) {
        PortKey portKey = new PortKey(nodeId, tpId);
        DiffServBw bws = portMap.get(portKey);
        if (null != bws) {
            ComUtility.debugInfoLog(bws.toString());
        }
    }

    public long getTunnelReserveBandWidth(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId nodeId,
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId tpId,
            byte priority) {
        PortKey portKey = new PortKey(nodeId, tpId);
        DiffServBw bws = portMap.get(portKey);
        if (null != bws) {
            return bws.getPriorityBandwidth(priority);
        }
        return 0;
    }

    /**
     * copy all port map for calc offline tunnels.
     */
    public void copyPortMapMirror() {
        portMap.forEach((key, value) ->
                                portSimuMap.put(new PortKey(key.getNode(), key.getTp()), new DiffServBw(value)));
        LOG.info("Port map Mirror generator {}", portSimuMap);
    }

    /**
     * destroy all port map for finish calc offline tunnels.
     */
    public void destroyPortMapMirror() {
        portSimuMap.clear();
        LOG.info("Port map Mirror destroy {}", portSimuMap);
    }

    public long bwNeedForTunnel(
            Link link, byte preemptPriority, byte holdPriority, List<TunnelKeyBw> tunnelKeyBws,
            List<TunnelKeyBw> deletedtunnelKeyBws, boolean isSimulate) {
        return bwNeeded(new PortKey(link), preemptPriority, holdPriority, tunnelKeyBws, deletedtunnelKeyBws,
                        isSimulate);
    }

    private long bwNeeded(
            PortKey portkey, byte preemptPriority, byte holdPriority, List<TunnelKeyBw> tunnelKeyBws,
            List<TunnelKeyBw> deletedtunnelKeyBws, boolean isSimulate) {
        Map<PortKey, DiffServBw> diffServBwMap = isSimulate ? portSimuMap : portMap;
        DiffServBw bws = diffServBwMap.get(portkey);
        if (bws == null) {
            return 0;
        }
        return bws.bwNeeded(preemptPriority, holdPriority, tunnelKeyBws, deletedtunnelKeyBws);
    }

    public long bwNeedForTunnel(
            Link link, byte preemptPriority, byte holdPriority, long demandBw,
            TunnelUnifyKey tunnelUnifyKey) {
        return bwNeeded(new PortKey(link), preemptPriority, holdPriority, demandBw, tunnelUnifyKey);
    }

    public long bwNeeded(
            PortKey portKey, byte preemptPriority, byte holdPriority, long demandBw,
            TunnelUnifyKey tunnelUnifyKey) {
        Map<PortKey, DiffServBw> diffServBwMap =
                (tunnelUnifyKey != null && tunnelUnifyKey.isSimulate()) ? portSimuMap : portMap;
        DiffServBw bws = diffServBwMap.get(portKey);
        if (bws == null) {
            return 0;
        }

        return bws.bwNeeded(preemptPriority, holdPriority, demandBw, tunnelUnifyKey);
    }

    public synchronized void dealBandWidth(PceBandWidthConsumer fun) throws BandwidthAllocException {
        fun.accept();
    }
}
