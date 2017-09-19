/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.realtimepath;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.h.control.rev170601.GetDomainPathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.GetRealtimePathInput;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.pce.path.rev150814.bw.shared.group.info.BwSharedGroupContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth.BwSharedGroupMng;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.pathchooser.PathChooserFactory;
import com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result.PathResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.PathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PcePathProvider;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.servicepath.DefaultBidirectImpl;
import com.zte.mw.sdn.components.path.calculator.borrowed.tunnelpath.ITunnel;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.ZeroBandwidthUtils;

import com.zte.ngip.ipsdn.pce.path.api.util.ComUtility;
import com.zte.ngip.ipsdn.pce.path.api.util.Logs;
import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;
import com.zte.ngip.ipsdn.pce.path.core.TeArgumentBean;
import com.zte.ngip.ipsdn.pce.path.core.transformer.MetricTransformer;

public class RealtimePathInstance extends BaseRealtimePathInstance {
    public RealtimePathInstance(GetRealtimePathInput input) {
        super(input);
    }

    public RealtimePathInstance(GetRealtimePathInput input, ITunnel existedTunnel) {
        super(input, existedTunnel);
    }

    public RealtimePathInstance(GetDomainPathInput input, TunnelUnifyKey serviceKey) {
        super();
        this.headNodeId = NodeId.getDefaultInstance(input.getHeadNodeId().getValue());
        this.tailNodeId = NodeId.getDefaultInstance(input.getTailNodeId().getValue());
        this.topoId = (input.getTopologyId() != null)
                ? input.getTopologyId() : TopologyId.getDefaultInstance(ComUtility.DEFAULT_TOPO_ID_STRING);
        this.teArg = new TeArgumentBean(input, this.topoId);
        setCalculateStrategy(input.getCalculateStrategyContainer());
        setRecalcWithoutDelay(input.isRecalcWithoutDelay());
        this.multiplePathsParam = input.getMultiplePathsParam();
        this.tunnelUnifyKey = serviceKey;
        this.biDirect = input.getBiDirectContainer() == null ? null : new DefaultBidirectImpl(input);
        this.enableZeroBandwidth = Optional.ofNullable(input.isEnableZeroBandwidth()).orElse(false);
        this.pathChooserName = PathChooserFactory.PathChooserName.DOMAIN_PATH_CHOOSER;
    }

    private static final Logger LOG = LoggerFactory.getLogger(RealtimePathInstance.class);
    private List<PathResult> pathResultList = new ArrayList<>();

    private static void checkPriority(Short newPriority, byte oldPriority) {

        if (Optional.ofNullable(newPriority).orElse((short) 7).byteValue() != oldPriority) {
            Logs.error(LOG, "Priority should not be changed.");
        }
    }

    @Override
    void setRealtimePathTeArgBean(GetRealtimePathInput input, ITunnel existedTunnel) {
        checkPriority(input.getHoldPriority(), existedTunnel.getTeArgumentBean().getHoldPriority());
        checkPriority(input.getPreemptPriority(), existedTunnel.getTeArgumentBean().getPreemptPriority());
        this.teArg = new TeArgumentBean(input, this.topoId);
    }

    public PceResult calcPath() {
        PathProvider<MetricTransformer> pathProvider = generateCommonRealtimePathProvider();
        return calcPath(pathProvider);
    }

    private PceResult calcPath(PathProvider<MetricTransformer> pathProvider) {
        PceResult result = new PceResult();
        pathProvider.calcPath(result);
        if (multiplePathsParam != null) {
            pathResultList.addAll(pathProvider.getPathResultList());
            if (needRecalcWithZeroBandwidth()) {
                result = recalcWithZeroBandwidth(pathProvider);
            }
            Logs.info(LOG, "calc real time path over {}",
                      pathResultList.stream().map(PathResult::getPath).map(ComUtility::pathToString)
                              .collect(Collectors.toList()));
        } else {
            path = pathProvider.getPath();
            LOG.info("calc real time path over {}", path);
            lspMetric = pathProvider.getLspMetric();
            lspDelay = pathProvider.getLspDelay();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private PathProvider<MetricTransformer> generateCommonRealtimePathProvider() {
        PathProvider<MetricTransformer> pathProvider = new PathProvider(headNodeId, tunnelUnifyKey, tailNodeId, topoId,
                                                                        generateCalculateStrategy(),
                                                                        generateTransformerFactory());
        pathProvider.setTeArgWithBuildNew(teArg);
        pathProvider.setIsRealTimePath(true);
        pathProvider.setRecalc(recalcWithoutDelay);
        pathProvider.setMultiplePathsParam(this.multiplePathsParam);
        pathProvider.setBiDirect(biDirect);
        pathProvider.setPathChooserName(pathChooserName);
        return pathProvider;
    }

    private boolean needRecalcWithZeroBandwidth() {
        Logs.debug(LOG, "needRecalcWithZeroBandwidth? isCalcFail={}, ZeroBandWidthFlag={}, enableZeroBandwidth={}",
                   pathResultList.get(0).getPceResult().isCalcFail(),
                   PcePathProvider.getInstance().getZeroBandWidthFlag(),
                   enableZeroBandwidth);
        return pathResultList.get(0).getPceResult().isCalcFail() && PcePathProvider.getInstance().getZeroBandWidthFlag()
                && enableZeroBandwidth;
    }

    private PceResult recalcWithZeroBandwidth(PathProvider<MetricTransformer> pathProvider) {
        TeArgumentBean oldTeArg = pathProvider.getTeArg();
        if (oldTeArg.getBandWidth() == 0) {
            return PceResult.create();
        }
        TeArgumentBean zeroBwTeArg = ZeroBandwidthUtils.generateZeroBandwidthTeArg(oldTeArg);

        Logs.info(LOG, "recalcWithZeroBandwidth");
        pathProvider.setTeArg(zeroBwTeArg);
        enableZeroBandwidth = false;
        PceResult result = new PceResult();
        pathProvider.calcPath(result);
        pathResultList.clear();
        pathResultList.addAll(pathProvider.getPathResultList());
        zeroBandwidthPath = true;
        return result;
    }

    /**
     * Call this method when {@link GetRealtimePathInput} contains a valid tunnelId.
     *
     * @param oldBwGroup oldBwGroup
     * @param newBwGroup newBwGroup
     * @return PceResult
     */
    public PceResult calcPath(BwSharedGroupContainer oldBwGroup, BwSharedGroupContainer newBwGroup) {
        PathProvider<MetricTransformer> pathProvider = generateCommonRealtimePathProvider();
        pathProvider.setBwSharedGroups(oldBwGroup, BwSharedGroupMng.getDeletedGroups(newBwGroup, oldBwGroup));
        return calcPath(pathProvider);
    }

    public boolean isZeroBandwidthPath() {
        return zeroBandwidthPath;
    }

    public List<Link> getLsp() {
        return path;
    }

    public TopologyId getTopoId() {
        return topoId;
    }

    public List<PathResult> getPathResultList() {
        return pathResultList;
    }
}
