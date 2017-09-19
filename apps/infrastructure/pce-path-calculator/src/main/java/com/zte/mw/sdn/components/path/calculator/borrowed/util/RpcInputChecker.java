/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.CreateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.RemoveServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.UpdateServiceInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.AdjustTunnelBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.Affinity;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateSlaveTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelHsbPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.CreateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetMaxAvailableBandwidthInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelHsbPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.UpdateTunnelPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.adjust.tunnel.bandwidth.input.PathAdjustRequest;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.affinity.AffinityStrategy;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.te.argument.lsp.NextAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import com.zte.mw.sdn.components.path.calculator.borrowed.affinity.AffinityStrategyChecker;

import com.zte.ngip.ipsdn.pce.path.api.util.Conditions;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;

/**
 * Created by 10204924 on 2017/4/6.
 */
public class RpcInputChecker {
    private RpcInputChecker() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(RpcInputChecker.class);
    private static final String HEAD_NODE_ID = "HeadNodeId";
    private static final String TAIL_NODE_ID = "TailNodeId";
    private static final String TUNNEL_ID = "TunnelId";
    private static final String SERVICE_NAME = "Service_name";

    /**
     * Check CreateTunnelPathInput.
     *
     * @param createTunnelPathInput createTunnelPathInput
     * @return result
     */
    public static boolean check(CreateTunnelPathInput createTunnelPathInput) {
        return new CheckChain<CreateTunnelPathInput>()
                .set(input -> checkNotNull(input, "CreateTunnelPathInput"))
                .set(input -> checkNotNull(
                        new Object[]{input.getHeadNodeId(), input.getTailNodeId()},
                        new String[]{HEAD_NODE_ID, TAIL_NODE_ID}))
                .set(input -> checkAffinityStrategy(input.getAffinityStrategy()))
                .check(createTunnelPathInput);
    }

    private static boolean checkNotNull(Object input, String name) {
        if (input == null) {
            Logs.error(LOG, "{} is null", name);
            return false;
        }
        return true;
    }

    private static boolean checkNotNull(Object[] objects, String[] names) {
        for (int i = 0; i < objects.length; i++) {
            if (!checkNotNull(objects[i], names[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkAffinityStrategy(AffinityStrategy affinityStrategy) {
        String checkResult = new AffinityStrategyChecker().check(affinityStrategy);
        if (!Strings.isNullOrEmpty(checkResult)) {
            Logs.error(LOG, checkResult, affinityStrategy);
            return false;
        }
        return true;
    }

    /**
     * Check CreateSlaveTunnelPathInput.
     *
     * @param createSlaveTunnelPathInput createSlaveTunnelPathInput
     * @return result
     */
    public static boolean check(CreateSlaveTunnelPathInput createSlaveTunnelPathInput) {
        return new CheckChain<CreateSlaveTunnelPathInput>()
                .set(input -> checkNotNull(input, "CreateSlaveTunnelPathInput"))
                .set(input -> checkNotNull(
                        new Object[]{input.getHeadNodeId(), input.getTailNodeId()},
                        new String[]{HEAD_NODE_ID, TAIL_NODE_ID}))
                .check(createSlaveTunnelPathInput);
    }

    /**
     * Check UpdateTunnelPathInput.
     *
     * @param updateTunnelPathInput updateTunnelPathInput
     * @return result
     */
    public static boolean check(UpdateTunnelPathInput updateTunnelPathInput) {
        return new CheckChain<UpdateTunnelPathInput>()
                .set(input -> checkNotNull(input, "UpdateTunnelPathInput"))
                .set(input -> checkNotNull(
                        new Object[]{input.getHeadNodeId(), input.getTunnelId()},
                        new String[]{HEAD_NODE_ID, TUNNEL_ID}))
                .set(input -> checkAffinityStrategy(input.getAffinityStrategy()))
                .check(updateTunnelPathInput);
    }

    /**
     * Check CreateTunnelHsbPathInput.
     *
     * @param createTunnelHsbPathInput createTunnelHsbPathInput
     * @return result
     */
    public static boolean check(CreateTunnelHsbPathInput createTunnelHsbPathInput) {
        return new CheckChain<CreateTunnelHsbPathInput>()
                .set(input -> checkNotNull(input, "CreateTunnelHsbPathInput"))
                .set(input -> checkNotNull(
                        new Object[]{input.getHeadNodeId(), input.getTailNodeId()},
                        new String[]{HEAD_NODE_ID, TAIL_NODE_ID}))
                .set(input -> checkAffinityStrategy(input.getAffinityStrategy()))
                .check(createTunnelHsbPathInput);
    }

    /**
     * Check UpdateTunnelHsbPathInput.
     *
     * @param updateTunnelHsbPathInput updateTunnelHsbPathInput
     * @return result
     */
    public static boolean check(UpdateTunnelHsbPathInput updateTunnelHsbPathInput) {
        return new CheckChain<UpdateTunnelHsbPathInput>()
                .set(input -> checkNotNull(input, "UpdateTunnelHsbPathInput"))
                .set(input -> checkNotNull(
                        new Object[]{input.getHeadNodeId(), input.getTunnelId()},
                        new String[]{HEAD_NODE_ID, TUNNEL_ID}))
                .set(input -> checkAffinityStrategy(input.getAffinityStrategy()))
                .set(input -> checkAffinityStrategy(Optional.ofNullable(input.getSlaveTeArgument())
                                                            .map(Affinity::getAffinityStrategy).orElse(null)))
                .check(updateTunnelHsbPathInput);
    }

    /**
     * Check GetMaxAvailableBandwidthInput.
     *
     * @param getMaxAvailableBandwidthInput getMaxAvailableBandwidthInput
     * @return result
     */
    public static boolean check(GetMaxAvailableBandwidthInput getMaxAvailableBandwidthInput) {
        return new CheckChain<GetMaxAvailableBandwidthInput>()
                .set(input -> checkNotNull(input, "GetMaxAvailableBandwidthInput"))
                .set(input -> checkNotNull(
                        new Object[]{input.getHeadNodeId(), input.getTailNodeId()},
                        new String[]{HEAD_NODE_ID, TAIL_NODE_ID}))
                .set(RpcInputChecker::checkGetMaxAvailableBandwidthInputValidation)
                .check(getMaxAvailableBandwidthInput);
    }

    public static boolean check(CreateServiceInput createServiceInput) {
        return new CheckChain<CreateServiceInput>().set(input -> checkNotNull(input, "CreateServiceInput"))
                .set(input -> checkNextAddress(input.getNextAddress()))
                .set(input -> checkNotNull(
                        new Object[]{input.getHeadNodeId(), input.getTailNodeId(), input.getServiceName()},
                        new String[]{HEAD_NODE_ID, TAIL_NODE_ID, SERVICE_NAME})).check(createServiceInput);
    }

    private static boolean checkNextAddress(List<NextAddress> nextAddressList) {
        return nextAddressList != null && !nextAddressList.isEmpty();
    }

    public static boolean check(UpdateServiceInput createServiceInput) {
        return new CheckChain<UpdateServiceInput>().set(input -> checkNotNull(input, "UpdateServiceInput"))
                .set(input -> checkNextAddress(input.getNextAddress()))
                .set(input -> checkNotNull(
                        new Object[]{input.getHeadNodeId(), input.getServiceName()},
                        new String[]{HEAD_NODE_ID, SERVICE_NAME})).check(createServiceInput);
    }

    public static boolean check(RemoveServiceInput removeServiceInput) {
        return new CheckChain<RemoveServiceInput>().set(input -> checkNotNull(input, "RemoveTunnelPathInput"))
                .set(input -> checkNotNull(
                        new Object[]{input.getHeadNodeId(), input.getServiceName()},
                        new String[]{HEAD_NODE_ID, SERVICE_NAME})).check(removeServiceInput);
    }

    public static boolean check(AdjustTunnelBandwidthInput adjustTunnelBandwidthInput) {
        return new CheckChain<AdjustTunnelBandwidthInput>()
                .set(input -> checkNotNull(input, "AdjustTunnelBandwidthInput")).set(input -> checkNotNull(
                        new Object[]{adjustTunnelBandwidthInput.getPathAdjustRequest()},
                        new String[]{"Path request List"}))
                .check(adjustTunnelBandwidthInput);
    }

    public static boolean check(PathAdjustRequest pathAdjustRequest) {
        return new CheckChain<PathAdjustRequest>().set(input -> checkNotNull(input, "PathAdjustRequest"))
                .set(input -> checkNotNull(
                        new Object[]{input.getHeadNodeId(), input.getTunnelId(), pathAdjustRequest.getBandwidth(),
                                pathAdjustRequest.getChangeType()},
                        new String[]{HEAD_NODE_ID, TAIL_NODE_ID, "Bandwidth", "ChangeType"}))
                .check(pathAdjustRequest);
    }

    public static boolean check(GetRealtimePathInput getRealtimePathInput) {
        return new CheckChain<GetRealtimePathInput>().set(input -> checkNotNull(input, "GetRealtimePathInput"))
                .set(input -> checkNotNull(new Object[]{input.getHeadNodeId()}, new String[]{HEAD_NODE_ID}))
                .set(input -> checkAnyNotNull(
                        new Object[]{input.getTunnelId(), input.getTailNodeId()},
                        new String[]{TUNNEL_ID, TAIL_NODE_ID}))
                .set(input -> checkTunnelIdValid(input.getTunnelId()))
                .check(getRealtimePathInput);
    }

    private static boolean checkAnyNotNull(Object[] objects, String[] names) {
        for (Object object : objects) {
            if (Objects.nonNull(object)) {
                return true;
            }
        }
        Logs.error(LOG, "{} are all null", Arrays.toString(names));
        return false;
    }

    private static boolean checkTunnelIdValid(Long tunnelId) {
        if (tunnelId == null) {
            return true;
        }
        if (tunnelId <= 0) {
            Logs.error(LOG, "Tunnel Id should not be less than 0");
            return false;
        }
        return true;
    }

    private static boolean checkGetMaxAvailableBandwidthInputValidation(GetMaxAvailableBandwidthInput input) {
        return !Conditions.or(
                Conditions.anyOneNonNull(input.getTryToAvoidLink(), input.getPreemptPriority(),
                                         input.getHoldPriority()),
                Objects.nonNull(input.getBandwidth()) && input.getBandwidth() != 0);
    }

    private static class CheckChain<T> {
        private List<Predicate<T>> chain = new ArrayList<>();

        public CheckChain<T> set(Predicate<T> criterion) {
            chain.add(criterion);
            return this;
        }

        public boolean check(T target) {
            for (Predicate<T> criterion : chain) {
                if (!criterion.test(target)) {
                    return false;
                }
            }
            return true;
        }
    }
}
