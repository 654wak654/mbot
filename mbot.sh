#!/bin/bash

while getopts ":s" opt; do
    case $opt in
        s)
            export isService=true
            ;;
        \?)
            echo "Invalid option: -$OPTARG" >&2
            exit 1
            ;;
    esac
done

if [ "$isService" = true ]; then
    java -jar build/app/mbot.jar > mbot.log 2>&1
else
    java -jar build/app/mbot.jar
fi
