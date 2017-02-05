# README for Ansible directory

## Roles
There are two types of ansible roles:
   * main-xxxx are top-level roles and correspond to roles in puppet terminology
   * other roles implement one component and correspond to "profiles" in puppet
     terminology

## Tags
Ansible roles will use the following tags:
  * "installation" for tasks must be executed when installing stuff during the
    creation of the AMI at a time details about the server are not known
  * "configuration" when the tasks must be executed to configure stuff during
    the bootstrap stage at the creation of the EC2 instance in an
    Auto-Scaling-Group or at a later stage when the configuration needs to be
    modified
