/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.util;

import java.util.LinkedList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.links.PathLink;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.links.PathLinkBuilder;

public class MplsLinkTools {
    private MplsLinkTools() {

    }

    public static List<PathLink> getMplsLinkPath(List<Link> path) {
        List<PathLink> mplsLinkPath = new LinkedList<>();
        if (path == null) {
            return mplsLinkPath;
        }
        for (Link link : path) {
            mplsLinkPath.add(new PathLinkBuilder(link).build());
        }

        return mplsLinkPath;
    }
}
