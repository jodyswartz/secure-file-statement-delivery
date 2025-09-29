#!/bin/sh
./mvnw.cmd "$@" 2>/dev/null || mvn "$@"
