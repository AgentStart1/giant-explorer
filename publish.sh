#!/bin/sh
# Publish the plugin core to the local Maven repository.
sh gradlew clean -xtest -xlint giant-explorer-plugin-core:publishToMavenLocal --no-daemon
