#!/bin/bash
set -e

echo "Starting rubackup.rb"
exec /opt/rubackup/rubackup.rb --logfile /var/log/rubackup/rubackup.dbg --loglevel 4 --outlevel 3
