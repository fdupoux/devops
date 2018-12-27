#!/bin/bash
set -e

# Set default value to variables when empty
export PHPBB_DATABASE_TYPE=${PHPBB_DATABASE_TYPE:-mysqli}

# Generate configuration files from parameters
/usr/bin/confd -onetime -backend env

echo "Starting processes via supervisord"
exec /usr/bin/supervisord -n
