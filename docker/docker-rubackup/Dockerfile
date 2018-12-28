FROM centos:7.6.1810

MAINTAINER Francois Dupoux

# Cleanup
RUN yum clean all

# Configure yum repositories
RUN yum -y install epel-release

# Install packages
RUN yum -y install https://github.com/fdupoux/rubackup/releases/download/0.2.3/rubackup-0.2.3-1.el7.noarch.rpm

# Install dependencies
RUN yum -y install mariadb

# Cleanup
RUN yum clean all

# Configure rubackup
RUN mkdir -p /etc/rubackup.d/
RUN mkdir -p /var/log/rubackup/

# Add files
ADD files/start.sh /start.sh
RUN chmod +x /start.sh

# Executing main process
CMD ["/start.sh"]
