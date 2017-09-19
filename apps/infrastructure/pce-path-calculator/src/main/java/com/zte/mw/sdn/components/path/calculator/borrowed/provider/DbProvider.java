/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.provider;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class DbProvider {
    private DbProvider() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(DbProvider.class);
    private static DbProvider instance = null;
    private DataBroker dataBroker;

    public static DbProvider getInstance() {

        if (instance == null) {
            instance = new DbProvider();
        }
        return instance;
    }

    public ListenerRegistration<DataChangeListener> registerDataChangeListener(
            InstanceIdentifier<?> path, DataChangeListener listener) {
        if (dataBroker == null) {
            LOG.error("registerDataChangeListener error, dataBroker null!");
            return null;
        }
        return dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                                                     path, listener, DataChangeScope.SUBTREE);
    }

    public ListenerRegistration<DataChangeListener> registerDataChangeListenerForOperational(
            InstanceIdentifier<?> path,
            DataChangeListener listener) {
        if (dataBroker == null) {
            LOG.error("registerDataChangeListener error, dataBroker null!");
            return null;
        }
        return dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, path, listener,
                                                     DataChangeScope.SUBTREE);
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void deleteOptionalData(InstanceIdentifier<?> path) throws OperationFailedException {
        DataObject data = readConfigrationData(path);
        if (data == null) {
            return;
        }
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.CONFIGURATION, path);
        try {
            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("", e);
        }
    }

    public <T extends DataObject> T readConfigrationData(InstanceIdentifier<T> path) {
        if (dataBroker == null) {
            LOG.error("readConfigrationData error, dataBroker null!");
            return null;
        }
        ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();
        try {
            Optional<T> optional = tx.read(LogicalDatastoreType.CONFIGURATION, path).checkedGet();
            if (optional.isPresent()) {
                return optional.get();
            } else {
                return null;
            }
        } catch (ReadFailedException | IllegalStateException e) {
            LOG.warn("PCE DB warring", e);
            return null;
        }
    }

    public <T extends DataObject> void mergeOperationalData(
            InstanceIdentifier<T> path,
            T data) throws OperationFailedException {
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.CONFIGURATION, path, data, true);
        try {
            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("", e);
        }
    }

    public <T extends DataObject> void mergeOperationalDbData(
            InstanceIdentifier<T> path,
            T data) throws OperationFailedException {
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.OPERATIONAL, path, data, true);
        try {
            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("", e);
        }
    }

    public void deleteOptionalDbData(InstanceIdentifier<?> path) throws OperationFailedException {
        DataObject data = readOperationalDbData(path);
        if (data == null) {
            return;
        }
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, path);
        try {
            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("", e);
        }
    }

    public <T extends DataObject> T readOperationalDbData(InstanceIdentifier<T> path) {
        if (dataBroker == null) {
            LOG.error("readOperationalData error, dataBroker null!");
            return null;
        }
        ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();
        try {
            Optional<T> optional = tx.read(LogicalDatastoreType.OPERATIONAL, path).checkedGet();
            if (optional.isPresent()) {
                return optional.get();
            } else {
                return null;
            }
        } catch (ReadFailedException | IllegalStateException e) {
            LOG.warn("PCE DB warring", e);
            return null;
        }
    }
}
