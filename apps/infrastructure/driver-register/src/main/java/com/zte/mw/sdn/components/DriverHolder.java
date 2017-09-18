/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components;

import java.util.ArrayList;

import com.zte.mw.sdn.connection.Driver;

public class DriverHolder implements AutoCloseable, DriverRegister {
    private ArrayList<Driver> list = new ArrayList<>();

    @Override
    public void close() throws Exception {

    }

    @Override
    public Driver[] getRegistered() {
        return list.toArray(new Driver[list.size()]);
    }

    @Override
    public void register(final Driver driver) {
        this.list.add(driver);
    }
}
