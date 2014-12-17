"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""

from resource_management import *
from resource_management.libraries.functions.check_process_status import check_process_status

import utils  # this is needed to avoid a circular dependency since utils.py calls this class
from hdfs import hdfs


class ZkfcSlave(Script):
  def install(self, env):
    import params

    self.install_packages(env, params.exclude_packages)
    env.set_params(params)

  def start(self, env, rolling_restart=False):
    import params

    env.set_params(params)
    self.configure(env)
    Directory(params.hadoop_pid_dir_prefix,
              mode=0755,
              owner=params.hdfs_user,
              group=params.user_group
    )
    utils.service(
      action="start", name="zkfc", user=params.hdfs_user, create_pid_dir=True,
      create_log_dir=True
    )

  def stop(self, env, rolling_restart=False):
    import params

    env.set_params(params)
    utils.service(
      action="stop", name="zkfc", user=params.hdfs_user, create_pid_dir=True,
      create_log_dir=True
    )

  def configure(self, env):
    hdfs()
    pass

  def status(self, env):
    import status_params

    env.set_params(status_params)

    check_process_status(status_params.zkfc_pid_file)


if __name__ == "__main__":
  ZkfcSlave().execute()
