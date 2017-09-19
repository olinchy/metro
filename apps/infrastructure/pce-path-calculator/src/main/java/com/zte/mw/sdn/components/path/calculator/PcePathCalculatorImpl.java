/*
 * Copyright (c) 2017 ZTE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.AdjustTunnelBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CalcCalendarTunnelInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateSlaveTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateSlaveTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelGroupPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelGroupPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelHsbPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelHsbPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllTunnelThroughLinkInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllTunnelThroughLinkOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllTunnelThroughPortInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllTunnelThroughPortOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllUnreservedBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetAllUnreservedBandwidthOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetMaxAvailableBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetMaxAvailableBandwidthOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GlobalOptimizationInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.MigrateTopologyIdInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.QueryFailReasonInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.QueryFailReasonOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.QueryTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.QueryTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.RefreshAllBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.RefreshAllBandwidthOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.RemoveTunnelGroupPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.RemoveTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.SetMaintenanceNodesInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelGroupPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelGroupPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbWithoutRollbackInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbWithoutRollbackOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathWithoutRollbackInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathWithoutRollbackOutput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelTopoidInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelTopoidOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;

public class PcePathCalculatorImpl implements AutoCloseable, PcePathCalculator {
    private static final Logger LOG = LoggerFactory.getLogger(PcePathCalculatorImpl.class);
    private static final PcePathProvider proxy;

    static {
        LOG.info("init proxy of PcePathProvider");
        proxy = PcePathProvider.getInstance();
        proxy.recoveryDb();
        proxy.setZeroBandWidthFlag(false);
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Future<RpcResult<UpdateTunnelHsbWithoutRollbackOutput>> updateTunnelHsbWithoutRollback(
            final UpdateTunnelHsbWithoutRollbackInput input) {
        return proxy.updateTunnelHsbWithoutRollback(input);
    }

    @Override
    public Future<RpcResult<Void>> removeTunnelPath(
            final RemoveTunnelPathInput input) {
        return proxy.removeTunnelPath(input);
    }

    @Override
    public Future<RpcResult<Void>> removeTunnelGroupPath(
            final RemoveTunnelGroupPathInput input) {
        return proxy.removeTunnelGroupPath(input);
    }

    @Override
    public Future<RpcResult<Void>> adjustTunnelBandwidth(
            final AdjustTunnelBandwidthInput input) {
        return proxy.adjustTunnelBandwidth(input);
    }

    @Override
    public Future<RpcResult<GetAllUnreservedBandwidthOutput>> getAllUnreservedBandwidth(
            final GetAllUnreservedBandwidthInput input) {
        return proxy.getAllUnreservedBandwidth(input);
    }

    @Override
    public Future<RpcResult<UpdateTunnelHsbPathOutput>> updateTunnelHsbPath(
            final UpdateTunnelHsbPathInput input) {
        return proxy.updateTunnelHsbPath(input);
    }

    @Override
    public Future<RpcResult<UpdateTunnelGroupPathOutput>> updateTunnelGroupPath(
            final UpdateTunnelGroupPathInput input) {
        return proxy.updateTunnelGroupPath(input);
    }

    @Override
    public Future<RpcResult<UpdateTunnelPathWithoutRollbackOutput>> updateTunnelPathWithoutRollback(
            final UpdateTunnelPathWithoutRollbackInput input) {
        return proxy.updateTunnelPathWithoutRollback(input);
    }

    @Override
    public Future<RpcResult<Void>> setMaintenanceNodes(
            final SetMaintenanceNodesInput input) {
        return proxy.setMaintenanceNodes(input);
    }

    @Override
    public Future<RpcResult<CreateTunnelPathOutput>> createTunnelPath(
            final CreateTunnelPathInput input) {
        return proxy.createTunnelPath(input);
    }

    @Override
    public Future<RpcResult<CreateTunnelGroupPathOutput>> createTunnelGroupPath(
            final CreateTunnelGroupPathInput input) {
        return proxy.createTunnelGroupPath(input);
    }

    @Override
    public Future<RpcResult<QueryFailReasonOutput>> queryFailReason(
            final QueryFailReasonInput input) {
        return proxy.queryFailReason(input);
    }

    @Override
    public Future<RpcResult<UpdateTunnelPathOutput>> updateTunnelPath(
            final UpdateTunnelPathInput input) {
        return proxy.updateTunnelPath(input);
    }

    @Override
    public Future<RpcResult<Void>> migrateTopologyId(
            final MigrateTopologyIdInput input) {
        return proxy.migrateTopologyId(input);
    }

    @Override
    public Future<RpcResult<GetMaxAvailableBandwidthOutput>> getMaxAvailableBandwidth(
            final GetMaxAvailableBandwidthInput input) {
        return proxy.getMaxAvailableBandwidth(input);
    }

    @Override
    public Future<RpcResult<Void>> calcCalendarTunnel(
            final CalcCalendarTunnelInput input) {
        return proxy.calcCalendarTunnel(input);
    }

    @Override
    public Future<RpcResult<CreateTunnelHsbPathOutput>> createTunnelHsbPath(
            final CreateTunnelHsbPathInput input) {
        return proxy.createTunnelHsbPath(input);
    }

    @Override
    public Future<RpcResult<RefreshAllBandwidthOutput>> refreshAllBandwidth(
            final RefreshAllBandwidthInput input) {
        return proxy.refreshAllBandwidth(input);
    }

    @Override
    public Future<RpcResult<UpdateTunnelTopoidOutput>> updateTunnelTopoid(
            final UpdateTunnelTopoidInput input) {
        return proxy.updateTunnelTopoid(input);
    }

    @Override
    public Future<RpcResult<CreateSlaveTunnelPathOutput>> createSlaveTunnelPath(
            final CreateSlaveTunnelPathInput input) {
        return proxy.createSlaveTunnelPath(input);
    }

    @Override
    public Future<RpcResult<QueryTunnelPathOutput>> queryTunnelPath(
            final QueryTunnelPathInput input) {
        return proxy.queryTunnelPath(input);
    }

    @Override
    public Future<RpcResult<Void>> globalOptimization(
            final GlobalOptimizationInput input) {
        return proxy.globalOptimization(input);
    }

    @Override
    public Future<RpcResult<GetAllTunnelThroughLinkOutput>> getAllTunnelThroughLink(
            final GetAllTunnelThroughLinkInput input) {
        return proxy.getAllTunnelThroughLink(input);
    }

    @Override
    public Future<RpcResult<GetRealtimePathOutput>> getRealtimePath(
            final GetRealtimePathInput input) {
        return proxy.getRealtimePath(input);
    }

    @Override
    public Future<RpcResult<GetAllTunnelThroughPortOutput>> getAllTunnelThroughPort(
            final GetAllTunnelThroughPortInput input) {
        return proxy.getAllTunnelThroughPort(input);
    }
}
