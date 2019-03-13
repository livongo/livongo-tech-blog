#!/bin/bash
# Copyright (C) 2019 Livongo Corporation - All Rights Reserved
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

EXTENSION_LIB='extension-functions.so'
EXTENSION_URL='https://www.sqlite.org/contrib/download/extension-functions.c?get=25'
EXTENSION_SOURCE="extension-functions.c"
ANALYSIS_SQL='analysis.sql'

function setup.ensure-extension-is-available () {
    if [ ! -f "$DIR/$EXTENSION_LIB" ]; then
        echo >&2 '[Setup] Extension functions shared library does not exist'
        setup.download-extension-source
        setup.compile-extension
    fi
}

function setup.download-extension-source () {
    if [ ! -f "$DIR/$EXTENSION_SOURCE" ]; then
        echo >&2 '[Setup] Downloading extension source'
        docker run --rm -v "$DIR":/output -w /output manorrock/wget \
               wget "$EXTENSION_URL" \
               -O "$EXTENSION_SOURCE"
    fi
    [ ! -f "$DIR/$EXTENSION_SOURCE" ] && {
        echo >&2 '[Setup] Download failed'
        exit 1
    }
    echo >&2 '[Setup] Extension source downloaded'
}

function setup.compile-extension () {
    docker run --rm -i \
           -v "$DIR":/input -w /input \
           gcc gcc -fPIC -shared "$EXTENSION_SOURCE" \
           -o "$EXTENSION_LIB" -lm

    [ ! -f "$DIR/$EXTENSION_LIB" ] && {
        echo >&2 '[Setup] Compilation failed'
        exit 1
    }
    rm "$DIR/$EXTENSION_SOURCE"
    echo >&2 '[Setup] Extension compiled'
}

function analyze () {
    docker run --rm -i -v "$DIR":/input -w /input nouchka/sqlite3 < "$DIR/$ANALYSIS_SQL"
}

setup.ensure-extension-is-available && analyze
