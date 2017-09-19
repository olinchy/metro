/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.util;

import java.util.Optional;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.te.argument.lsp.NextAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 10204924 on 2017/2/13.
 */
public class NextAddressMatcher {
    private NextAddressMatcher() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(NextAddressMatcher.class);

    /**
     * isLinkMatchNextAddress.
     *
     * @param link        link
     * @param nextAddress nextAddress
     * @return match or not
     */
    public static boolean isLinkMatchNextAddress(Link link, NextAddress nextAddress) {
        if (link == null) {
            LOG.error("isLinkMatchNextAddress link is null!");
            throw new NullPointerException("link is null!");
        }
        if (nextAddress == null) {
            return true;
        }
        boolean isIpv4NextAddress = Optional.ofNullable(nextAddress.isIpv4()).orElse(false);
        boolean isMatched = isLinkDstMatchNextAddressDst(link.getDestination(), nextAddress.getDestination());
        if (isMatched) {
            return true;
        } else if (isIpv4NextAddress) {
            return isLinkSrcMatchNextAddressDst(link.getSource(), nextAddress.getDestination());
        }
        return false;
    }

    private static boolean isLinkDstMatchNextAddressDst(Destination linkDst, Destination nextAddressDst) {
        return isNodeAndTpMatchNextAddressDst(linkDst.getDestNode(), linkDst.getDestTp(), nextAddressDst);
    }

    private static boolean isLinkSrcMatchNextAddressDst(Source linkSrc, Destination nextAddressDst) {
        return isNodeAndTpMatchNextAddressDst(linkSrc.getSourceNode(), linkSrc.getSourceTp(), nextAddressDst);
    }

    private static boolean isNodeAndTpMatchNextAddressDst(NodeId linkNode, TpId linkTp, Destination nextAddressDst) {
        Optional<Destination> destination = Optional.ofNullable(nextAddressDst);
        if (destination.isPresent()) {
            if (!linkNode.equals(destination.get().getDestNode())) {
                return false;
            } else {
                return destination.map(Destination::getDestTp).map(linkTp::equals).orElse(true);
            }
        }
        return true;
    }
}
