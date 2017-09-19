/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationProvider {
    private NotificationProvider() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(NotificationProvider.class);
    private static NotificationProvider instance = new NotificationProvider();
    private NotificationProviderService notificationService;

    public static NotificationProvider getInstance() {
        return instance;
    }

    public static void setInstance(NotificationProvider ins) {
        instance = ins;
    }

    public void setNotificationService(
            NotificationProviderService notificationService) {
        this.notificationService = notificationService;
    }

    public <T extends Notification> void notify(T notification) {
        if (null != notificationService) {
            LOG.info("pce notification publish");
            notificationService.publish(notification);
        }
    }
}
