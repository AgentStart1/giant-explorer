#!/bin/sh
. ./common.sh

mkdir -p build
mkdir -p build/GiantExplorer

# 直接在根目录构建
gradlew clean build --no-daemon
checkLastResult

cp app/build/outputs/apk/release/*.apk build/GiantExplorer/

printEndLabel app
