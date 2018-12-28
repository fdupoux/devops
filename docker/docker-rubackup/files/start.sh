#!/bin/bash
set -e

if [ -n "${RUBACKUP_IMPORT_GPG_PUBKEY}" ]
then
    /usr/bin/wget ${RUBACKUP_IMPORT_GPG_PUBKEY} -O /var/tmp/gnupg-pubkey.txt
    /usr/bin/gpg --import /var/tmp/gnupg-pubkey.txt
    /usr/bin/gpg --list-keys --fingerprint --with-colons | sed -E -n -e 's/^fpr:::::::::([0-9A-F]+):$/\1:6:/p' | gpg --import-ownertrust
fi

echo "Starting rubackup.rb"
exec /opt/rubackup/rubackup.rb --logfile /var/log/rubackup/rubackup.log --loglevel 4 --outlevel 3
