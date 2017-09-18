/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn;

import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Created by odl on 17-9-11.
 */
public interface Model {
    String getNeIdentity();

    <T extends DataObject> T get(Class<T> dataClass);

    enum OperationType {
        CREATE, DELETE, UPDATE
    }
}
