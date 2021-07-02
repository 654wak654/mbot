#!/bin/bash

systemctl stop mbot
git pull
gradle clean
gradle assemble
systemctl start mbot
