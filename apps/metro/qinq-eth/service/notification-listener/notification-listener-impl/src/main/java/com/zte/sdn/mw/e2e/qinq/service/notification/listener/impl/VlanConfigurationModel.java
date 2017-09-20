/*
 * Copyright © 2015 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.sdn.mw.e2e.qinq.service.notification.listener.impl;

import org.opendaylight.yang.gen.v1.http.www.zte.com.cn.zxr10.netconf.schema.rosng.switchvlan.rev160711.Configuration;
import org.opendaylight.yangtools.yang.binding.DataObject;

import com.zte.mw.sdn.Model;

/**
 * Created by root on 17-9-19.
 */
public class VlanConfigurationModel implements Model {
    public VlanConfigurationModel(String neIdentity, Configuration configuration) {
        this.neIdentity = neIdentity;
        this.configuration = configuration;
    }

    private String neIdentity;
    private Configuration configuration;

    @Override
    public String getNeIdentity() {
        return neIdentity;
    }

    @Override
    public <T extends DataObject> T get(final Class<T> dataClass) {
        return (T) configuration;
    }
}
