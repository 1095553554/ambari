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
import os

from resource_management import *
import re


def service(action=None, name=None, user=None, create_pid_dir=False,
            create_log_dir=False):
  import params

  pid_dir = format("{hadoop_pid_dir_prefix}/{user}")
  pid_file = format("{pid_dir}/hadoop-{user}-{name}.pid")
  log_dir = format("{hdfs_log_dir_prefix}/{user}")
  check_process = format(
    "ls {pid_file} >/dev/null 2>&1 &&"
    " ps -p `cat {pid_file}` >/dev/null 2>&1")

  if create_pid_dir:
    Directory(pid_dir,
              owner=user,
              recursive=True)
  if create_log_dir:
    Directory(log_dir,
              owner=user,
              recursive=True)

  hadoop_env_exports = {
    'HADOOP_LIBEXEC_DIR': params.hadoop_libexec_dir
  }

  if params.security_enabled and name == "datanode":
    ## The directory where pid files are stored in the secure data environment.
    hadoop_secure_dn_pid_dir = format("{hadoop_pid_dir_prefix}/{hdfs_user}")
    hadoop_secure_dn_pid_file = format("{hadoop_secure_dn_pid_dir}/hadoop_secure_dn.pid")

    # At Champlain stack and further, we may start datanode as a non-root even in secure cluster
    if not (params.hdp_stack_version != "" and compare_versions(params.hdp_stack_version, '2.2') >= 0) or params.secure_dn_ports_are_in_use:
      user = "root"
      pid_file = format(
        "{hadoop_pid_dir_prefix}/{hdfs_user}/hadoop-{hdfs_user}-{name}.pid")

    if action == 'stop' and (params.hdp_stack_version != "" and compare_versions(params.hdp_stack_version, '2.2') >= 0) and \
      os.path.isfile(hadoop_secure_dn_pid_file):
        # We need special handling for this case to handle the situation
        # when we configure non-root secure DN and then restart it
        # to handle new configs. Otherwise we will not be able to stop
        # a running instance 
        user = "root"
        
        try:
          check_process_status()
          
          custom_export = {
            'HADOOP_SECURE_DN_USER': params.hdfs_user
          }
          hadoop_env_exports.update(custom_export)
          
        except ComponentIsNotRunning:
          pass


  hadoop_daemon = format("{hadoop_bin}/hadoop-daemon.sh")

  if user == "root":
    cmd = [hadoop_daemon, "--config", params.hadoop_conf_dir]
    daemon_cmd = as_sudo(cmd + [action, name])
  else:
    cmd = format("{hadoop_daemon} --config {hadoop_conf_dir}")
    daemon_cmd = as_user(format("{ulimit_cmd} {cmd} {action} {name}"), user)
     
  service_is_up = check_process if action == "start" else None
  #remove pid file from dead process
  File(pid_file,
       action="delete",
       not_if=check_process
  )
  Execute(daemon_cmd,
          not_if=service_is_up,
          environment=hadoop_env_exports
  )

  #After performing the desired action, perform additional tasks.
  # Only needed during Rolling Upgrade
  '''
  if action == "start":
    Execute("hdfs dfsadmin -report -live",
            user=params.hdfs_principal_name if params.security_enabled else params.hdfs_user
    )
  '''

  if action == "stop":
    File(pid_file,
         action="delete",
    )

def get_port(address):
  """
  Extracts port from the address like 0.0.0.0:1019
  """
  if address is None:
    return None
  m = re.search(r'(?:http(?:s)?://)?([\w\d.]*):(\d{1,5})', address)
  if m is not None:
    return int(m.group(2))
  else:
    return None

def is_secure_port(port):
  """
  Returns True if port is root-owned at *nix systems
  """
  if port is not None:
    return port < 1024
  else:
    return False
