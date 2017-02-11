def profilespath = '/home/francois/PROPFILES'
def git_base_repo_url = 'https://github.com/fdupoux/devops.git'
def jenkins_node = 'slave-infras'
def branch_name = 'master'

def script_initialization = """#!/bin/bash
echo "Job running on \$(hostname -f)"
echo "WORKSPACE='\${WORKSPACE}'"
du -sh \${WORKSPACE}/*
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
accounts = prop.get("AWS_ACCOUNTS_LIST").toString().split(",");
git_extra_repo_url = prop.get("GIT_DEVOPS_PRIVATE").toString()

// create a CentOS AMI Creator job for each AWS Account
for (account in accounts) {
  job ("${account}_ansible_makeami_centos") {
    disabled(false)
    label(jenkins_node)
    logRotator(-1,20)

    wrappers {
      colorizeOutput('xterm')
    }
    multiscm {
      // get data from the main repository
      git {
        remote {
          url(git_base_repo_url)
        }
        branch('origin/' + branch_name)
        extensions {
            relativeTargetDirectory("git_base_repo")
        }
      }
      // get data from additional repository (to add extra files)
      git {
        remote {
          url(git_extra_repo_url)
        }
        branch('origin/' + branch_name)
        extensions {
            relativeTargetDirectory("git_extra_repo")
        }
      }
    }
    steps {
      environmentVariables {
        env('ACCOUNT', "${account}")
        env('AWS_ACCESS_KEY_ID', "\${AWS_ACCESS_KEY}")
        env('AWS_SECRET_ACCESS_KEY', "\${AWS_SECRET_KEY}")
        env('TERM', "xterm")
        env('ANSIBLE_FORCE_COLOR', 1)
        env('ANSIBLE_SSH_ARGS', '')
        env('PYTHONUNBUFFERED', 1)
        propertiesFile("${profilespath}/${account}-aws-credentials.properties")
      }

      def ansible_inventory = "hosts"
      def ansible_args_extra = ""

      def ansible_playbook = "playbook-generic-prod-makeami-centos.yml"
      def ansible_command = "ansible-playbook -i ${ansible_inventory} ${ansible_args_extra} ${ansible_playbook}"

      shell(script_initialization + ansible_command)
    }
    publishers {
      archiveArtifacts {
        pattern("**/target/ami_id.txt")
        fingerprint(true)
        onlyIfSuccessful(true)
      }
    }
  }
}
