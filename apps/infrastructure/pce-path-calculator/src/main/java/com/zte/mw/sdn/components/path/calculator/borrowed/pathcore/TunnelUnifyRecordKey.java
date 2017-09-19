/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.pathcore;

import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

/**
 * Created by 10167095 on 2/23/16.
 */
public class TunnelUnifyRecordKey extends TunnelUnifyKey {
    public TunnelUnifyRecordKey(TunnelUnifyKey tunnelUnifyKey) {
        this(tunnelUnifyKey, tunnelUnifyKey.isReverse());
    }

    public TunnelUnifyRecordKey(TunnelUnifyKey tunnelUnifyKey, boolean isReverse) {
        super(tunnelUnifyKey.getHeadNode(), tunnelUnifyKey.getTunnelId());
        this.isTg = tunnelUnifyKey.isTg();
        this.isMaster = tunnelUnifyKey.isMaster();
        this.tgId = tunnelUnifyKey.getTgId();
        this.tunnelId = tunnelUnifyKey.getTunnelId();
        this.hsbFlag = tunnelUnifyKey.isHsbFlag();
        this.isSimulateFlag = tunnelUnifyKey.isSimulate();
        this.isBiDirectional = tunnelUnifyKey.isBiDirectional();
        this.isReverse = isReverse;
        this.serviceName = tunnelUnifyKey.getServiceName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((headNode == null) ? 0 : headNode.hashCode());
        result = prime * result + (isMaster ? 1231 : 1237);
        result = prime * result + (isTg ? 1231 : 1237);
        result = prime * result + tgId;
        result = prime * result + tunnelId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return isTunnelUnifyRecordKeyEqual((TunnelUnifyRecordKey) obj);
    }

    private boolean isTunnelUnifyRecordKeyEqual(TunnelUnifyRecordKey other) {
        if (!isHeadEqual(other)) {
            return false;
        }
        if (!isHsbEqual(other)) {
            return false;
        }
        if (!isIdEqual(other)) {
            return false;
        }
        if (!isBiDirectionalEqual(other)) {
            return false;
        }
        return isSimulateFlag == other.isSimulateFlag;
    }

    private boolean isHeadEqual(TunnelUnifyRecordKey other) {
        if (headNode == null) {
            if (other.headNode != null) {
                return false;
            }
        } else if (!headNode.equals(other.headNode)) {
            return false;
        }
        return true;
    }

    private boolean isHsbEqual(TunnelUnifyRecordKey other) {
        if (isMaster != other.isMaster) {
            return false;
        }
        if (isTg != other.isTg) {
            return false;
        }
        return true;
    }

    private boolean isIdEqual(TunnelUnifyRecordKey other) {
        if (tgId != other.tgId) {
            return false;
        }
        if (tunnelId != other.tunnelId) {
            return false;
        }
        if (!isServiceNameEqual(other)) {
            return false;
        }
        return true;
    }

    private boolean isBiDirectionalEqual(TunnelUnifyRecordKey other) {
        if (isBiDirectional != other.isBiDirectional) {
            return false;
        }
        if (isReverse != other.isReverse) {
            return false;
        }
        return true;
    }
}
