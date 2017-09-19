/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.bandwidth;

import com.zte.ngip.ipsdn.pce.path.api.util.TunnelUnifyKey;

public class TunnelKeyBw {
    public TunnelKeyBw(TunnelUnifyKey key, long demandBw) {
        this.key = key;
        this.demandBw = demandBw;
    }

    public TunnelKeyBw(TunnelKeyBw source) {
        this.demandBw = source.demandBw;
        setKey(new TunnelUnifyKey(source.getTunnelUnifyKey()));
    }

    TunnelUnifyKey key;
    long demandBw;

    /**
     * set key for this class.
     *
     * @param key set key for this class
     */
    public void setKey(TunnelUnifyKey key) {
        this.key = key;
    }

    public TunnelUnifyKey getTunnelUnifyKey() {
        return this.key;
    }

    public long getBw() {
        return this.demandBw;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (int) (demandBw ^ (demandBw >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object ob) {
        if (this == ob) {
            return true;
        }
        if (ob == null || getClass() != ob.getClass()) {
            return false;
        }

        TunnelKeyBw that = (TunnelKeyBw) ob;

        if (demandBw != that.demandBw) {
            return false;
        }
        return key != null ? key.equals(that.key) : that.key == null;
    }
}