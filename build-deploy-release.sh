#!/bin/sh

# build plugin
mvn clean install

# upload
sh ./com.github.sdbg.releng.p2/upload-release.sh
