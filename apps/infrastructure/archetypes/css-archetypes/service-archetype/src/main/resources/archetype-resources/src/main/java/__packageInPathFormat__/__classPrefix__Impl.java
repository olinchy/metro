#set($symbol_pound='#')
#set($symbol_dollar='$')
#set($symbol_escape='\' )
/*
 * ${copyright} and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package ${package};

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ${classPrefix}Impl implements AutoCloseable, ${classPrefix} {
    private static final Logger LOG = LoggerFactory.getLogger(${classPrefix}Impl.class);

    @Override
    public void close() throws Exception {
        LOG.info("${classPrefix}Impl Closed");
    }
}
