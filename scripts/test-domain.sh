#!/usr/bin/env bash
# Compile and run the pure-Kotlin :domain unit tests WITHOUT Maven/Google repositories.
#
# Uses the Kotlin compiler and JUnit 4 that ship inside the Gradle distribution, so it works
# in sandboxes where Gradle cannot download dependencies. On a normal machine prefer:
#   ./gradlew :domain:test
#
# Usage: scripts/test-domain.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GRADLE_HOME="${GRADLE_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v gradle)")")")}"
LIB="$GRADLE_HOME/lib"
PLUGINS="$LIB/plugins"
OUT="$ROOT/build/offline-domain"

jar() { { ls "$LIB"/$1 "$PLUGINS"/$1 2>/dev/null || true; } | head -n1; }

STDLIB="$(jar 'kotlin-stdlib-2*.jar')"
COMPILER_CP="$(jar 'kotlin-compiler-embeddable-*.jar'):$STDLIB:$(jar 'kotlin-script-runtime-*.jar'):$(jar 'kotlin-reflect-*.jar'):$(jar 'kotlin-daemon-embeddable-*.jar'):$(jar 'trove4j-*.jar'):$(jar 'annotations-*.jar'):$(jar 'kotlinx-coroutines-core-jvm-*.jar')"
JUNIT_CP="$(jar 'junit-4*.jar'):$(jar 'hamcrest-core-*.jar')"

rm -rf "$OUT"
mkdir -p "$OUT/main" "$OUT/test"

kotlinc() {
  java -cp "$COMPILER_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect "$@"
}

echo "== Compiling domain/src/main"
kotlinc -cp "$STDLIB" -d "$OUT/main" $(find "$ROOT/domain/src/main" -name '*.kt')

echo "== Compiling domain/src/test"
kotlinc -cp "$STDLIB:$JUNIT_CP:$OUT/main" -d "$OUT/test" $(find "$ROOT/domain/src/test" -name '*.kt')

echo "== Running tests"
CLASSES="$(cd "$OUT/test" && find . -name '*Test.class' | sed 's|^\./||; s|\.class$||; s|/|.|g' | sort)"
java -cp "$STDLIB:$JUNIT_CP:$OUT/main:$OUT/test" org.junit.runner.JUnitCore $CLASSES
