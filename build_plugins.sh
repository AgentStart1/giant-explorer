#!/bin/sh
. ./common.sh

mkdir -p build
mkdir -p build/li
mkdir -p build/yue
mkdir -p build/yue-html

# 构建 yue 插件
gradlew :plugins:yue:yue-plugin:clean :plugins:yue:yue-plugin:build --no-daemon
checkLastResult

# 构建 li 插件
gradlew :plugins:li:li-plugin:clean :plugins:li:li-plugin:build --no-daemon
checkLastResult

# 构建 yue-html
cd plugins/yue-html
printStartLabel yue-html
sh dispatch.sh $1
checkLastResult yue-html $?

printEndLabel plugin
