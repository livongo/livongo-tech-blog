#!/bin/bash

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
