// define jenkins jobs for "testacnt"
getJobs = { ->
  return [
    [
      targets: ['centos'],
      disabled: false,
      category: 'bldami',
      jnknode: 'slave-infras',
      archive_artifact: "**/target/ami_id.txt",
      envs: ['none'],
    ],
    [
      targets: ['websrv'],
      disabled: false,
      category: 'bldami',
      jnknode: 'slave-infras',
      argsextra: "-e input_ami_id=\${IMPORTED_AMI_ID}",
      copy_artifact: [category:'bldami', target:'centos', env: 'none', filepath:'**/target/ami_id.txt'],
      archive_artifact: "**/target/ami_id.txt",
      envs: ['none'],
    ],
    [
      targets: ['websrv'],
      disabled: false,
      category: 'deploy',
      jnknode: 'slave-infras',
      argsextra: "-e input_ami_id=\${IMPORTED_AMI_ID}",
      copy_artifact: [category:'bldami', target:'websrv', env: 'none', filepath:'**/target/ami_id.txt'],
      envs: ['test', 'prod'],
    ],
    [
      targets: ['websrv'],
      disabled: false,
      category: 'config',
      jnknode: 'slave-infras',
      envs: ['test', 'prod'],
    ],
  ]
}
