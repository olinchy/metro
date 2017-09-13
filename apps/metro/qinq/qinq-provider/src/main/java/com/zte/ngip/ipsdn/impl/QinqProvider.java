/*
 * Copyright (c) 2015 ZTE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.ngip.ipsdn.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QinqProvider implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(QinqProvider.class);

    @Override
    public void close() throws Exception {
        LOG.info("QinqProvider Closed");
    }
}
