/*
 * Copyright (c) 2016 Zte Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.zte.mw.sdn.components.path.calculator.borrowed.level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.device.manager.rev150915.NodeType;
import org.opendaylight.yang.gen.v1.urn.zte.ngip.ipsdn.inventory.rev150814.IpsdnNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zte.mw.sdn.components.path.calculator.borrowed.provider.DbProvider;

/**
 * Created by 10167095 on 12/16/15.
 */
public class LevelProvider {
    private LevelProvider() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(LevelProvider.class);
    private static LevelProvider instance = new LevelProvider();
    private Map<NodeId, Long> topoLevel = new HashMap<>();

    public static LevelProvider getInstance() {
        return instance;
    }

    public static void reset() {
        instance = new LevelProvider();
    }

    /*powermock can not with databroker*/
    public void setOnTest(Map<NodeId, Long> map) {
        topoLevel = map;
    }

    public Map<NodeId, Long> getTopoLevel() {
        return topoLevel;
    }

    public void init() {
        getDbTopoLevel();
        DbProvider.getInstance().registerDataChangeListener(
                InstanceIdentifier.builder(Nodes.class).build(),
                new NodesChange());
    }

    private void getDbTopoLevel() {
        Nodes nodes;
        InstanceIdentifier<Nodes> path = InstanceIdentifier.create(Nodes.class);
        nodes = DbProvider.getInstance().readConfigrationData(path);

        if (nodes == null) {
            LOG.error("read TopoLevel error from DB.");
            return;
        }

        for (Node node : nodes.getNode()) {
            addNodeLevel(node);
        }
    }

    private void addNodeLevel(Node node) {
        if ((null == node) || (null == node.getAugmentation(IpsdnNode.class))) {
            return;
        }
        LOG.info("addNodeLevel:" + node.getKey().toString());

        NodeId nodeId = new NodeId(node.getId().getValue());
        NodeType level = node.getAugmentation(IpsdnNode.class).getNodeType();

        if (level != null) {
            topoLevel.put(nodeId, (long) level.getIntValue());
            LOG.info("addNodeLevel:" + " level-" + level.getIntValue());
        }
    }

    @Override
    public String toString() {
        String str = "";

        for (Map.Entry<NodeId, Long> entry : topoLevel.entrySet()) {
            str += "========================" + "\n";
            str += entry.getKey().toString() + "\n";
            str += entry.getValue().toString() + "\n";
        }

        return str;
    }

    public String toString(String nodeId) {
        String str = "";

        Long level = topoLevel.get(NodeId.getDefaultInstance(nodeId));
        if (level == null) {
            return str;
        }

        str += level.toString() + "\n";
        return str;
    }

    private class NodesChange implements DataChangeListener {
        @Override
        public void onDataChanged(
                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> arg0) {
            onCreatedData(arg0.getCreatedData());
            onUpdateData(arg0.getOriginalData(), arg0.getUpdatedData());
            onRemovedData(arg0.getOriginalData(), arg0.getRemovedPaths());
        }

        private void onCreatedData(
                Map<InstanceIdentifier<?>, DataObject> createdData) {
            for (Map.Entry<?, ?> entry : createdData.entrySet()) {
                if (entry.getValue() instanceof Node) {
                    addNodeLevel((Node) entry.getValue());
                }
            }
        }

        private void onUpdateData(
                Map<InstanceIdentifier<?>, DataObject> originData,
                Map<InstanceIdentifier<?>, DataObject> updatedData) {
            for (Map.Entry<?, ?> entry : updatedData.entrySet()) {
                if (entry.getValue() instanceof Node) {
                    updateNodeLevel((Node) originData.get(entry.getKey()), (Node) entry.getValue());
                }
            }
        }

        private void onRemovedData(
                Map<InstanceIdentifier<?>, DataObject> originData,
                Set<InstanceIdentifier<?>> removedData) {
            for (InstanceIdentifier<?> key : removedData) {
                if (originData.get(key) instanceof Node) {
                    removeNodeLevel((Node) originData.get(key));
                }
            }
        }

        private void updateNodeLevel(Node originNode, Node updateNode) {
            LOG.info("updateNodeLevel:" + updateNode.getKey().toString());

            NodeType oldLevel = getNodeType(originNode);
            NodeType newLevel = getNodeType(updateNode);

            if (!isLevelNeedUpd(oldLevel, newLevel)) {
                return;
            }

            NodeId nodeId = new NodeId(updateNode.getId().getValue());

            if (topoLevel.containsKey(nodeId)) {
                topoLevel.remove(nodeId);
            }

            if (newLevel != null) {
                topoLevel.put(nodeId, (long) newLevel.getIntValue());
                LOG.info("updateNodeLevel:" + " newlevel-" + newLevel.getIntValue());
            }
        }

        private void removeNodeLevel(Node node) {
            if (node == null) {
                return;
            }

            LOG.info("removeNodeLevel:" + node.getKey().toString());

            NodeId nodeId = new NodeId(node.getId().getValue());
            topoLevel.remove(nodeId);
        }

        private NodeType getNodeType(Node node) {
            if ((null == node) || (null == node.getAugmentation(IpsdnNode.class))) {
                return null;
            }

            return node.getAugmentation(IpsdnNode.class).getNodeType();
        }

        private boolean isLevelNeedUpd(NodeType oldLevel, NodeType newLevel) {
            return !Optional.ofNullable(oldLevel).map(old -> old.equals(newLevel)).orElse(newLevel == null);
        }
    }
}
