/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.runtime;

import com.zte.mw.sdn.Model;
import com.zte.mw.sdn.connection.Connection;
import com.zte.mw.sdn.connection.Driver;

public class StubDriver implements Driver {
    @Override
    public void config(final Model model, final Connection connection) {
        //        connection.config(identifier, );
    }
}
