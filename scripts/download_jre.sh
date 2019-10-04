#!/bin/bash

rm -f jre12.tar.gz
wget --no-check-certificate "https://api.adoptopenjdk.net/v2/binary/releases/openjdk12?openjdk_impl=hotspot&os=linux&arch=x64&release=latest&type=jre" -O jre12.tar.gz

rm -Rf jre
mkdir jre

tar xf jre12.tar.gz --strip-components=1 --directory jre
