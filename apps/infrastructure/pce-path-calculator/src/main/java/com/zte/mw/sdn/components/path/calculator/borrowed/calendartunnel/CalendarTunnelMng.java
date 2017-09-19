/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.calendartunnel;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroupMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelsRecordPerPort;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.topology.MaintenanceTopologyMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.topology.TunnelsRecordPerTopology;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelgrouppath.TunnelGroupPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelhsbpath.TunnelHsbPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathInstance;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.TunnelPathKey;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

public class CalendarTunnelMng {
    private CalendarTunnelMng() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(CalendarTunnelMng.class);
    private static CalendarTunnelMng instance = new CalendarTunnelMng();
    private ExecutorService executor = Executors.newSingleThreadExecutor(PceUtil.getThreadFactory("CalendarTunnelMng"));
    private ConcurrentHashMap<TunnelPathKey, TunnelPathInstance> calendarTunnelPaths = new ConcurrentHashMap<>();
    private ConcurrentHashMap<TunnelPathKey, TunnelHsbPathInstance> calendarTunnelHsbPaths = new ConcurrentHashMap<>();
    private PcePathProvider pcePathProvider;

    public static CalendarTunnelMng getInstance() {
        return instance;
    }

    /**
     * destroy all resource in calc tunnels.
     */
    private static void destroyMirrorResource() {
        BandWidthMng.getInstance().destroyPortMapMirror();
        BwSharedGroupMng.getInstance().destroyBwSharedGroupMapMirror();
        TunnelsRecordPerPort.getInstance().destroyPortSimuRecordMirror();
        TunnelsRecordPerTopology.getInstance().destroySimilateTopoRecord();
        MaintenanceTopologyMng.getInstance().destroySimulateGlobalExcludingAddresses();
    }

    /**
     * set pcePathPrivate.
     *
     * @param pcePathProvider pcePathPrivate
     */
    public void setPcePathProvider(PcePathProvider pcePathProvider) {
        this.pcePathProvider = pcePathProvider;
    }

    /**
     * mirrorAllResource.
     */
    public void mirrorAllResource() {
        LOG.info("Start mirror all resource begin");

        try {
            executor.submit(() -> {
                destroyCalendarTunnelsMap();
                destroyMirrorResource();
                generatorCalendarTunnelsMap();
                mirrorResource();
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Faced error when create mirror resource {}", e);
        }
        LOG.info("Start mirror all resource over");
    }

    private void destroyCalendarTunnelsMap() {
        calendarTunnelPaths.clear();
        calendarTunnelHsbPaths.clear();
    }

    /**
     * generator all tunnels map in mirror.
     */
    private void generatorCalendarTunnelsMap() {
        pcePathProvider.getTunnelPaths().entrySet().forEach(
                entry -> calendarTunnelPaths.put(entry.getKey(), new TunnelPathInstance(entry.getValue(), true)));
        LOG.info("calendarTunnelPaths Mirror generator {}", calendarTunnelPaths);
        pcePathProvider.getTunnelHsbPaths().entrySet().forEach(
                entry -> calendarTunnelHsbPaths.put(entry.getKey(), new TunnelHsbPathInstance(entry.getValue(), true)));
        LOG.info("calendarTunnelHsbPaths Mirror generator {}", calendarTunnelHsbPaths);
    }

    /**
     * mirror all resource in calc tunnels.
     */
    public void mirrorResource() {
        BandWidthMng.getInstance().copyPortMapMirror();
        BwSharedGroupMng.getInstance().copyBwSharedGroupMappMirror();
        TunnelsRecordPerPort.getInstance().copyPortSimuRecordMirror();
        TunnelsRecordPerTopology.getInstance().copySimilateTopoRecord();
        MaintenanceTopologyMng.getInstance().copySimulateGlobalExcludingAddresses();
    }

    /**
     * clearAllMirrorResource.
     */
    public void clearAllMirrorResource() {
        LOG.info("Clear all mirror resource begin");
        try {
            executor.submit(() -> {
                destroyCalendarTunnelsMap();
                destroyMirrorResource();
                PceUtil.clearSimulateMap();
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Faced error when clear mirror resource {}", e);
        }
        LOG.info("Clear all mirror resource over");
    }

    public Collection<TunnelPathInstance> getAllTunnelInstance() {
        return calendarTunnelPaths.values();
    }

    public Collection<TunnelHsbPathInstance> getAllTunnelHsbInstance() {
        return calendarTunnelHsbPaths.values();
    }

    public Collection<TunnelGroupPathInstance> getAllTunnelGroupInstance() {
        return Collections.emptyList();
    }

    /**
     * write tunnel into memory.
     *
     * @param key  tunnel key
     * @param path tunnel instance
     */
    public void putTunnelInstance(TunnelPathKey key, TunnelPathInstance path) {
        calendarTunnelPaths.put(key, path);
    }

    /**
     * remove tunnel from memory.
     *
     * @param key tunnel key
     */
    public void removeTunnelInstance(TunnelPathKey key) {
        calendarTunnelPaths.remove(key);
    }

    /**
     * find tunnel in calendarTunnelHsbPaths.
     *
     * @param key tunnel key
     * @return TunnelHsbPathInstance
     */
    public TunnelHsbPathInstance getTunnelHsbInstance(TunnelPathKey key) {
        return calendarTunnelHsbPaths.get(key);
    }

    public TunnelHsbPathInstance getTunnelHsbInstance(NodeId headNode, int tunnelId) {
        return calendarTunnelHsbPaths.get(new TunnelPathKey(headNode, tunnelId));
    }

    /**
     * find tunnel in calendarTunnelPaths.
     *
     * @param key tunnel key
     * @return TunnelPathInstance
     */
    public TunnelPathInstance getTunnelInstance(TunnelPathKey key) {
        return calendarTunnelPaths.get(key);
    }

    public TunnelPathInstance getTunnelInstance(NodeId headNode, int tunnelId) {
        return calendarTunnelPaths.get(new TunnelPathKey(headNode, tunnelId));
    }

    /**
     * write tunnel into memory.
     *
     * @param key  tunnel key
     * @param path TunnelHsbPathInstance
     */
    public void putTunnelHsbInstance(TunnelPathKey key, TunnelHsbPathInstance path) {
        calendarTunnelHsbPaths.put(key, path);
    }

    /**
     * remove tunnel from memory.
     *
     * @param key tunnel key
     */
    public void removeTunnelHsbInstance(TunnelPathKey key) {
        calendarTunnelHsbPaths.remove(key);
    }
}
