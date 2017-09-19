/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.servicepath;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.uci.ics.jung.graph.Graph;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.ServiceChange;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.ServiceChangeBuilder;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.services.grouping.ServiceList;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.services.grouping.ServiceListBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandWidthMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.TunnelsRecordPerPort;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.NotificationProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathHolder;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.CommonTunnel;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.ITunnel;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.topochange.LinkChange;
import com.zte.ngip.ipsdn.pce.path.api.topochange.LinkUpdateType;
import com.zte.ngip.ipsdn.pce.path.api.util.LinkUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.PortKey;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;

/**
 * Created by 10204924 on 2017/8/21.
 */
public class ServiceOnTopoChangeHandler {
    public ServiceOnTopoChangeHandler(PcePathHolder pathHolder) {
        this.pathHolder = pathHolder;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServiceOnTopoChangeHandler.class);
    private static final Predicate<LinkChange> CREATE_TYPE =
            linkUpdate -> linkUpdate.getType() == LinkChange.Type.CREATE;
    private static final Predicate<LinkChange> REMOVE_TYPE =
            linkUpdate -> linkUpdate.getType() == LinkChange.Type.REMOVE;
    private static final Predicate<LinkChange> UPDATE_TYPE =
            linkUpdate -> linkUpdate.getType() == LinkChange.Type.UPDATE;
    private PcePathHolder pathHolder;

    private static Stream<ITunnel> getFilteredServices(List<ITunnel> allServices, Predicate<ITunnel> predicate) {
        return allServices.stream().filter(predicate);
    }

    /**
     * handleLinkChange.
     *
     * @param linkChangeList linkChangeList
     * @param graph          graph
     */
    public void handleLinkChange(List<LinkChange> linkChangeList, Graph<NodeId, Link> graph) {
        final List<ITunnel> allServices = pathHolder.getAllServices();

        PceUtil.printTunnelDebugInfo("handleLinkChange allServices ", LOG, allServices);
        final Set<ITunnel> tunnelsNeedRefresh = new HashSet<>();
        // create
        tunnelsNeedRefresh.addAll(handleLinkCreated(linkChangeList, allServices));
        // remove
        tunnelsNeedRefresh.addAll(handleLinkRemoved(linkChangeList, graph));
        // update
        tunnelsNeedRefresh.addAll(handleLinkUpdated(linkChangeList, graph, allServices));

        Logs.debug(LOG, "servicesNeedRefresh {}", tunnelsNeedRefresh);
        // build notification
        final Predicate<ITunnel> isService =
                itunnel -> itunnel instanceof ServicePathInstance || itunnel instanceof ServiceHsbPathInstance;
        List<ServiceList> serviceList =
                tunnelsNeedRefresh.stream().filter(isService).map(itunnel -> (CommonTunnel) itunnel)
                        .map(CommonTunnel::getServiceName).filter(Objects::nonNull)
                        .map(serviceName -> new ServiceListBuilder().setServiceName(serviceName).build())
                        .collect(Collectors.toList());
        if (serviceList.isEmpty()) {
            return;
        }
        ServiceChange notification = new ServiceChangeBuilder().setServiceList(serviceList).build();
        NotificationProvider.getInstance().notify(notification);
        Logs.info(LOG, "publish notification {}", notification);
    }

    private Set<ITunnel> handleLinkCreated(List<LinkChange> linkChangeList, List<ITunnel> allServices) {
        OptionalLong maxBwCreated = linkChangeList.parallelStream().filter(CREATE_TYPE).map(LinkChange::getCreatedLink)
                .filter(java.util.Optional::isPresent).map(java.util.Optional::get)
                .mapToLong(LinkUtils::getValidBandwidth).max();
        boolean hasCreated = maxBwCreated.isPresent();
        if (!hasCreated) {
            return Collections.emptySet();
        }
        Logs.debug(LOG, "handleLinkCreated maxBwCreated {}", maxBwCreated.getAsLong());
        return handleLinkCreatedOrBwIncrease(allServices, maxBwCreated.getAsLong()).collect(Collectors.toSet());
    }

    private Set<ITunnel> handleLinkRemoved(List<LinkChange> linkChangeList, Graph<NodeId, Link> graph) {
        // 有Link删除时，刷新经过这条Link的所有Service
        return linkChangeList.parallelStream().filter(REMOVE_TYPE).map(LinkChange::getRemovedLink)
                .filter(Optional::isPresent).map(Optional::get)
                .flatMap(link -> getServicesOnLinkNeedRefresh(link, graph, null))
                .collect(Collectors.toSet());
    }

    private Stream<ITunnel> getServicesOnLinkNeedRefresh(
            Link link, Graph<NodeId, Link> graph,
            Predicate<ITunnel> predicate) {
        List<ITunnel> servicesOnLink = TunnelsRecordPerPort.getInstance().getTunnelKeyOnLink(link, graph).stream()
                .map(tunnelUnifyKey -> pathHolder.getTunnelInstance(tunnelUnifyKey)).filter(Objects::nonNull)
                .collect(Collectors.toList());
        Logs.debug(LOG, "servicesOnLink {}", servicesOnLink.size());
        if (predicate == null) {
            return servicesOnLink.stream();
        }
        return getFilteredServices(servicesOnLink, predicate);
    }

    private Set<ITunnel> handleLinkUpdated(
            List<LinkChange> linkChangeList, Graph<NodeId, Link> graph,
            List<ITunnel> allServices) {
        return linkChangeList.stream().filter(UPDATE_TYPE)
                .flatMap(linkUpdate -> handleDomainLinkUpdate(linkUpdate, graph, allServices))
                .collect(Collectors.toSet());
    }

    private Stream<ITunnel> handleDomainLinkUpdate(
            LinkChange linkChange, Graph<NodeId, Link> graph,
            List<ITunnel> allServices) {
        final java.util.Optional<Link> oldLinkOpt = linkChange.getOriginalLink();
        final java.util.Optional<Link> newLinkOpt = linkChange.getUpdatedLink();
        if (!oldLinkOpt.isPresent() || !newLinkOpt.isPresent()) {
            Logs.error(LOG, "handleDomainLinkUpdate error: {} {}", oldLinkOpt, newLinkOpt);
            return Stream.empty();
        }
        final Set<ITunnel> tunnelsNeedRefresh = new HashSet<>();
        for (LinkUpdateType type : linkChange.getLinkUpdateTypeSet()) {
            switch (type) {
                case BANDWIDTH_INCREASE:
                    long oldBw = LinkUtils.getValidBandwidth(oldLinkOpt.get());
                    long newBw = LinkUtils.getValidBandwidth(newLinkOpt.get());
                    Logs.debug(LOG, "type is {}, oldBw is {}, newBw is {}", type, oldBw, newBw);
                    tunnelsNeedRefresh
                            .addAll(handleLinkCreatedOrBwIncrease(allServices, newBw).collect(Collectors.toSet()));
                    break;
                case BANDWIDTH_DECREASE:
                    oldBw = LinkUtils.getValidBandwidth(oldLinkOpt.get());
                    newBw = LinkUtils.getValidBandwidth(newLinkOpt.get());
                    Logs.debug(LOG, "type is {}, oldBw is {}, newBw is {}", type, oldBw, newBw);
                    tunnelsNeedRefresh.addAll(handleBandwidthDecrease(
                            oldLinkOpt.get(),
                            linkChange.getMigrateTunnels()));
                    break;
                case DELAY_INCREASE:
                    long oldDelay = LinkUtils.getValidLinkDelay(oldLinkOpt.get());
                    long newDelay = LinkUtils.getValidLinkDelay(newLinkOpt.get());
                    Logs.debug(LOG, "type is {}, oldDelay is {}, newDelay is {}", type, oldDelay, newDelay);
                    tunnelsNeedRefresh.addAll(handleDelayIncrease(oldLinkOpt.get(), graph));
                    break;
                case DELAY_DECREASE:
                    oldDelay = LinkUtils.getValidLinkDelay(oldLinkOpt.get());
                    newDelay = LinkUtils.getValidLinkDelay(newLinkOpt.get());
                    Logs.debug(LOG, "type is {}, oldDelay is {}, newDelay is {}", type, oldDelay, newDelay);
                    tunnelsNeedRefresh.addAll(handleDelayDecrease(allServices));
                    break;
                default:
                    break;
            }
        }
        return tunnelsNeedRefresh.stream();
    }

    private Stream<ITunnel> handleLinkCreatedOrBwIncrease(List<ITunnel> allServices, long newBw) {
        // 新增Link的带宽要满足隧道带宽
        final Predicate<ITunnel> bwSatisfied = tunnel -> tunnel.getTeArgumentBean().getBandWidth() <= newBw;
        // 有Link添加时，通知刷新 NoPath、Hsb overlap path、Zero bandwidth、DelayIneligible 这几种类型的Service
        final Predicate<ITunnel> noPath = ITunnel::isUnestablished;
        final Predicate<ITunnel> hsbOverlapPath = ITunnel::isPathOverlap;
        final Predicate<ITunnel> zeroBwPath = ITunnel::isChangeToZeroBandWidth;
        final Predicate<ITunnel> delayIneligible = tunnel -> !tunnel.isDelayEligible();
        Predicate<ITunnel> notSatisfactory = noPath.or(hsbOverlapPath).or(zeroBwPath).or(delayIneligible);
        return getFilteredServices(allServices, notSatisfactory.and(bwSatisfied));
    }

    private Set<ITunnel> handleBandwidthDecrease(Link oldLink, List<TunnelUnifyKey> migrateTunnels) {
        // 带宽变小时通知刷新被抢占的业务
        return migrateTunnels.stream().map(pathHolder::getTunnelInstance).filter(Objects::nonNull)
                .peek(positiveTunnel -> freeReversePortBwForMigrateTunnels(positiveTunnel, oldLink))
                .collect(Collectors.toSet());
    }

    // 如果因为带宽变小而被抢占的是双向信令隧道，把反向接口下占用的带宽也释放掉
    private void freeReversePortBwForMigrateTunnels(ITunnel positiveTunnel, Link oldLink) {
        if (!BiDirect.isBiDirect(positiveTunnel.getBiDirect())) {
            return;
        }
        TunnelUnifyKey positiveTunnelKey = positiveTunnel.getTunnelUnifyKey();
        byte holdPriority = positiveTunnel.getTeArgumentBean().getHoldPriority();
        if (positiveTunnelKey.isBiDirectional()) {
            PortKey reversePortKey =
                    new PortKey(oldLink.getDestination().getDestNode(), oldLink.getDestination().getDestTp());
            BandWidthMng.getInstance().free(reversePortKey, holdPriority, positiveTunnelKey, PceResult.nullPceResult);
            Logs.info(LOG, "free reverse port bw with {} {} holdPriority={}", reversePortKey, positiveTunnelKey,
                      holdPriority);
        }
    }

    private Set<ITunnel> handleDelayIncrease(Link link, Graph<NodeId, Link> graph) {
        // 时延变大通知刷新经过该Link的带时延约束的业务
        final Predicate<ITunnel> delayRestricted = ITunnel::isDelayRestricted;
        return getServicesOnLinkNeedRefresh(link, graph, delayRestricted).collect(Collectors.toSet());
    }

    private Set<ITunnel> handleDelayDecrease(List<ITunnel> allServices) {
        // 时延变小通知刷新带时延约束的路径不理想的业务
        final Predicate<ITunnel> delayRestricted = ITunnel::isDelayRestricted;
        final Predicate<ITunnel> delayIneligible = tunnel -> !tunnel.isDelayEligible();
        final Predicate<ITunnel> noPath = ITunnel::isUnestablished;
        final Predicate<ITunnel> hsbOverlapPath = ITunnel::isPathOverlap;

        Predicate<ITunnel> needRefresh = delayRestricted.and(delayIneligible.or(noPath).or(hsbOverlapPath));
        return getFilteredServices(allServices, needRefresh).collect(Collectors.toSet());
    }
}
