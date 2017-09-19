/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.ngip.ipsdn.pce.path.calculator.api;

import java.util.concurrent.ExecutionException;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.RemoveTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathOutput;

public interface PcePathCalculator {
    CreateTunnelPathOutput createTunnelPath(
            CreateTunnelPathInput input) throws ExecutionException, InterruptedException;

    UpdateTunnelPathOutput updateTunnelPath(
            UpdateTunnelPathInput input) throws ExecutionException, InterruptedException;

    boolean removeTunnelPath(RemoveTunnelPathInput input) throws ExecutionException, InterruptedException;
}
