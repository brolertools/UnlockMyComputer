#! /bin/bash
IFS= read -rs PASSWD
sudo -k
if sudo -lS &> /dev/null << EOF
$PASSWD
EOF
then
    echo 'Y'
else
    echo 'N'
fi