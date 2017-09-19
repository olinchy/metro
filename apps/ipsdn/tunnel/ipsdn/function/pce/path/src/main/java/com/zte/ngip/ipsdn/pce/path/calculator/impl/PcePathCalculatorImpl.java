/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.ngip.ipsdn.pce.path.calculator.impl;

import java.util.concurrent.ExecutionException;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.impl.rev141210.PcePathModule;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.RemoveTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathOutput;

import com.zte.ngip.ipsdn.pce.path.calculator.api.PcePathCalculator;
import com.zte.ngip.ipsdn.pce.path.impl.provider.PcePathProvider;

public class PcePathCalculatorImpl implements PcePathCalculator, AutoCloseable {
    public PcePathCalculatorImpl(
            final PcePathProvider provider,
            final PcePathModule pcePathModule) {
        this.provider = provider;
        this.pcePathModule = pcePathModule;
    }

    private final PcePathProvider provider;
    private final PcePathModule pcePathModule;

    @Override
    public CreateTunnelPathOutput createTunnelPath(
            final CreateTunnelPathInput input) throws ExecutionException, InterruptedException {
        return provider.createTunnelPath(input).get().getResult();
    }

    @Override
    public UpdateTunnelPathOutput updateTunnelPath(
            final UpdateTunnelPathInput input) throws ExecutionException, InterruptedException {
        return provider.updateTunnelPath(input).get().getResult();
    }

    @Override
    public boolean removeTunnelPath(final RemoveTunnelPathInput input) throws ExecutionException, InterruptedException {
        return provider.removeTunnelPath(input).get().isSuccessful();
    }

    @Override
    public void close() throws Exception {
        pcePathModule.moduleClose();
    }
}
