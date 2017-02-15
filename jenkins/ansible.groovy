// Global variables
def profilespath = '/home/francois/PROPFILES'
def git_base_repo_url = 'https://github.com/fdupoux/devops.git'
def git_branch = 'master'
def jobdefs = [:]

// Load job definitions from account specific files
hudson.FilePath workspace = hudson.model.Executor.currentExecutor().getCurrentWorkspace()
hudson.FilePath searchdir = workspace.child("jenkins")
println("Attempting to find account specific jobs definitions files in ${searchdir}")
def resultList = searchdir.list().findAll { it.name  ==~ /account_.*\.groovy/ }
for (curfile in resultList)
{
  println("Found definition file: ${searchdir}/${curfile.name}")
  def matcher = "${curfile.name}" =~ /account_(?<accname>\w+).groovy/
  if (matcher.matches() )
  {
    def accname = matcher.group('accname')
    println("Processing definition file: ${searchdir}/${curfile.name}")
    evaluate(new File("${searchdir}/${curfile.name}"))
    jobdefs["${accname}"] = getJobs()
  }
  else
  {
    println("Ignoring definition file: ${searchdir}/${curfile.name}")
  }
}

// Initialization part of the shell script which runs ansible
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

// Create jobs for each account
jobdefs.each { account, jobs ->
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

    // read sensitive variables from properties file
    Properties prop = new Properties()
    prop.load(new FileInputStream("${profilespath}/aws-account-${account}.properties"));
    git_extra_repo_url = prop.get("GIT_DEVOPS_PRIVATE").toString()

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
            if (jobdata.containsKey('inventory'))
            {
              ansible_inventory = jobdata.inventory
            }
            def ansible_jobsvars = "-e account=${account} -e aws_region=\${AWS_REGION} -e env=${curenv}"
            def ansible_playbook = "playbook-generic-${jobdata.category}-${target}.yml"
            def ansible_command = "ansible-playbook -i ${ansible_inventory} ${ansible_jobsvars} ${ansible_args_extra} ${ansible_playbook}"

            shell("${script_initialization}\n${ansible_command}\n")
          }

          publishers
          {
            if (archive_artifact != "")
            {
              archiveArtifacts
              {
                pattern(archive_artifact)
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
