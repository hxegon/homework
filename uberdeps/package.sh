#!/usr/bin/env bash
#
# see: https://github.com/tonsky/uberdeps
set -e

cd "$( dirname "${BASH_SOURCE[0]}" )"
clojure -M -m uberdeps.uberjar --deps-file ../deps.edn --main-class hxegon.homework --target ../target/homework.jar
