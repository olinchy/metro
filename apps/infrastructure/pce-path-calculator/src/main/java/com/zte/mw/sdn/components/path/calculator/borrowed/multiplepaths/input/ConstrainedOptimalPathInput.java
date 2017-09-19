/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.input;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;

import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.BiDirect;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.input.OptimalPathInput;

/**
 * Created by 10204924 on 2017/6/2.
 */
public interface ConstrainedOptimalPathInput<V, E> extends OptimalPathInput<V, E> {
    List<E> getOldPath();

    TunnelUnifyKey getTunnelUnifyKey();

    TeArgumentBean getTeArgumentBean();

    BiDirect getBiDirect();

    List<Link> getExcludedLinks();

    BwSharedGroupContainer getBwSharedGroups();

    BwSharedGroupContainer getDeletedBwSharedGroups();

    boolean isNeedScaleBandwidth();

    boolean isRecalc();

    long getReqMaxDelay();

    List<Link> getMasterPath();
}
