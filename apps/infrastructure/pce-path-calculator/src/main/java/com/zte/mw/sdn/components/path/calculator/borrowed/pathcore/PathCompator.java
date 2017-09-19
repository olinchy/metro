/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.pathcore;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

public class PathCompator {
    private PathCompator() {

    }

    public static boolean isPathEqual(
            List<Link> path1,
            List<Link> path2) {
        if ((path1 == null && path2 == null)
                || (path1 == path2)) {
            return true;
        }

        if (path1 == null || path2 == null) {
            return false;
        }

        if (path1.size() != path2.size()) {
            return false;
        }

        return isValidPathEqual(path1, path2);
    }

    private static boolean isValidPathEqual(List<Link> path1, List<Link> path2) {
        for (int i = 0; i < path1.size(); ++i) {
            if (!isLinkEqual(path1.get(i), path2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLinkEqual(Link link1, Link link2) {
        if (sourceAreEqual(link1.getSource(), link2.getSource()) && destinationAreEqual(
                link1.getDestination(),
                link2.getDestination())) {
            return true;
        } else {
            return sourceEqual2Destination(link1.getSource(), link2.getDestination()) && sourceEqual2Destination(
                    link2.getSource(), link1.getDestination());
        }
    }

    private static boolean sourceAreEqual(Source source1, Source source2) {
        return source1.getSourceNode().equals(source2.getSourceNode())
                && source1.getSourceTp().equals(source2.getSourceTp());
    }

    private static boolean destinationAreEqual(
            Destination destination1,
            Destination destination2) {
        return destination1.getDestNode().equals(destination2.getDestNode())
                && destination1.getDestTp().equals(destination2.getDestTp());
    }

    private static boolean sourceEqual2Destination(
            Source source,
            Destination destination) {
        return source.getSourceNode().equals(destination.getDestNode())
                && source.getSourceTp().equals(destination.getDestTp());
    }
}
