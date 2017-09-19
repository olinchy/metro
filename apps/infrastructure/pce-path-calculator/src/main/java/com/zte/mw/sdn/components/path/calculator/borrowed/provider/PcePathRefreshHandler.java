/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.uci.ics.jung.graph.Graph;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.DiffServBw;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelsRecordPerPort;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.ITunnel;

import com.zte.ngip.ipsdn.pce.path.api.RefreshTarget;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

/**
 * Created by 10204924 on 2017/8/22.
 */
public class PcePathRefreshHandler {
    PcePathRefreshHandler(PcePathHolder pathHolder) {
        this.pathHolder = pathHolder;
    }

    private static final Logger LOG = LoggerFactory.getLogger(PcePathRefreshHandler.class);
    private PcePathHolder pathHolder;

    public void refreshTunnels(
            boolean isSimulate, List<TunnelUnifyKey> migrateTunnels,
            Set<RefreshTarget> refreshTargets, TopologyId topoId) {
        if (topoId == null) {
            Logs.error(LOG, "topoId is null!");
            return;
        }
        Logs.info(LOG, "refreshTunnels targets {}", refreshTargets);
        if (refreshTargets.contains(RefreshTarget.ALL_PATHS)) {
            refreshAllTunnels(isSimulate, topoId);
            return;
        }
        final Set<ITunnel> needRefreshPath =
                getTunnelsNeedRefreshPath(isSimulate, migrateTunnels, refreshTargets, null);
        final Set<ITunnel> needRefreshSegments = getTunnelsNeedRefreshSegments(isSimulate, refreshTargets);

        needRefreshSegments.stream().filter(tunnel -> !needRefreshPath.contains(tunnel))
                .filter(tunnel -> tunnel.getTopoId().equals(topoId))
                .forEach(ITunnel::refreshSegments);
        refreshPathsOnDemand(needRefreshPath, tunnel -> tunnel.getTopoId().equals(topoId), ITunnel::refreshPath, true);

        if (refreshTargets.contains(RefreshTarget.UNESTABLISHED_AND_SRLG_PATHS)) {
            Logs.info(LOG, "refresh UNESTABLISHED_AND_SRLG_PATHS");
            refreshUnestablishAndSrlgTunnels(isSimulate, topoId, null, DiffServBw.HIGHEST_PRIORITY);
        } else if (refreshTargets.contains(RefreshTarget.UNESTABLISHED_PATHS)) {
            Logs.info(LOG, "refresh UNESTABLISHED_PATHS");
            refreshUnestablishTunnels(isSimulate, topoId, null, DiffServBw.HIGHEST_PRIORITY, null);
        } else if (refreshTargets.contains(RefreshTarget.DELAY_STRATEGY_UNESTABLISHED_PATHS)) {
            Logs.info(LOG, "refresh DELAY_STRATEGY_UNESTABLISHED_PATHS");
            Predicate<ITunnel> isDelayStrategy = ITunnel::isDelayStrategy;
            refreshUnestablishTunnels(isSimulate, topoId, null, DiffServBw.HIGHEST_PRIORITY, isDelayStrategy);
        }
    }

    void refreshAllTunnels(boolean isSimulate, TopologyId topoId) {
        boolean result = pathHolder.getAllTunnels(isSimulate).stream().filter(tunnel -> topoId != null)
                .filter(tunnel -> topoId.equals(tunnel.getTopoId())).sorted(new TunnelHoldPriorityComparator())
                .map(ITunnel::refreshPath).anyMatch(PceResult::isNeedRefreshUnestablishTunnels);
        if (result) {
            refreshUnestablishTunnels(isSimulate, topoId, null, DiffServBw.HIGHEST_PRIORITY, null);
        }
    }

    void refreshUnestablishAndSrlgTunnels(
            boolean isSimulate, TopologyId topoId, TunnelUnifyKey sourceTunnel,
            byte sourcePriority) {
        refreshUnestablished(isSimulate, topoId, sourceTunnel, sourcePriority, ITunnel::refreshUnestablishAndSrlgPath,
                             null);
    }

    void refreshUnestablishTunnels(
            boolean isSimulate, TopologyId topoId, TunnelUnifyKey sourceTunnel,
            byte sourcePriority, Predicate<ITunnel> otherRestriction) {
        refreshUnestablished(isSimulate, topoId, sourceTunnel, sourcePriority, ITunnel::refreshUnestablishPath,
                             otherRestriction);
    }

    private void refreshUnestablished(
            boolean isSimulate, TopologyId topoId, TunnelUnifyKey sourceTunnel,
            byte sourcePriority, Function<ITunnel, PceResult> refreshFunction, Predicate<ITunnel> otherRestriction) {

        Predicate<ITunnel> needRefresh = tunnel -> topoId != null;
        needRefresh = needRefresh.and(tunnel -> topoId.equals(tunnel.getTopoId()));
        needRefresh = needRefresh.and(tunnel -> !tunnel.getTunnelUnifyKey().equals(sourceTunnel));
        needRefresh = needRefresh.and(tunnel -> tunnel.getHoldPriority() >= sourcePriority);
        if (otherRestriction != null) {
            needRefresh = needRefresh.and(otherRestriction);
        }
        List<ITunnel> tunnels = pathHolder.getAllTunnels(
                java.util.Optional.ofNullable(sourceTunnel).map(TunnelUnifyKey::isSimulate).orElse(isSimulate));

        refreshPathsOnDemand(tunnels, needRefresh, refreshFunction, true);
    }

    private Set<ITunnel> getTunnelsNeedRefreshSegments(boolean isSimulate, Set<RefreshTarget> refreshTargets) {
        if (refreshTargets.contains(RefreshTarget.ALL_SEGMENTS)) {
            return pathHolder.getAllTunnels(isSimulate).stream().filter(ITunnel::isSrTunnel)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    void refreshTunnelsOnLink(boolean isSimulate, Link link, Graph<NodeId, Link> graph, Set<RefreshTarget> targets) {
        if (null == link) {
            LOG.error("refreshTunnelsOnLink, link is null!");
            return;
        }
        final Set<ITunnel> needRefreshSegments =
                pathHolder.getTunnelsAffectedByLinksNeedRefreshSegments(isSimulate, Lists.newArrayList(link), targets);
        final Set<ITunnel> needRefreshPath = getTunnelsOnLinkNeedRefresh(isSimulate, link, graph, targets);

        needRefreshSegments.parallelStream().filter(tunnel -> !needRefreshPath.contains(tunnel))
                .forEach(ITunnel::refreshSegments);

        if (CollectionUtils.isNullOrEmpty(needRefreshPath)) {
            LOG.debug("Need not refreshTunnelsOnLink, no tunnel on link," + link.toString());
            return;
        }

        LOG.info("refresh tunnels on link ", link);
        refreshPathsOnDemand(needRefreshPath, tunnel -> true, ITunnel::refreshPath, true);
    }

    private Set<ITunnel> getTunnelsOnLinkNeedRefresh(
            boolean isSimulate, Link link, Graph<NodeId, Link> graph,
            Set<RefreshTarget> refreshTargets) {
        if (graph == null) {
            return Collections.emptySet();
        }
        final Stream<ITunnel> tunnelsOnLink =
                TunnelsRecordPerPort.getInstance().getTunnelKeyOnLink(isSimulate, link, graph).parallelStream()
                        .map(pathHolder::getTunnelInstance).filter(Objects::nonNull);
        if (CollectionUtils.isNullOrEmpty(refreshTargets) || refreshTargets.contains(RefreshTarget.ALL_PATHS)) {
            return tunnelsOnLink.collect(Collectors.toSet());
        } else {
            return getTunnelsNeedRefreshPath(isSimulate, Collections.emptyList(), refreshTargets,
                                             tunnelsOnLink.collect(Collectors.toList()));
        }
    }

    private void refreshPathsOnDemand(
            Collection<ITunnel> tunnelList, Predicate<ITunnel> predicate,
            Function<? super ITunnel, PceResult> refreshFunction, boolean needPreempt) {
        Logs.debug(LOG, "refreshPathsOnDemand");
        tunnelList.stream().filter(predicate).sorted(new TunnelHoldPriorityComparator())
                .peek(tunnel -> Logs.info(LOG, "{}", tunnel.getTunnelUnifyKey()))
                .map(refreshFunction).filter(pceResult1 -> needPreempt)
                .reduce(new PceResult(), (pceResult, pceResult2) -> {
                    pceResult.merge(pceResult2);
                    return pceResult;
                }).preemptedTunnelsProcess(true);
    }

    private Set<ITunnel> getTunnelsNeedRefreshPath(
            boolean isSimulate, List<TunnelUnifyKey> migrateTunnels,
            Set<RefreshTarget> refreshTargets, List<ITunnel> specificTunnels) {
        final Set<ITunnel> tunnelsNeedRefresh = new HashSet<>();
        Collection<ITunnel> candidates = new HashSet<>(
                java.util.Optional.ofNullable(specificTunnels).orElseGet(() -> pathHolder.getAllTunnels(isSimulate)));
        for (RefreshTarget refreshTarget : refreshTargets) {
            switch (refreshTarget) {
                case DELAY_RESTRICTED_PATHS:
                    tunnelsNeedRefresh
                            .addAll(candidates.parallelStream().filter(ITunnel::isDelayRestricted)
                                            .collect(Collectors.toList()));
                    break;
                case UNESTABLISHED_PATHS:
                    tunnelsNeedRefresh.addAll(migrateTunnels.parallelStream().map(pathHolder::getTunnelInstance)
                                                      .filter(Objects::nonNull).collect(Collectors.toList()));
                    break;
                case HSB_PATHS:
                    final Set<ITunnel> allHsb = new HashSet<>(pathHolder.getAllTunnelHsbPath(false));
                    final Collection<ITunnel> needRefreshHsb = specificTunnels == null ? allHsb : specificTunnels
                            .parallelStream().filter(allHsb::contains).collect(Collectors.toSet());
                    tunnelsNeedRefresh.addAll(needRefreshHsb);
                    break;
                case SRLG_PATHS:
                    tunnelsNeedRefresh.addAll(pathHolder.getAllTunnelHsbPath(false).parallelStream()
                                                      .filter(ITunnel::isSrlgOverlap).collect(Collectors.toList()));
                    break;
                case FREED_PATHS:
                    tunnelsNeedRefresh.addAll(migrateTunnels.parallelStream().map(pathHolder::getTunnelInstance)
                                                      .filter(Objects::nonNull).collect(Collectors.toList()));
                    break;
                case METRIC_STRATEGY_DELAY_INELIGIBLE_PATHS:
                    final Predicate<ITunnel> withMetricStrategy = ITunnel::isMetricStrategy;
                    final Predicate<ITunnel> isDelayEligible = ITunnel::isDelayEligible;
                    tunnelsNeedRefresh.addAll(candidates.parallelStream()
                                                      .filter(isDelayEligible.negate().and(withMetricStrategy)).collect(
                                    Collectors.toList()));
                    break;
                case ZERO_BANDWIDTH_PATHS:
                    tunnelsNeedRefresh.addAll(candidates.parallelStream().filter(ITunnel::isChangeToZeroBandWidth)
                                                      .collect(Collectors.toList()));
                    break;
                default:
                    break;
            }
        }
        return tunnelsNeedRefresh;
    }

    void refreshTunnelPath(TunnelUnifyKey tunnelPathKey, TopologyId topoId) {
        java.util.Optional.ofNullable(pathHolder.getTunnelInstance(tunnelPathKey))
                .filter(tunnel -> null == topoId || topoId.equals(tunnel.getTopoId()))
                .ifPresent(tunnel -> tunnel.refreshPath(tunnelPathKey));
    }

    private class TunnelHoldPriorityComparator implements Comparator<ITunnel> {
        @Override
        public int compare(ITunnel tunnel1, ITunnel tunnel2) {

            //priority from high to low
            if (tunnel1.getHoldPriority() > tunnel2.getHoldPriority()) {
                return 1;
            } else if (tunnel1.getHoldPriority() < tunnel2.getHoldPriority()) {
                return -1;
            }
            return 0;
        }
    }
}
