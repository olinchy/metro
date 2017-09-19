/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import java.util.concurrent.Future;

import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Created by 10087505 on 2017/6/15.
 */
public interface RpcRemoveHandler<I, O> {
    Future<RpcResult<O>> remove(I input);
}
