#!/bin/sh
#
# Gradle startup script for POSIX compatible shells
#

APP_HOME=$(CDPATH= cd "${0%/*}" 2>/dev/null && pwd) || APP_HOME=$(pwd)
APP_BASE_NAME="${0##*/}"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

die() { echo; echo "$*"; echo; exit 1; }

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
    [ -x "$JAVACMD" ] || die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
else
    JAVACMD=java
    command -v java >/dev/null 2>&1 || die "ERROR: JAVA_HOME not set and java not found in PATH."
fi

exec "$JAVACMD" \
    -Xmx64m \
    -Xms64m \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
