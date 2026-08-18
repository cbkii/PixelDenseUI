#!/usr/bin/env bash

SCRIPT_DIR=$(
    cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 &&
    pwd -P
) || {
    printf 'ERROR: Cannot determine script directory.\n' >&2
    exit 1
}
ROOT_DIR=$(cd -- "$SCRIPT_DIR/.." >/dev/null 2>&1 && pwd -P) || {
    printf 'ERROR: Cannot determine repository root.\n' >&2
    exit 1
}

errors=0
checks=0

pass() {
    checks=$((checks + 1))
}

fail() {
    checks=$((checks + 1))
    errors=$((errors + 1))
    printf 'ERROR: %s\n' "$*" >&2
}

require_file() {
    local rel=${1:-}
    if [[ -n $rel && -s "$ROOT_DIR/$rel" ]]; then
        pass
    else
        fail "Missing required file: ${rel:-<empty path>}"
    fi
}

require_exact_line() {
    local file=${1:-}
    local expected=${2:-}
    if grep -Fxq -- "$expected" "$file" 2>/dev/null; then
        pass
    else
        fail "Expected line '$expected' not found in ${file#$ROOT_DIR/}"
    fi
}

main() {
    local rel
    local java_count

    for rel in \
        app/src/main/java/dev/pixeldenseui/ModuleMain.java \
        app/src/main/java/dev/pixeldenseui/hooks/FrameworkStatusBarHooks.java \
        app/src/main/java/dev/pixeldenseui/hooks/StatusBarHooks.java \
        app/src/main/java/dev/pixeldenseui/hooks/SystemUiResourceHooks.java \
        app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java \
        app/src/main/java/dev/pixeldenseui/hooks/LauncherHooks.java \
        app/src/main/resources/META-INF/xposed/java_init.list \
        app/src/main/resources/META-INF/xposed/module.prop \
        app/src/main/resources/META-INF/xposed/scope.list \
        LICENSE \
        docs/UPSTREAM.md \
        docs/APK_EVIDENCE.md \
        NOTICE.md; do
        require_file "$rel"
    done

    require_exact_line "$ROOT_DIR/app/src/main/resources/META-INF/xposed/scope.list" 'android'
    require_exact_line "$ROOT_DIR/app/src/main/resources/META-INF/xposed/scope.list" 'com.android.systemui'
    require_exact_line "$ROOT_DIR/app/src/main/resources/META-INF/xposed/scope.list" 'com.google.android.apps.nexuslauncher'
    require_exact_line "$ROOT_DIR/app/src/main/resources/META-INF/xposed/module.prop" 'minApiVersion=101'
    require_exact_line "$ROOT_DIR/app/src/main/resources/META-INF/xposed/module.prop" 'targetApiVersion=101'

    if grep -R --line-number --fixed-strings 'NO_CUTOUT' "$ROOT_DIR/app/src/main/java" >/dev/null 2>&1; then
        fail 'Java source unexpectedly contains the aggressive cutout-removal path.'
    else
        pass
    fi

    java_count=$(find "$ROOT_DIR/app/src/main/java" -type f -name '*.java' -print | wc -l)
    if [[ $java_count =~ ^[0-9]+$ ]] && ((java_count >= 10)); then
        pass
    else
        fail "Unexpected Java source count: $java_count"
    fi

    printf '\n==================================================\n'
    if ((errors == 0)); then
        printf 'RESULT:       SUCCESS\n'
    else
        printf 'RESULT:       FAILED\n'
    fi
    printf 'CHECKS:       %d\n' "$checks"
    printf 'ERRORS:       %d\n' "$errors"
    printf 'JAVA SOURCES: %s\n' "$java_count"
    printf 'SCOPES:       android, SystemUI, Pixel Launcher\n'
    printf 'LIBXPOSED:    API 101\n'
    printf 'CUTOUT MODE:  retain + clamp (no removal)\n'
    printf '==================================================\n'

    ((errors == 0))
}

main "$@"
