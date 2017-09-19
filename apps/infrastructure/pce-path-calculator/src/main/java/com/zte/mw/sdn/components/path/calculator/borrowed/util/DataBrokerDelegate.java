/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.util;

import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import com.zte.ngip.ipsdn.common.db.DataBrokerOperation;
import com.zte.ngip.ipsdn.common.db.DataStoreSilentProcessor;

public class DataBrokerDelegate {
    private DataBrokerDelegate() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(DataBrokerDelegate.class);
    private static DataBrokerDelegate instance;

    static {
        instance = new DataBrokerDelegate();
    }

    private DataStoreSilentProcessor processor;

    public static DataBrokerDelegate getInstance() {
        return instance;
    }

    public void setDataBroker(DataBroker dataBroker) {
        processor = new DataStoreSilentProcessor(dataBroker);
    }

    public void setDataBrokerAndExecutor(DataBroker dataBroker, ExecutorService executorService) {
        processor = new DataStoreSilentProcessor(dataBroker, executorService);
    }

    public <T extends DataObject> void put(
            final LogicalDatastoreType type, final InstanceIdentifier<T> path,
            final T data) {
        checkForReady();
        processor.addDBOper(new DataBrokerOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction tx) {
                tx.put(type, path, data);
            }
        });
    }

    private void checkForReady() {
        Preconditions.checkNotNull(processor, "you must inject dataBroker Service before you use DataBrokerDelegate");
    }

    public <T extends DataObject> void delete(final LogicalDatastoreType type, final InstanceIdentifier<T> path) {
        checkForReady();
        processor.addDBOper(new DataBrokerOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction tx) {
                tx.delete(type, path);
            }
        });
    }

    public <T extends DataObject> ListenableFuture<Optional<T>> read(
            final LogicalDatastoreType type,
            final InstanceIdentifier<T> path) {
        checkForReady();
        final SettableFuture<Optional<T>> retFuture = SettableFuture.create();
        processor.addDBOper(new DataBrokerOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction tx) {
                Futures.addCallback(tx.read(type, path), new FutureCallback<Optional<T>>() {
                    @Override
                    public void onSuccess(Optional<T> result) {
                        retFuture.set(result);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.warn("occur execption while read {} , {}", path, throwable);
                        retFuture.set(Optional.<T>absent());
                    }
                });
            }
        });
        return retFuture;
    }
}
