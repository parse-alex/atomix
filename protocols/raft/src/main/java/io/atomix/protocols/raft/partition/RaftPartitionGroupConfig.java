/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.protocols.raft.partition;

import io.atomix.primitive.partition.PartitionGroup;
import io.atomix.primitive.partition.PartitionGroupConfig;
import io.atomix.storage.StorageLevel;
import io.atomix.utils.memory.MemorySize;

import java.util.HashSet;
import java.util.Set;

/**
 * Raft partition group configuration.
 */
public class RaftPartitionGroupConfig extends PartitionGroupConfig<RaftPartitionGroupConfig> {
  private static final int DEFAULT_PARTITIONS = 7;
  private static final String DATA_PREFIX = ".data";

  private Set<String> members = new HashSet<>();
  private int partitionSize;
  private RaftStorageConfig storageConfig = new RaftStorageConfig();
  private RaftCompactionConfig compactionConfig = new RaftCompactionConfig();

  @Override
  public PartitionGroup.Type getType() {
    return RaftPartitionGroup.TYPE;
  }

  @Override
  protected int getDefaultPartitions() {
    return DEFAULT_PARTITIONS;
  }

  /**
   * Returns the set of members in the partition group.
   *
   * @return the set of members in the partition group
   */
  public Set<String> getMembers() {
    return members;
  }

  /**
   * Sets the set of members in the partition group.
   *
   * @param members the set of members in the partition group
   * @return the Raft partition group configuration
   */
  public RaftPartitionGroupConfig setMembers(Set<String> members) {
    this.members = members;
    return this;
  }

  /**
   * Returns the partition size.
   *
   * @return the partition size
   */
  public int getPartitionSize() {
    return partitionSize;
  }

  /**
   * Sets the partition size.
   *
   * @param partitionSize the partition size
   * @return the Raft partition group configuration
   */
  public RaftPartitionGroupConfig setPartitionSize(int partitionSize) {
    this.partitionSize = partitionSize;
    return this;
  }

  /**
   * Returns the storage configuration.
   *
   * @return the storage configuration
   */
  public RaftStorageConfig getStorageConfig() {
    return storageConfig;
  }

  /**
   * Sets the storage configuration.
   *
   * @param storageConfig the storage configuration
   * @return the Raft partition group configuration
   */
  public RaftPartitionGroupConfig setStorageConfig(RaftStorageConfig storageConfig) {
    this.storageConfig = storageConfig;
    return this;
  }

  /**
   * Returns the compaction configuration.
   *
   * @return the compaction configuration
   */
  public RaftCompactionConfig getCompactionConfig() {
    return compactionConfig;
  }

  /**
   * Sets the compaction configuration.
   *
   * @param compactionConfig the compaction configuration
   * @return the Raft partition group configuration
   */
  public RaftPartitionGroupConfig setCompactionConfig(RaftCompactionConfig compactionConfig) {
    this.compactionConfig = compactionConfig;
    return this;
  }
}
