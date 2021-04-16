#!/usr/bin/env bash

if [[ $UID != 0 ]]; then
    echo "Root privilege required."
    echo "Please run this script with sudo."
    exit 1
fi

cp core/spyke /usr/local/bin
echo "Binary spyke is copied to /usr/local/bin."
pwd=$(pwd)
sed -i "s|TOBEREPLACED|$pwd|g" /usr/local/bin/spyke

exit 0
