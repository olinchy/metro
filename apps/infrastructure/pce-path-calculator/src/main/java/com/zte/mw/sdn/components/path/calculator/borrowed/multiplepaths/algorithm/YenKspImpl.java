/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.te.argument.lsp.NextAddress;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.te.argument.lsp.NextAddressBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input.ConstrainedOptimalPathInput;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input.ConstrainedOptimalPathInputBuilder;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser.PathChooser;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.MultiplePathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.util.NextAddressMatcher;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.core.strategy.ICalcStrategy;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformer;

/**
 * Created by 10204924 on 2017/2/3.
 */
public final class YenKspImpl implements MultiplePathsAlgorithm<PathResult> {
    /**
     * Construct an implication of KSP algorithm.
     *
     * @param initialInput                           initialInput
     * @param constrainedOptimalPathAlgorithmFactory constrainedOptimalPathAlgorithmFactory
     */
    public YenKspImpl(
            ConstrainedOptimalPathInput<NodeId, Link> initialInput,
            ConstrainedOptimalPathAlgorithmFactory<NodeId, Link> constrainedOptimalPathAlgorithmFactory) {
        this.initialInput = initialInput;
        this.constrainedOptimalPathAlgorithmFactory = constrainedOptimalPathAlgorithmFactory;
    }

    private static final Logger LOG = LoggerFactory.getLogger(YenKspImpl.class);
    private final LinkedList<PathResult> shortestPaths = new LinkedList<>();
    private final List<PathResult> potentialPaths = new ArrayList<>();
    private final MultiplePathResult<PathResult> multiplePathResult = new MultiplePathResult<>(shortestPaths);
    private final ConstrainedOptimalPathInput<NodeId, Link> initialInput;
    private final ConstrainedOptimalPathAlgorithmFactory<NodeId, Link> constrainedOptimalPathAlgorithmFactory;

    private static boolean isPathValid(List<Link> path) {
        return path != null && !path.isEmpty();
    }

    private static List<NextAddress> getRestNextAddresses(
            NextAddress curNextAddress,
            List<NextAddress> initialNextAddresses) {
        int start = initialNextAddresses.indexOf(curNextAddress);
        int end = initialNextAddresses.size();
        return new ArrayList<>(initialNextAddresses.subList(start, end));
    }

    @Override
    public MultiplePathResult<PathResult> calcMultiplePaths(final int maxK, PathChooser<PathResult> chooser) {
        if (maxK == 0) {
            return multiplePathResult;
        }
        Preconditions.checkArgument(maxK >= 0, "maxK should not be less than 0!");
        Preconditions.checkNotNull(
                constrainedOptimalPathAlgorithmFactory,
                "constrainedOptimalPathAlgorithmFactory should not be null!");

        /* calc first path */
        PathResult firstPath = calcOptimalPath(initialInput);
        if (!isPathValid(firstPath.getPath())) {

            Logs.info(LOG, "calc first path failed!");

            multiplePathResult.setFailType(firstPath.getPceResult().getFailReason());
        } else {
            shortestPaths.addLast(firstPath);

            Logs.info(LOG, "calcMultiplePaths k=0 path=\n{}", ComUtility.pathToString(firstPath.getPath()));

            if (chooser != null && chooser.choose(shortestPaths.getLast())) {
                return multiplePathResult;
            }
            /* calc rest K-1 paths */
            calcRestPaths(maxK, chooser);
        }

        return multiplePathResult;
    }

    private PathResult calcOptimalPath(ConstrainedOptimalPathInput<NodeId, Link> input) {
        return (PathResult) constrainedOptimalPathAlgorithmFactory.create(input).calcConstrainedOptimalPath(input);
    }

    private void calcRestPaths(final int maxK, PathChooser<PathResult> chooser) {
        for (int k = 1; k < maxK; k++) {
            calcPotentialPaths(k);
            if (potentialPaths.isEmpty()) {
                Logs.info(LOG, "calcMultiplePaths k={} path=\n[]", k);
                return;
            }
            sortPotentialPathsAscend();
            shortestPaths.add(potentialPaths.remove(0));

            Logs.info(LOG, "calcMultiplePaths k={} path=\n{}", k,
                      ComUtility.pathToString(shortestPaths.getLast().getPath()));

            if (chooser != null && chooser.choose(shortestPaths.getLast())) {
                return;
            }
        }
    }

    private void calcPotentialPaths(int kth) {
        List<Link> lastPath = shortestPaths.getLast().getPath();

        for (int i = 0; i < lastPath.size(); i++) {
            List<Link> rootPath = lastPath.subList(0, i);

            /* Backup initialNextAddress, because it will be modified by replaceNextAddress*/
            List<NextAddress> initialNextAddress =
                    Optional.ofNullable(initialInput.getTeArgumentBean().getNextAddress())
                            .orElse(Collections.emptyList());

            List<NextAddress> tmpNextAddresses = new ArrayList<>();
            boolean needContinue = generateTmpNextAddresses(rootPath, initialNextAddress, tmpNextAddresses);
            /* If rootPath doesn't match the strict nextAddresses, needn't calculate the spurPath, do continue */
            if (needContinue) {
                continue;
            }

            List<Link> tmpExcludedLinks = new ArrayList<>();
            tmpExcludedLinks.addAll(initialInput.getExcludedLinks());
            tmpExcludedLinks.addAll(getTmpExcludedLinks(rootPath));

            ConstrainedOptimalPathInput<NodeId, Link> kthInput =
                    new ConstrainedOptimalPathInputBuilder(initialInput)
                            .setExcludedLinks(tmpExcludedLinks).build();

            /* This step will modify initialNextAddress. Because kthInput is a shallow copy of initialInput,
             * some modification of kthInput will affect initialInput. */
            kthInput.getTeArgumentBean().getArgLsp().replaceNextAddress(tmpNextAddresses);

            PathResult potentialKthPath = calcOptimalPath(kthInput);

            if (isPathValid(potentialKthPath.getPath())) {
                addPotentialPathIfAbsent(potentialKthPath);
                Logs.debug(LOG, "k={} i={} potentialKthPath=\n{}", kth, i,
                           ComUtility.pathToString(potentialKthPath.getPath()));
            } else {
                Logs.debug(LOG, "k={} i={} potentialKthPath=\n{}", kth, i, "[]");
            }
            /* reset initialNextAddress */
            kthInput.getTeArgumentBean().getArgLsp().replaceNextAddress(initialNextAddress);
        }
    }

    private void sortPotentialPathsAscend() {
        final ICalcStrategy<NodeId, Link> calcStrategy = initialInput.getCalcStrategy();
        final ITransformer<Link> transformer = initialInput.getTransformer();
        potentialPaths.sort((path1, path2) -> calcStrategy.compare(path1.getPath(), path2.getPath(), transformer));
    }

    private boolean generateTmpNextAddresses(
            List<Link> rootPath, List<NextAddress> initialNextAddresses,
            List<NextAddress> tmpNextAddresses) {

        boolean needContinue = false;
        Iterator<NextAddress> iterator = initialNextAddresses.iterator();
        NextAddress curNextAddress = iterator.hasNext() ? iterator.next() : null;
        while (curNextAddress != null) {
            boolean isFound = findNextAddressInRootPath(rootPath, curNextAddress);
            if (!isFound) {
                needContinue = Optional.ofNullable(curNextAddress.isStrict()).orElse(false);
                break;
            }
            curNextAddress = iterator.hasNext() ? iterator.next() : null;
        }

        List<NextAddress> nextAddressesOfRootPath = transformRootPathToStrictNextAddresses(rootPath);
        tmpNextAddresses.addAll(nextAddressesOfRootPath);
        if (curNextAddress != null) {
            List<NextAddress> restNextAddresses = getRestNextAddresses(curNextAddress, initialNextAddresses);
            tmpNextAddresses.addAll(restNextAddresses);
        }
        return needContinue;
    }

    private Set<Link> getTmpExcludedLinks(final List<Link> rootPath) {
        Set<Link> tmpExcludedLinks = new HashSet<>();
        for (PathResult kthPath : shortestPaths) {
            final int rootSize = rootPath.size();
            if (kthPath.getPath().size() > rootSize && rootPath.equals(kthPath.getPath().subList(0, rootSize))) {
                tmpExcludedLinks.add(kthPath.getPath().get(rootSize));
            }
        }
        return tmpExcludedLinks;
    }

    private void addPotentialPathIfAbsent(final PathResult potentialPath) {
        if (!potentialPaths.contains(potentialPath)) {
            potentialPaths.add(potentialPath);
        }
    }

    private boolean findNextAddressInRootPath(List<Link> rootPath, NextAddress curNextAddress) {
        return rootPath.parallelStream()
                .anyMatch(link -> NextAddressMatcher.isLinkMatchNextAddress(link, curNextAddress));
    }

    private List<NextAddress> transformRootPathToStrictNextAddresses(List<Link> rootPath) {
        return rootPath.stream().map(LinkAttributes::getDestination)
                .map(destination -> new NextAddressBuilder().setDestination(destination).setStrict(true)
                        .build()).collect(Collectors.toList());
    }
}
