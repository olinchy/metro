/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.connection.Driver;

public class DriverHolder implements AutoCloseable, DriverRegister {
    private static Logger lOG = LoggerFactory.getLogger(DriverHolder.class);
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
        lOG.info("driver " + driver.toString() + " registered");
        this.list.add(driver);
    }

    @Override
    public void remove(final Driver driver) {
        lOG.info("driver " + driver.toString() + " removed");
        list.remove(driver);
    }
}
