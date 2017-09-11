/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.infrastructure.e2e;

import com.zte.mw.sdn.infrastructure.task.Result;

/**
 * Created by odl on 17-9-11.
 */
public class NetConfDriver implements Driver {
    @Override
    public Result commit(final Model model) {
        return null;
    }

    @Override
    public void rollback(final Model model) {

    }
}
