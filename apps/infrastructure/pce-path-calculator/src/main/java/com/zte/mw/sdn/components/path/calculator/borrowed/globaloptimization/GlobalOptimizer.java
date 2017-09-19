/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.globaloptimization;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.AdjustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;

import com.zte.ngip.ipsdn.pce.path.api.RefreshTarget;
import com.zte.ngip.ipsdn.pce.path.api.thread.PceUniThreadPool;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;

/**
 * Created by 10204924 on 2017/7/18.
 */
public class GlobalOptimizer {
    public GlobalOptimizer(PcePathProvider provider) {
        this.provider = Preconditions.checkNotNull(provider);
    }

    private static final Logger LOG = LoggerFactory.getLogger(GlobalOptimizer.class);
    private static ExecutorService optimizeExecutor =
            Executors.newCachedThreadPool(new PceUniThreadPool("pce-thread-").generateThreadFactor("optimize"));
    private PcePathProvider provider;

    /**
     * Global optimizeAsync.
     *
     * @param strategy strategy
     */
    public void optimizeAsync(AdjustStrategy strategy) {
        CompletableFuture.runAsync(() -> optimize(strategy), optimizeExecutor);
    }

    private void optimize(AdjustStrategy strategy) {
        Logs.info(LOG, "Start optimize...");
        switch (strategy) {
            case MinimunBandwidthOccupation:
                // not support yet
                break;
            case DegradationLspRetry:
                doDegradationLspRetry();
                break;
            default:
                break;
        }
        Logs.info(LOG, "Finish optimize.");
    }

    // retry to calc a new path for these tunnels: down, srlg conflicted, hsb overlapped
    private void doDegradationLspRetry() {
        Logs.info(LOG, "doDegradationLspRetry");
        final Set<RefreshTarget> refreshTargets = new HashSet<>();
        refreshTargets.add(RefreshTarget.UNESTABLISHED_AND_SRLG_PATHS);

        provider.refreshTunnels(false, Collections.emptyList(), refreshTargets,
                                TopologyId.getDefaultInstance(ComUtility.DEFAULT_TOPO_ID_STRING));
    }
}
