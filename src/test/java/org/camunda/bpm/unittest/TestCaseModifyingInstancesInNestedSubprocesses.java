/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.unittest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.complete;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.processInstanceQuery;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.junit5.ProcessEngineExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

/**
 * @author Daniel Meyer
 * @author Martin Schimak
 */
public class TestCaseModifyingInstancesInNestedSubprocesses {

  @RegisterExtension
  ProcessEngineExtension extension = ProcessEngineExtension.builder()
    .build();

  @Test
  @Deployment(resources = { "nestedSubprocesses.bpmn" })
  public void shouldSetParentIDWhenModified() {
    // Given we create a new process instance
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("testProcess");
    // Then it should be active
    assertThat(processInstance).isActive();
    // And it should be the only instance
    assertThat(processInstanceQuery().count()).isEqualTo(1);
    // And there should exist just a single task within that process instance
    assertThat(task(processInstance)).isNotNull();
    // When we modify the instance and move the execution to the next subprocess
    extension.getRuntimeService().createProcessInstanceModification(processInstance.getProcessInstanceId())
        .startTransition("Flow_0n3b37l")
        .cancelAllForActivity("Task1")
        .execute();

    // Then there is one instance of this subprocess in the history service
    List<HistoricActivityInstance> historicInstancesOfSubprocess3 = extension.getHistoryService()
        .createHistoricActivityInstanceQuery()
        .processInstanceId(processInstance.getProcessInstanceId())
        .activityId("Subprocess3")
        .list();
    assertEquals(historicInstancesOfSubprocess3.size(), 1);
    // We expect that this instance has a parent ID (i.e., the id of the sourrounding
    assertNotNull(historicInstancesOfSubprocess3.get(0).getParentActivityInstanceId(), "T");

  }

}
