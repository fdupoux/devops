FROM centos:7.6.1810

MAINTAINER Francois Dupoux

# Cleanup
RUN yum clean all

# Configure yum repositories
RUN yum -y install epel-release
RUN yum -y install http://rpms.remirepo.net/enterprise/remi-release-7.rpm
RUN yum -y install http://nginx.org/packages/centos/7/noarch/RPMS/nginx-release-centos-7-0.el7.ngx.noarch.rpm
RUN yum-config-manager --enable remi-php72
RUN yum-config-manager --enable nginx

# Install confd to generate configuration
ADD https://github.com/kelseyhightower/confd/releases/download/v0.16.0/confd-0.16.0-linux-amd64 /usr/bin/confd
RUN chmod +x /usr/bin/confd

# Install packages
RUN yum -y install supervisor nginx php-fpm

# Cleanup
RUN yum clean all

# Adding the configuration file of the nginx
ADD files/etc/nginx/nginx.conf /etc/nginx/nginx.conf
ADD files/etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf
ADD files/index.php /var/www/index.php

# Adding the configuration file of the Supervisor
ADD files/etc/supervisord.conf /etc/supervisord.conf
RUN mkdir -p /run/php-fpm/

# Install packages
RUN yum -y install bzip2 php-curl php-json php-gd php-mysql php-zlib php-ftp php-xml

# Debugging packages
# RUN yum -y install mlocate less vim net-tools tcpdump bind-utils nmap-ncat telnet

# Cleanup
RUN yum clean all

# Download phpbb installation
RUN rm -f phpBB.tar.bz2
RUN curl --output /var/tmp/phpBB.tar.bz2 https://www.phpbb.com/files/release/phpBB-3.2.5.tar.bz2

# Install phpBB
RUN rm -rf /var/www/* ; tar xfjv /var/tmp/phpBB.tar.bz2 -C /var/www/ --strip-components=1
RUN rm -rf /var/www/install
RUN rm -f /var/tmp/phpBB.tar.bz2
RUN chown nginx:nginx /var/www/ -R && chmod 644 /var/www/ -R && chmod +X /var/www/ -R

# Configure php-fpm
RUN sed -i -e 's/user.*/user = nginx/' -e 's/group.*/group = nginx/' /etc/php-fpm.d/www.conf

# Add files
ADD files/start.sh /start.sh
ADD files/confd /etc/confd
RUN chmod +x /start.sh

# Set the port to 80
EXPOSE 80

# Executing main process
CMD ["/start.sh"]
