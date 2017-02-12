def profilespath = '/home/francois/PROPFILES'
def git_base_repo_url = 'https://github.com/fdupoux/devops.git'
def git_branch = 'master'

def script_initialization = """#!/bin/bash
echo "Job running on \$(hostname -f)"
echo "AWS_REGION='\${AWS_REGION}'"
echo "ACCOUNT='\${ACCOUNT}'"
echo "WORKSPACE='\${WORKSPACE}'"
du -sh \${WORKSPACE}/*
if test -f \${WORKSPACE}/imported_artifacts/ami_id.txt
then
  export IMPORTED_AMI_ID="\$(cat \${WORKSPACE}/imported_artifacts/ami_id.txt)"
  echo "IMPORTED_AMI_ID='\${IMPORTED_AMI_ID}'"
fi
mkdir -p \${WORKSPACE}/git_combined
rsync -a \${WORKSPACE}/git_base_repo/ \${WORKSPACE}/git_combined/ --exclude=.git --delete --delete-excluded
rsync -a \${WORKSPACE}/git_extra_repo/ \${WORKSPACE}/git_combined/ --exclude=.git
cd \${WORKSPACE}/git_combined
export VAULT_PASSWD=~/.vault-password
ansible --version
make vendor
cd ansible
"""

// read list of aws accounts from properties file
Properties prop = new Properties()
prop.load(new FileInputStream("${profilespath}/global.properties"));
aws_accounts = prop.get("AWS_ACCOUNTS_LIST").toString().split(",");
git_extra_repo_url = prop.get("GIT_DEVOPS_PRIVATE").toString()

// Data which are specific to each job
def jobs =
[
  [
    target: 'centos',
    disabled: false,
    accounts: aws_accounts,
    category: 'bldami',
    jnknode: 'slave-infras',
    archive_artifact: "**/target/ami_id.txt",
  ],
  [
    target: 'websrv',
    disabled: false,
    accounts: [aws_accounts[2]],
    category: 'bldami',
    jnknode: 'slave-infras',
    argsextra: "-e input_ami_id=\${IMPORTED_AMI_ID}",
    copy_artifact: [category:'bldami', target:'centos', filepath:'**/target/ami_id.txt'],
    archive_artifact: "**/target/ami_id.txt",
  ],
  [
    target: 'websrv',
    disabled: false,
    accounts: [aws_accounts[2]],
    category: 'deploy',
    jnknode: 'slave-infras',
    argsextra: "-e input_ami_id=\${IMPORTED_AMI_ID}",
    copy_artifact: [category:'bldami', target:'websrv', filepath:'**/target/ami_id.txt'],
  ],
  [
    target: 'websrv',
    disabled: false,
    accounts: [aws_accounts[2]],
    category: 'config',
    jnknode: 'slave-infras',
  ],
]

// create jobs
jobs.each
{ data ->

  def job_disabled = false
  if (data.containsKey('disabled'))
  {
    job_disabled = data.disabled
  }
  def ansible_args_extra = ""
  if (data.containsKey('argsextra'))
  {
    ansible_args_extra = data.argsextra
  }
  def copy_artifact = ""
  if (data.containsKey('copy_artifact'))
  {
    copy_artifact = data.copy_artifact
  }
  def archive_artifact = ""
  if (data.containsKey('archive_artifact'))
  {
    archive_artifact = data.archive_artifact
  }

  for (account in data.accounts)
  {
    job ("${account}_ansible_${data.category}_${data.target}")
    {
      disabled(job_disabled)
      label(data.jnknode)
      logRotator(-1,20)

      wrappers
      {
        colorizeOutput('xterm')
      }

      multiscm
      {
        // get data from the main repository
        git
        {
          remote
          {
            url(git_base_repo_url)
          }
          branch('origin/' + git_branch)
          extensions
          {
              relativeTargetDirectory("git_base_repo")
          }
        }
        // get data from additional repository (to add extra files)
        git
        {
          remote
          {
            url(git_extra_repo_url)
          }
          branch('origin/' + git_branch)
          extensions
          {
              relativeTargetDirectory("git_extra_repo")
          }
        }
      }

      steps
      {
        environmentVariables
        {
          env('ACCOUNT', "${account}")
          env('AWS_ACCESS_KEY_ID', "\${AWS_ACCESS_KEY}")
          env('AWS_SECRET_ACCESS_KEY', "\${AWS_SECRET_KEY}")
          env('TERM', "xterm")
          env('ANSIBLE_FORCE_COLOR', 1)
          env('ANSIBLE_SSH_ARGS', '')
          env('PYTHONUNBUFFERED', 1)
          propertiesFile("${profilespath}/aws-account-${account}.properties")
        }

        if (copy_artifact != "")
        {
          copyArtifacts("${account}_ansible_${copy_artifact.category}_${copy_artifact.target}")
          {
            includePatterns(copy_artifact.filepath)
            targetDirectory('imported_artifacts')
            flatten()
            buildSelector
            {
                latestSuccessful(true)
            }
          }
        }

        def ansible_inventory = "hosts-${account}"
        def ansible_jobsvars = " -e account=${account} -e aws_region=\${AWS_REGION} "
        def ansible_playbook = "playbook-generic-prod-${data.category}-${data.target}.yml"
        def ansible_command = "ansible-playbook -i ${ansible_inventory} ${ansible_jobsvars} ${ansible_args_extra} ${ansible_playbook}"

        shell("${script_initialization}\n${ansible_command}\n")
      }

      publishers
      {
        if (data.archive_artifact != "")
        {
          archiveArtifacts
          {
            pattern(data.archive_artifact)
            fingerprint(true)
            onlyIfSuccessful(true)
          }
        }
      }
    }
  }
}
