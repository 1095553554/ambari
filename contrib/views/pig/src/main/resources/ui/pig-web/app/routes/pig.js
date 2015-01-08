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

App.PigRoute = Em.Route.extend({
  beforeModel: function(transition) {
    App.set('previousTransition', transition);
  },
  redirect: function () {
    var testsConducted = App.get("smokeTests");
    if (!testsConducted) {
        App.set("smokeTests", true);
        this.transitionTo('splash');
    }
  },
  actions: {
    gotoSection: function(nav) {
      var location = (nav.hasOwnProperty('url'))?nav.url:this.routeName;
      this.transitionTo(location);
    },
    showAlert:function (alert) {
      var pigAlert = this.controllerFor('pigAlert');
      return pigAlert.get('content').pushObject(Em.Object.create(alert));
    },
    openModal: function(modal,content) {
      this.controllerFor(modal).set('model', content);
      return this.render(['modal',modal].join('/'), {
        into: 'pig',
        outlet: 'modal',
        controller:modal
      });
    },
    removeModal: function() {
      return this.disconnectOutlet({
        outlet: 'modal',
        parentView: 'pig'
      });
    }
  },
  model: function() {
    return this.store.find('script');
  },
  renderTemplate: function() {
    this.render('pig');
    this.render('pig/alert', {into:'pig',outlet:'alert',controller:'pigAlert'});
  }
});

App.PigIndexRoute = Em.Route.extend({
  redirect:function () {
    this.transitionTo('pig.scripts');
  }
});

App.ErrorRoute = Ember.Route.extend({
  setupController:function (controller,error) {
    var data;
    if(!(error instanceof Error)) {
      data = JSON.parse(error.responseText);
    } else {
      data = error;
    }
    controller.set('model',data);
  }
});
