/*
 * Copyright (c) 2017 ZTE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.zte.sdn.mw.e2e.qinq.servic.nbi.rpc.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiRpcProvider implements  AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NbiRpcProvider.class);


    @Override
    public void close() throws Exception {
        LOG.info("NbiRpcProvider Closed");
    }

}
