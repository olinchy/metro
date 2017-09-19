/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.srlg;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.PathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelUnifyRecordKey;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelsRecordPerPort;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;

import com.zte.ngip.ipsdn.pce.path.api.srlg.AovidLinks;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.transformer.MetricTransformer;

/**
 * Created by 10088483 on 2016/7/13.
 */
public class PriorityAvoidLinks {
    private static final Logger LOG = LoggerFactory.getLogger(PriorityAvoidLinks.class);
    private List<AovidLinks> avoidLinksInPriority = Lists.newArrayList();

    public void addAvoidLinks(AovidLinks aovidLinks) {
        avoidLinksInPriority.add(aovidLinks);
    }

    public ListenableFuture<PceResult> calcPathByAvoidPriorityAsync(
            PathProvider<MetricTransformer> pathProvider,
            TunnelUnifyKey pathKey, List<Link> oldPath) {
        return Futures.immediateFuture(calcPathByAvoidPriority(pathProvider, pathKey, oldPath));
    }

    public PceResult calcPathByAvoidPriority(
            PathProvider<MetricTransformer> pathProvider, TunnelUnifyKey pathKey,
            List<Link> oldPath) {
        PceResult pceResult = null;
        for (int i = 0; i < getCalcPathCount(); i++) {
            if (0 != i) {
                TunnelsRecordPerPort.getInstance().update(new TunnelUnifyRecordKey(pathKey), null, oldPath);
            }
            pathProvider.clearTryToAvoidLinks();
            pathProvider.setNeedBandScaled(false);
            setPathProvider(pathProvider, i);

            Logs.info(LOG, "begin time {} AvoidPriority path compute {}", i, pathKey);
            try {
                pceResult = pathProvider.calcPathAsync().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("calcPathByAvoidPriority exception {}", e);
            }
            if (pceResult != null && !pceResult.isCalcFail()) {
                Logs.info(LOG, "end time {} AvoidPriority path compute {}", i, pathKey);
                break;
            }
        }
        return pceResult == null ? new PceResult() : pceResult;
    }

    private int getCalcPathCount() {
        return avoidLinksInPriority.size();
    }

    private void setPathProvider(PathProvider<MetricTransformer> pathProvider, int times) {
        pathProvider.addAvoidPath(avoidLinksInPriority.stream().limit((long) times + 1).collect(Collectors.toList()));
        pathProvider.setExcludePath(null);
        avoidLinksInPriority.stream().skip((long) times + 1).limit((long) getCalcPathCount()).map(AovidLinks::getLinks)
                .forEach(pathProvider::setExcludePath);
        pathProvider.setFailRollback(!((times + 1) == getCalcPathCount()));
    }
}
