/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.pathcore;

import java.util.List;

import com.zte.ngip.ipsdn.pce.path.api.srlg.AovidLinks;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.transformer.ITransformerFactory;

/**
 * Created by 10088483 on 5/3/16.
 */
public class BandwidthTransformerFactory implements ITransformerFactory<BandwidthTransformer> {
    @Override
    public BandwidthTransformer create(List<AovidLinks> contrainedLinks, TunnelUnifyKey tunnelKey) {
        return new BandwidthTransformer(tunnelKey);
    }
}
