/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.util;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BandwidthAllocException;

/**
 * Created by 10087505 on 2017/6/7.
 */
@FunctionalInterface
public interface PceBandWidthConsumer {
    void accept() throws BandwidthAllocException;
}
