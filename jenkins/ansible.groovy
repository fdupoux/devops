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

// define jobs parameters
def jobs =
[
  [
    targets: ['centos'],
    disabled: false,
    accounts: aws_accounts,
    category: 'bldami',
    jnknode: 'slave-infras',
    archive_artifact: "**/target/ami_id.txt",
    envs: ['none'],
  ],
  [
    targets: ['websrv'],
    disabled: false,
    accounts: [aws_accounts[2]],
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
    accounts: [aws_accounts[2]],
    category: 'deploy',
    jnknode: 'slave-infras',
    argsextra: "-e input_ami_id=\${IMPORTED_AMI_ID}",
    copy_artifact: [category:'bldami', target:'websrv', env: 'none', filepath:'**/target/ami_id.txt'],
    envs: ['test', 'prod'],
  ],
  [
    targets: ['websrv'],
    disabled: false,
    accounts: [aws_accounts[2]],
    category: 'config',
    jnknode: 'slave-infras',
    envs: ['test', 'prod'],
  ],
]

// create jobs
for (jobdata in jobs)
{
  def job_disabled = false
  if (jobdata.containsKey('disabled'))
  {
    job_disabled = jobdata.disabled
  }
  def ansible_args_extra = ""
  if (jobdata.containsKey('argsextra'))
  {
    ansible_args_extra = jobdata.argsextra
  }
  def copy_artifact = ""
  if (jobdata.containsKey('copy_artifact'))
  {
    copy_artifact = jobdata.copy_artifact
  }
  def archive_artifact = ""
  if (jobdata.containsKey('archive_artifact'))
  {
    archive_artifact = jobdata.archive_artifact
  }

  for (account in jobdata.accounts)
  {
    for (target in jobdata.targets)
    {
      for (curenv in jobdata.envs)
      {
        job ("${account}_ansible_${curenv}_${jobdata.category}_${target}")
        {
          disabled(job_disabled)
          label(jobdata.jnknode)
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
            // get exta data from additional repository
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
              copyArtifacts("${account}_ansible_${copy_artifact.env}_${copy_artifact.category}_${copy_artifact.target}")
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
            def ansible_jobsvars = "-e account=${account} -e aws_region=\${AWS_REGION} -e env=${curenv}"
            def ansible_playbook = "playbook-generic-${jobdata.category}-${target}.yml"
            def ansible_command = "ansible-playbook -i ${ansible_inventory} ${ansible_jobsvars} ${ansible_args_extra} ${ansible_playbook}"

            shell("${script_initialization}\n${ansible_command}\n")
          }

          publishers
          {
            if (jobdata.archive_artifact != "")
            {
              archiveArtifacts
              {
                pattern(jobdata.archive_artifact)
                fingerprint(true)
                onlyIfSuccessful(true)
              }
            }
          }
        }
      }
    }
  }
}
