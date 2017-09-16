/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.connection;

import com.zte.mw.sdn.exceptions.connections.ConnectionException;

public interface Connection {
    void commit() throws ConnectionException;

    void rollback() throws ConnectionException;
}
