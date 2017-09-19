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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.ITunnel;

import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Conditions;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

/**
 * Created by 10204924 on 2017/6/16.
 */
/*缓存哪些link会影响SR隧道路径的分段结果，以便于这些link属性变化时，有针对性地去刷新受影响的SR隧道的segments。
* 该缓存只作为运行时期数据，不持久化，当缓存丢失以后，就刷新所有SR隧道的Segments。*/
public class TunnelSegmentsAffectedByLinkCache {
    TunnelSegmentsAffectedByLinkCache() {
        Logs.info(LOG, "created! ");
    }

    private static final Logger LOG = LoggerFactory.getLogger(TunnelSegmentsAffectedByLinkCache.class);
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
    private Map<Link, Set<TunnelUnifyKey>> linkTunnelsMap = new HashMap<>();// 记录Link会影响哪些隧道的Segments

    private static String linkTunnelsMapToString(Map<Link, Set<TunnelUnifyKey>> linkTunnelsMap) {
        return linkTunnelsMap.entrySet().stream()
                .map(entry -> "Link{" + ComUtility.pathToString(Lists.newArrayList(entry.getKey())) + "}=" + entry
                        .getValue().toString()).reduce((s1, s2) -> s1 + ", " + s2).orElse("");
    }

    void update(Set<Link> links, TunnelUnifyKey tunnelUnifyKey) {
        writeLock.lock();
        Logs.debug(LOG, "{} lock for update links={} tunnel={}", Thread.currentThread().getName(),
                   ComUtility.pathToString(Lists.newArrayList(links)), tunnelUnifyKey);
        try {
            Logs.trace(LOG, "before update linkTunnelsMap={}", linkTunnelsMapToString(linkTunnelsMap));
            if (CollectionUtils.isNullOrEmpty(links)) { // 隧道没路就清除映射关系
                linkTunnelsMap.values().forEach(tunnelUnifyKeys -> tunnelUnifyKeys.remove(tunnelUnifyKey));
                Iterator<Map.Entry<Link, Set<TunnelUnifyKey>>> iterator = linkTunnelsMap.entrySet().iterator();
                while (iterator.hasNext()) { // link下挂的隧道为空，就删除该link
                    Map.Entry<Link, Set<TunnelUnifyKey>> entry = iterator.next();
                    Conditions.ifTrue(entry.getValue().isEmpty(), iterator::remove);
                }
            } else { // 添加
                for (Link link : links) {
                    linkTunnelsMap.computeIfAbsent(link, link1 -> new HashSet<>());
                    linkTunnelsMap.get(link).add(tunnelUnifyKey);
                }
            }
            Logs.debug(LOG, "After update linkTunnelsMap={}", linkTunnelsMapToString(linkTunnelsMap));
        } finally {
            writeLock.unlock();
            Logs.debug(LOG, "{} unlock after update", Thread.currentThread().getName());
        }
    }

    Set<ITunnel> getAffectedTunnels(
            List<Link> links,
            Function<TunnelUnifyKey, ITunnel> transferFunction, Supplier<Set<ITunnel>> defaultValueSupplier) {
        Set<TunnelUnifyKey> tunnelUnifyKeys = Collections.emptySet();
        boolean needRefreshAll;
        readLock.lock();
        Logs.debug(LOG, "{} lock for read", Thread.currentThread().getName());
        try {
            Logs.debug(LOG, "getTunnelsAffectedByLinksNeedRefreshSegments {}", linkTunnelsMap);
            if (linkTunnelsMap.isEmpty()) {
                needRefreshAll = true;
            } else {
                needRefreshAll = false;
                tunnelUnifyKeys = links.stream().map(linkTunnelsMap::get).filter(Objects::nonNull).flatMap(Set::stream)
                        .collect(Collectors.toSet());
            }
        } finally {
            readLock.unlock();
            Logs.debug(LOG, "{} unlock after read", Thread.currentThread().getName());
        }
        final Set<TunnelUnifyKey> needRemove = new HashSet<>();
        Set<ITunnel> needRefreshSegments;
        if (needRefreshAll) {
            needRefreshSegments = defaultValueSupplier.get();
        } else {
            needRefreshSegments = tunnelUnifyKeys.stream().map(key -> {
                ITunnel tunnel = transferFunction.apply(key);
                if (tunnel == null) {
                    needRemove.add(key);
                }
                return tunnel;
            }).filter(Objects::nonNull).distinct().filter(ITunnel::isSrTunnel)
                    .collect(Collectors.toSet());
        }
        remove(needRemove);
        return needRefreshSegments;
    }

    private void remove(Collection<TunnelUnifyKey> keys) {
        if (CollectionUtils.isNullOrEmpty(keys)) {
            return;
        }
        writeLock.lock();
        Logs.debug(LOG, "{} lock for remove", Thread.currentThread().getName());
        try {
            for (TunnelUnifyKey key : keys) {
                remove(key);
            }
        } finally {
            writeLock.unlock();
            Logs.debug(LOG, "{} unlock after remove", Thread.currentThread().getName());
        }
    }

    private void remove(TunnelUnifyKey key) {
        Logs.info(LOG, "remove TunnelUnifyKey={}", key);
        linkTunnelsMap.values().stream().filter(Objects::nonNull)
                .forEach(tunnelUnifyKeys -> tunnelUnifyKeys.remove(key));
    }

    void clear() {
        Logs.info(LOG, "clear");
        writeLock.lock();
        try {
            linkTunnelsMap.clear();
        } finally {
            writeLock.unlock();
        }
    }
}
