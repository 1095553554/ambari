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

var App = require('app');

App.MainStackVersionsController = Em.ArrayController.extend({
  name: 'mainStackVersionsController',

  content: App.StackVersion.find(),
  timeoutRef: null,
  isPolling: false,
  dataIsLoaded: false,
  mockUrl: '/data/stack_versions/stack_version_all.json',
  realUrl: function () {
    return App.apiPrefix + '/clusters/' + App.get('clusterName') + '/stack_versions?fields=*,repository_versions/*,repository_versions/operatingSystems/repositories/*';
  }.property('App.clusterName'),
  realUpdateUrl: function () {
    return App.apiPrefix + '/clusters/' + App.get('clusterName') + '/stack_versions?fields=ClusterStackVersions/*';
    //TODO return App.apiPrefix + '/clusters/' + App.get('clusterName') + '/stack_versions?fields=ClusterStackVersions/state,ClusterStackVersions/host_states&minimal_response=true';
  }.property('App.clusterName'),

  /**
   * request latest data from server and update content
   */
  doPolling: function () {
    var self = this;

    this.set('timeoutRef', setTimeout(function () {
      if (self.get('isPolling')) {
        self.loadStackVersionsToModel(self.get('dataIsLoaded')).done(function () {
          self.doPolling();
        })
      }
    }, App.componentsUpdateInterval));
  },
  /**
   * load all data components required by stack version table
   * @return {*}
   */
  load: function () {
    var dfd = $.Deferred();
    var self = this;
    this.loadStackVersionsToModel().done(function () {
      self.set('dataIsLoaded', true);
      dfd.resolve();
    });
    return dfd.promise();
  },

  /**
   * get stack versions from server and push it to model
   * @return {*}
   */
  loadStackVersionsToModel: function (isUpdate) {
    var dfd = $.Deferred();

    App.HttpClient.get(this.getUrl(isUpdate), App.stackVersionMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  getUrl: function (isUpdate) {
    return App.get('testMode') ? this.get('mockUrl') :
      isUpdate ? this.get('realUpdateUrl') : this.get('realUrl');
  },

  filterHostsByStack: function (version, state) {
    if (!version || !state)
      return;
    App.router.get('mainHostController').filterByStack(version, state);
    App.router.get('mainHostController').set('showFilterConditionsFirstLoad', true);
    App.router.transitionTo('hosts.index');
  },

  showHosts: function(event) {
    var self = this;
    var status = event.currentTarget.title.toCapital();
    var version = event.contexts[0];
    var hosts = event.contexts[1];
    if (hosts.length) {
      return App.ModalPopup.show({
        bodyClass: Ember.View.extend({
          title: Em.I18n.t('admin.stackVersions.hosts.popup.title').format(version, status, hosts.length),
          template: Em.Handlebars.compile('<h4>{{view.title}}</h4><span class="limited-height-2">'+ hosts.join('<br/>') + '</span>')
        }),
        header: Em.I18n.t('admin.stackVersions.hosts.popup.header').format(status),
        primary: Em.I18n.t('admin.stackVersions.hosts.popup.primary'),
        secondary: Em.I18n.t('common.close'),
        onPrimary: function() {
          this.hide();
          self.filterHostsByStack(version, status);
        }
      });
    }
  }

});
