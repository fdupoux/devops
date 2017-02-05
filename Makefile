# Download ansible dependencies using ansible-galaxy
vendor:
	rm -rf ansible/vendor
	ansible-galaxy install -p ansible/vendor -r ansible/ansible-galaxy-roles.yml
