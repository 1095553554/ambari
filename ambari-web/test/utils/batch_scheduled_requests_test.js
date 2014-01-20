/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('utils/helper');
var batchUtils = require('utils/batch_scheduled_requests');

describe('batch_scheduled_requests', function() {

  describe('#getRollingRestartComponentName', function() {
    var tests = [
      {serviceName: 'HDFS', componentName: 'DATANODE'},
      {serviceName: 'YARN', componentName: 'NODEMANAGER'},
      {serviceName: 'MAPREDUCE', componentName: 'TASKTRACKER'},
      {serviceName: 'HBASE', componentName: 'HBASE_REGIONSERVER'},
      {serviceName: 'STORM', componentName: 'SUPERVISOR'},
      {serviceName: 'SOME_INVALID_SERVICE', componentName: null}
    ];

    tests.forEach(function(test) {
      it(test.serviceName + ' - ' + test.componentName, function() {
        expect(batchUtils.getRollingRestartComponentName(test.serviceName)).to.equal(test.componentName);
      });
    });

  });

  describe('#getBatchesForRollingRestartRequest', function() {
    var tests = [
      {
        hostComponents: Em.A([
          Em.Object.create({componentName:'DATANODE', service:{serviceName:'HDFS'}, host:{hostName:'host1'}}),
          Em.Object.create({componentName:'DATANODE', service:{serviceName:'HDFS'}, host:{hostName:'host2'}}),
          Em.Object.create({componentName:'DATANODE', service:{serviceName:'HDFS'}, host:{hostName:'host3'}})
        ]),
        batchSize: 2,
        m: 'DATANODES on three hosts, batchSize = 2',
        e: {
          batchCount: 2
        }
      },
      {
        hostComponents: Em.A([
          Em.Object.create({componentName:'DATANODE', service:{serviceName:'HDFS'}, host:{hostName:'host1'}}),
          Em.Object.create({componentName:'DATANODE', service:{serviceName:'HDFS'}, host:{hostName:'host2'}}),
          Em.Object.create({componentName:'DATANODE', service:{serviceName:'HDFS'}, host:{hostName:'host3'}})
        ]),
        batchSize: 3,
        m: 'DATANODES on 3 hosts, batchSize = 3',
        e: {
          batchCount: 1
        }
      },
      {
        hostComponents: Em.A([
          Em.Object.create({componentName:'DATANODE', service:{serviceName:'HDFS'}, host:{hostName:'host1'}}),
          Em.Object.create({componentName:'DATANODE', service:{serviceName:'HDFS'}, host:{hostName:'host2'}}),
          Em.Object.create({componentName:'DATANODE', service:{serviceName:'HDFS'}, host:{hostName:'host3'}})
        ]),
        batchSize: 1,
        m: 'DATANODES on 3 hosts, batchSize = 1',
        e: {
          batchCount: 3
        }
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        expect(batchUtils.getBatchesForRollingRestartRequest(test.hostComponents, test.batchSize).length).to.equal(test.e.batchCount);
      });
    });
  });

});
