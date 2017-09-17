/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.runtime;

import java.util.concurrent.ThreadPoolExecutor;

import com.zte.mw.sdn.connection.Connection;
import com.zte.mw.sdn.connection.Driver;


public interface MicrowaveRuntime {
    ThreadPoolExecutor getDispatchPool();

    ThreadPoolExecutor getConfigurationPool();

    Connection createSouthConnection(String neIdentity);

    Driver[] getDrivers();
}
