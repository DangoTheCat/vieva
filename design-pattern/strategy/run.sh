#!/usr/bin/env sh
# Compile and run the Strategy pattern demo (needs JDK 17+ on PATH).
set -e
cd "$(dirname "$0")"
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out com.swd392.strategy.Main
