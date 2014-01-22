/**
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
package org.apache.ambari.server.actionmanager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.persist.UnitOfWork;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.serveraction.ServerActionManager;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;


/**
 * This class acts as the interface for action manager with other components.
 */
@Singleton
public class ActionManager {
  private static Logger LOG = LoggerFactory.getLogger(ActionManager.class);
  private final ActionScheduler scheduler;
  private final ActionDBAccessor db;
  private final ActionQueue actionQueue;
  private final AtomicLong requestCounter;
  private final RequestFactory requestFactory;


  @Inject
  public ActionManager(@Named("schedulerSleeptime") long schedulerSleepTime,
                       @Named("actionTimeout") long actionTimeout,
                       ActionQueue aq, Clusters fsm, ActionDBAccessor db, HostsMap hostsMap,
                       ServerActionManager serverActionManager, UnitOfWork unitOfWork,
                       RequestFactory requestFactory, Configuration configuration) {
    this.actionQueue = aq;
    this.db = db;
    scheduler = new ActionScheduler(schedulerSleepTime, actionTimeout, db,
        actionQueue, fsm, 2, hostsMap, serverActionManager, unitOfWork, configuration);
    requestCounter = new AtomicLong(
        db.getLastPersistedRequestIdWhenInitialized());
    this.requestFactory = requestFactory;
  }

  public void start() {
    LOG.info("Starting scheduler thread");
    scheduler.start();
  }

  public void shutdown() {
    scheduler.stop();
  }

  public void sendActions(List<Stage> stages, ExecuteActionRequest actionRequest) throws AmbariException {
    Request request = requestFactory.createNewFromStages(stages, actionRequest);
    sendActions(request, actionRequest);
  }

  public void sendActions(Request request, ExecuteActionRequest executeActionRequest) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("Persisting Request into DB: %s", request));

      if (executeActionRequest != null) {
        LOG.debug("In response to request: " + request.toString());
      }
    }
    db.persistActions(request);
    scheduler.awake();
  }

  public List<Request> getRequests(Collection<Long> requestIds) {
    return db.getRequests(requestIds);
  }

  public List<Stage> getRequestStatus(long requestId) {
    return db.getAllStages(requestId);
  }

  public Stage getAction(long requestId, long stageId) {
    return db.getStage(StageUtils.getActionId(requestId, stageId));
  }

  /**
   * Get all actions(stages) for a request.
   *
   * @param requestId the request id
   * @return list of all stages associated with the given request id
   */
  public List<Stage> getActions(long requestId) {
    return db.getAllStages(requestId);
  }

  /**
   * Persists command reports into the db
   */
  public void processTaskResponse(String hostname, List<CommandReport> reports) {
    if (reports == null) {
      return;
    }
    //persist the action response into the db.
    for (CommandReport report : reports) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Processing command report : " + report.toString());
      }
      String actionId = report.getActionId();
      long[] requestStageIds = StageUtils.getRequestStage(actionId);
      long requestId = requestStageIds[0];
      long stageId = requestStageIds[1];
      HostRoleCommand command = db.getTask(report.getTaskId());
      if (command == null) {
        LOG.warn("The task " + report.getTaskId()
            + " is invalid");
        continue;
      }
      if (!command.getStatus().equals(HostRoleStatus.IN_PROGRESS)
          && !command.getStatus().equals(HostRoleStatus.QUEUED)) {
        LOG.warn("The task " + command.getTaskId()
            + " is not in progress, ignoring update");
        continue;
      }
      db.updateHostRoleState(hostname, requestId, stageId, report.getRole(),
          report);
    }
  }

  public void handleLostHost(String host) {
    //Do nothing, the task will timeout anyway.
    //The actions can be failed faster as an optimization
    //if action timeout happens to be much larger than
    //heartbeat timeout.
  }

  public long getNextRequestId() {
    return requestCounter.incrementAndGet();
  }

  public List<HostRoleCommand> getRequestTasks(long requestId) {
    return db.getRequestTasks(requestId);
  }

  public List<HostRoleCommand> getAllTasksByRequestIds(Collection<Long> requestIds) {
    return db.getAllTasksByRequestIds(requestIds);
  }

  public List<HostRoleCommand> getTasksByRequestAndTaskIds(Collection<Long> requestIds, Collection<Long> taskIds) {
    return db.getTasksByRequestAndTaskIds(requestIds, taskIds);
  }

  public Collection<HostRoleCommand> getTasks(Collection<Long> taskIds) {
    return db.getTasks(taskIds);
  }

  public List<Stage> getRequestsByHostRoleStatus(Set<HostRoleStatus> statuses) {
    return db.getStagesByHostRoleStatus(statuses);
  }

  /**
   * Returns last 20 requests
   *
   * @return
   */
  public List<Long> getRequests() {
    return db.getRequestIds();
  }

  /**
   * Returns last 20 requests
   *
   * @return
   */
  public List<Long> getRequestsByStatus(RequestStatus status) {
    return db.getRequestsByStatus(status);
  }

  public Map<Long, String> getRequestContext(List<Long> requestIds) {
    return db.getRequestContext(requestIds);
  }

  public String getRequestContext(long requestId) {
    return db.getRequestContext(requestId);
  }
}
