/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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
import com.zte.ngip.ipsdn.common.db.DataStoreSilentProcessor;

public class DataBrokerProvider {
    private DataBrokerProvider() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(DataBrokerProvider.class);
    private static DataBrokerProvider instance = new DataBrokerProvider();
    private DataBroker dataBroker;
    private DataStoreSilentProcessor processor;

    /*
     *
     */
    public static DataBrokerProvider getInstance() {
        return instance;
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    /*
     *
     */
    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        processor = new DataStoreSilentProcessor(dataBroker);
    }

    public <T extends DataObject> T readConfigurationDb(InstanceIdentifier<T> path) {
        ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();
        Optional<T> result;
        try {
            result = tx.read(LogicalDatastoreType.CONFIGURATION, path).checkedGet();
            tx.close();
        } catch (ReadFailedException e) {
            LOG.warn("readOperationalData ", e);
            return null;
        }
        if (!result.isPresent()) {
            LOG.trace("LocalNodes is not presented in Operational/DS.");
            return null;
        }
        return result.get();
    }

    /*
     *
     */
    public <T extends DataObject> void deleteConfiguration(final InstanceIdentifier<T> path) {
        checkForReady();
        processor.addDBOper(tx -> {
            try {
                Optional<T> obj = tx.read(LogicalDatastoreType.CONFIGURATION, path).checkedGet();
                if (obj.isPresent()) {
                    tx.delete(LogicalDatastoreType.CONFIGURATION, path);
                } else {
                    LOG.info("delete obj isn't exist {}", path);
                }
            } catch (ReadFailedException | IllegalStateException e) {
                LOG.warn("delete occur exception when read{}", e);
            }
        });
    }

    private void checkForReady() {
        Preconditions.checkNotNull(processor, "you must inject dataBroker Service before you use DataBrokerDelegate");
    }

    /*
     *
     */
    public <T extends DataObject> void putConfiguration(final InstanceIdentifier<T> path, final T data) {
        checkForReady();
        processor.addDBOper(tx -> tx.put(LogicalDatastoreType.CONFIGURATION, path, data, true));
    }

    /*
     *
     */
    public synchronized <T extends DataObject> void mergeConfiguration(
            final InstanceIdentifier<T> path, final T data) {
        checkForReady();
        processor.addDBOper(tx -> tx.merge(LogicalDatastoreType.CONFIGURATION, path, data, true));
    }

    public <T extends DataObject> T readConfiguration(InstanceIdentifier<T> path) {
        try {
            Optional<T> temp = read(LogicalDatastoreType.CONFIGURATION, path).get(10L, TimeUnit.SECONDS);
            if (temp.isPresent()) {
                return temp.get();
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("read Data ", e);
            return null;
        }
        LOG.trace("LocalNodes is not presented in CONFIGURATION/DS.");
        return null;
    }

    /*
     *
     */
    public <T extends DataObject> ListenableFuture<Optional<T>> read(
            final LogicalDatastoreType type,
            final InstanceIdentifier<T> path) {
        checkForReady();
        final SettableFuture<Optional<T>> retFuture = SettableFuture.create();
        processor.addDBOper(tx -> Futures.addCallback(
                tx.read(type, path),
                new FutureCallback<Optional<T>>() {
                    @Override
                    public void onSuccess(Optional<T> result) {
                        retFuture.set(result);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.warn("occur execption while read {} , {}", path, throwable);
                        retFuture.set(Optional.absent());
                    }
                })
        );
        return retFuture;
    }
}
