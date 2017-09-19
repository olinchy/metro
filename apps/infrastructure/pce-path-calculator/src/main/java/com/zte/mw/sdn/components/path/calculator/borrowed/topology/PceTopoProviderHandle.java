/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.topology;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.zte.ngip.ipsdn.pce.path.api.PceTopoProvider;

/**
 * Created by 10087505 on 2017/4/10.
 */
public class PceTopoProviderHandle implements InvocationHandler {
    public PceTopoProviderHandle(PceTopoProvider pceTopoProvider) {
        this.pceTopoProvider = pceTopoProvider;
    }

    PceTopoProvider pceTopoProvider;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        return method.invoke(pceTopoProvider, args);
    }
}
