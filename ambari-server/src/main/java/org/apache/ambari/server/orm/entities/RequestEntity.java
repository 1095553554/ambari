/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.orm.entities;

import org.apache.ambari.server.actionmanager.RequestType;

import javax.persistence.*;
import java.util.Collection;

@Table(name = "request")
@Entity
public class RequestEntity {

  @Column(name = "request_id")
  @Id
  private Long requestId;

  @Column(name = "cluster_id", updatable = false, insertable = false)
  @Basic
  private Long clusterId;

  @Column(name = "request_schedule_id", updatable = false, insertable = false, nullable = true)
  @Basic
  private Long requestScheduleId;

  @Column(name = "request_context")
  @Basic
  private String requestContext;

  @Column(name = "command_name")
  @Basic
  private String commandName;

  @Column(name = "inputs", length = 32000)
  @Basic
  private String inputs;

  @Column(name = "target_service")
  @Basic
  private String targetService;

  @Column(name = "target_component")
  @Basic
  private String targetComponent;

  @Column(name = "target_hosts")
  @Lob
  private String targetHosts;

  @Column(name = "request_type")
  @Enumerated(value = EnumType.STRING)
  private RequestType requestType;

  @Column(name = "status")
  private String status;

  @Basic
  @Column(name = "create_time", nullable = false)
  private Long createTime = System.currentTimeMillis();

  @Basic
  @Column(name = "start_time", nullable = false)
  private Long startTime = -1L;

  @Basic
  @Column(name = "end_time", nullable = false)
  private Long endTime = -1L;

  @OneToMany(mappedBy = "request")
  private Collection<StageEntity> stages;

  @ManyToOne(cascade = {CascadeType.MERGE})
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id")
  private ClusterEntity cluster;

  @ManyToOne(cascade = {CascadeType.MERGE})
  @JoinColumn(name = "request_schedule_id", referencedColumnName = "schedule_id")
  private RequestScheduleEntity requestScheduleEntity;

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long id) {
    this.requestId = id;
  }

  public String getRequestContext() {
    return requestContext;
  }

  public void setRequestContext(String request_context) {
    this.requestContext = request_context;
  }

  public Collection<StageEntity> getStages() {
    return stages;
  }

  public void setStages(Collection<StageEntity> stages) {
    this.stages = stages;
  }

  public ClusterEntity getCluster() {
    return cluster;
  }

  public void setCluster(ClusterEntity cluster) {
    this.cluster = cluster;
  }

  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public String getInputs() {
    return inputs;
  }

  public void setInputs(String inputs) {
    this.inputs = inputs;
  }

  public String getTargetService() {
    return targetService;
  }

  public void setTargetService(String targetService) {
    this.targetService = targetService;
  }

  public String getTargetComponent() {
    return targetComponent;
  }

  public void setTargetComponent(String targetComponent) {
    this.targetComponent = targetComponent;
  }

  public String getTargetHosts() {
    return targetHosts;
  }

  public void setTargetHosts(String targetHosts) {
    this.targetHosts = targetHosts;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public void setRequestType(RequestType requestType) {
    this.requestType = requestType;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public RequestScheduleEntity getRequestScheduleEntity() {
    return requestScheduleEntity;
  }

  public void setRequestScheduleEntity(RequestScheduleEntity requestScheduleEntity) {
    this.requestScheduleEntity = requestScheduleEntity;
  }

  public Long getRequestScheduleId() {
    return requestScheduleId;
  }

  public void setRequestScheduleId(Long scheduleId) {
    this.requestScheduleId = scheduleId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestEntity that = (RequestEntity) o;

    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return requestId != null ? requestId.hashCode() : 0;
  }
}
