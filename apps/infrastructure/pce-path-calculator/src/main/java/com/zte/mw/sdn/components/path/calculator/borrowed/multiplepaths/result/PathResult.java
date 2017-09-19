/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.multiplepaths.result;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import com.zte.mw.sdn.components.path.calculator.borrowed.pathcore.LspAttributes;
import com.zte.mw.sdn.components.path.calculator.borrowed.provider.PceResult;
import com.zte.mw.sdn.components.path.calculator.borrowed.util.PceUtil;

import com.zte.ngip.ipsdn.pce.path.api.srlg.SrlgAttribute;
import com.zte.ngip.ipsdn.pce.path.api.util.CollectionUtils;
import com.zte.ngip.ipsdn.pce.path.core.result.Path;

/**
 * Created by 10204924 on 2017/2/8.
 */
public final class PathResult implements Path<Link> {
    private List<Link> path = new LinkedList<>();
    private PceResult pceResult = new PceResult();
    private LspAttributes lspAttributes = new LspAttributes();

    @Override
    public List<Link> getPath() {
        return Optional.ofNullable(path).orElse(Collections.emptyList());
    }

    public void setPath(List<Link> path) {
        this.path = path;
        calcLspAttributes();
    }

    private void calcLspAttributes() {
        lspAttributes = PceUtil.calcLspAttributes(path);
        if (CollectionUtils.isNullOrEmpty(path)) {
            pceResult.setCalcFail(true);
        }
    }

    public PceResult getPceResult() {
        return pceResult;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, pceResult);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PathResult that = (PathResult) obj;
        return Objects.equals(path, that.path) && Objects.equals(pceResult, that.pceResult);
    }

    @Override
    public String toString() {
        return "PathResult{" + "path=" + path + ", pceResult=" + pceResult + ", lspAttributes=" + lspAttributes + '}';
    }

    public long getLspMetric() {
        return lspAttributes.getLspMetric();
    }

    public long getLspDelay() {
        return lspAttributes.getLspDelay();
    }

    public SrlgAttribute getSrlgAttr() {
        return lspAttributes.getSrlgAttr();
    }
}
