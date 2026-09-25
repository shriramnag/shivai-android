#!/bin/sh
# Gradle start up script for POSIX compatible shells
APP_HOME=$(CDPATH= cd "${0%/*}" 2>/dev/null && pwd) || APP_HOME=$(pwd)
APP_NAME="Gradle"
APP_BASE_NAME="${0##*/}"
MAX_FD=maximum

warn() { echo "$*"; }
die() { echo; echo "$*"; echo; exit 1; }

cygwin=false; msys=false; darwin=false; nonstop=false
case "$(uname)" in
  CYGWIN* ) cygwin=true ;;
  Darwin* ) darwin=true ;;
  MSYS* | MINGW* ) msys=true ;;
  NONSTOP* ) nonstop=true ;;
esac

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ]; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD=java
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set."
fi

if [ "$cygwin" = "false" ] && [ "$darwin" = "false" ] && [ "$nonstop" = "false" ]; then
    case $MAX_FD in
      max*) MAX_FD=$(ulimit -H -n) || warn "Could not query max file descriptor limit" ;;
    esac
    case $MAX_FD in
      '' | soft) ;;
      *) ulimit -n "$MAX_FD" || warn "Could not set max file descriptor limit" ;;
    esac
fi

# JVM arguments are passed separately. Keeping them as separate shell words
# prevents Java from treating the quoted options as a class name.
set -- \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"

exec "$JAVACMD" -Xmx64m -Xms64m "$@"
