/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.kerby.kerberos.kdc.identitybackend;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * utility class for ZooKeeper
 */
public class ZKUtil {
    public static final char ZNODE_PATH_SEPARATOR = '/';
    private static final Logger LOG = LoggerFactory.getLogger(ZKUtil.class);

    public static String joinZNode(String prefix, String suffix) {
        return prefix + ZNODE_PATH_SEPARATOR + suffix;
    }

    /**
     * Check if the specified node exists. Sets no watches.
     */
    public static int checkExists(ZooKeeper zk, String node)
        throws KeeperException {
        try {
            Stat s = zk.exists(node, null);
            return s != null ? s.getVersion() : -1;
        } catch (KeeperException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    /**
     * Sets the data of the existing znode to be the specified data.
     */
    public static boolean setData(ZooKeeper zk, String node, byte[] data)
        throws KeeperException {
        try {
            return zk.setData(node, data, -1) != null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Set data into node creating node if it doesn't yet exist.
     * Does not set watch.
     */
    public static void createSetData(final ZooKeeper zk, final String node,
                                     final byte[] data)
        throws KeeperException {
        if (checkExists(zk, node) == -1) {
            ZKUtil.createWithParents(zk, node, data);
        } else {
            ZKUtil.setData(zk, node, data);
        }
    }

    /**
     * Creates the specified node and all parent nodes required for it to exist.
     */
    public static void createWithParents(ZooKeeper zk, String node)
        throws KeeperException {
        createWithParents(zk, node, new byte[0]);
    }

    /**
     * Creates the specified node and all parent nodes required for it to exist.  The creation of
     * parent znodes is not atomic with the leafe znode creation but the data is written atomically
     * when the leaf node is created.
     */
    public static void createWithParents(ZooKeeper zk, String node, byte[] data)
        throws KeeperException {
        try {
            if (node == null) {
                return;
            }
            zk.create(node, data, createACL(zk, node),
                CreateMode.PERSISTENT);
        } catch (KeeperException.NodeExistsException nee) {
            return;
        } catch (KeeperException.NoNodeException nne) {
            createWithParents(zk, getParent(node));
            createWithParents(zk, node, data);
        } catch (InterruptedException ie) {

        }
    }

    /**
     * Returns the ACL list
     */
    private static ArrayList<ACL> createACL(ZooKeeper zk, String node) {
        return ZooDefs.Ids.OPEN_ACL_UNSAFE;//TODO
    }

    /**
     * Returns the full path of the immediate parent of the specified node.
     * null if passed the root node or an invalid node
     */
    public static String getParent(String node) {
        int idx = node.lastIndexOf(ZNODE_PATH_SEPARATOR);
        return idx <= 0 ? null : node.substring(0, idx);
    }

    /**
     * Get znode data. Does not set a watcher.
     */
    public static byte[] getData(ZooKeeper zk, String node)
        throws KeeperException, InterruptedException {
        try {
            byte[] data = zk.getData(node, false, null);
            return data;
        } catch (KeeperException.NoNodeException e) {
            LOG.debug("Unable to get data of znode " + node + " because node does not exist");
            return null;
        } catch (KeeperException e) {
            LOG.warn("Unable to get data of znode " + node, e);
            return null;
        }
    }

    /**
     * Lists the children of the specified node without setting any watches.
     * null if parent does not exist
     */
    public static List<String> listChildrenNoWatch(ZooKeeper zk, String node)
            throws KeeperException {
        List<String> children = null;
        try {
            // List the children without watching
            children = zk.getChildren(node, null);
        } catch (KeeperException.NoNodeException nne) {
            return null;
        } catch (InterruptedException ie) {

        }
        return children;
    }
}