#!/bin/bash

systemctl stop mbot
git pull
gradle clean
gradle jar
systemctl start mbot
