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

pass() { checks=$((checks + 1)); }
fail() {
    checks=$((checks + 1))
    errors=$((errors + 1))
    printf 'ERROR: %s\n' "$*" >&2
}

require_file() {
    local rel=${1:-}
    if [[ -n $rel && -s "$ROOT_DIR/$rel" ]]; then pass; else fail "Missing required file: ${rel:-<empty path>}"; fi
}

require_absent() {
    local rel=${1:-}
    if [[ -n $rel && ! -e "$ROOT_DIR/$rel" ]]; then pass; else fail "File must not be committed/present: ${rel:-<empty path>}"; fi
}

require_exact_line() {
    local file=${1:-}
    local expected=${2:-}
    if grep -Fxq -- "$expected" "$file" 2>/dev/null; then pass; else fail "Expected line '$expected' not found in ${file#$ROOT_DIR/}"; fi
}

require_text() {
    local file=${1:-}
    local expected=${2:-}
    if grep -Fq -- "$expected" "$file" 2>/dev/null; then pass; else fail "Expected text '$expected' not found in ${file#$ROOT_DIR/}"; fi
}

reject_text() {
    local file=${1:-}
    local rejected=${2:-}
    if grep -Fq -- "$rejected" "$file" 2>/dev/null; then fail "Rejected text '$rejected' found in ${file#$ROOT_DIR/}"; else pass; fi
}

main() {
    local rel
    local java_count

    for rel in \
        app/src/main/java/dev/pixeldenseui/ModuleMain.java \
        app/src/main/java/dev/pixeldenseui/SettingsActivity.java \
        app/src/main/java/dev/pixeldenseui/config/ModuleConfig.java \
        app/src/main/java/dev/pixeldenseui/hooks/FrameworkStatusBarHooks.java \
        app/src/main/java/dev/pixeldenseui/hooks/HookUtil.java \
        app/src/main/java/dev/pixeldenseui/hooks/StatusBarHooks.java \
        app/src/main/java/dev/pixeldenseui/hooks/SystemUiResourceHooks.java \
        app/src/main/java/dev/pixeldenseui/hooks/LockscreenHooks.java \
        app/src/main/java/dev/pixeldenseui/hooks/ScreenshotHooks.java \
        app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java \
        app/src/main/java/dev/pixeldenseui/hooks/LauncherHooks.java \
        app/src/main/resources/META-INF/xposed/java_init.list \
        app/src/main/resources/META-INF/xposed/module.prop \
        app/src/main/resources/META-INF/xposed/scope.list \
        app/src/test/java/dev/pixeldenseui/config/ModuleConfigTest.java \
        .github/workflows/build.yml \
        .github/workflows/release.yml \
        LICENSE \
        docs/UPSTREAM.md \
        docs/APK_EVIDENCE.md \
        docs/DEVICE_HOOK_MAP.md \
        docs/ROADMAP.md \
        docs/RRO_EVALUATION.md \
        docs/STATUS_BAR_INSET_POLICY.md \
        docs/VALIDATION.md \
        NOTICE.md; do
        require_file "$rel"
    done

    require_exact_line "$ROOT_DIR/app/src/main/resources/META-INF/xposed/scope.list" 'system'
    require_exact_line "$ROOT_DIR/app/src/main/resources/META-INF/xposed/scope.list" 'android'
    require_exact_line "$ROOT_DIR/app/src/main/resources/META-INF/xposed/scope.list" 'com.android.systemui'
    require_exact_line "$ROOT_DIR/app/src/main/resources/META-INF/xposed/scope.list" 'com.google.android.apps.nexuslauncher'
    require_exact_line "$ROOT_DIR/app/src/main/resources/META-INF/xposed/module.prop" 'minApiVersion=101'
    require_exact_line "$ROOT_DIR/app/src/main/resources/META-INF/xposed/module.prop" 'targetApiVersion=101'

    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/SettingsActivity.java" 'requestScope(missing'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/SettingsActivity.java" 'controls below remain read-only'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/SettingsActivity.java" 'PixelDenseUI.Settings'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/SettingsActivity.java" 'settings_ui_cache'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/SettingsActivity.java" 'if (service != dead) return;'
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/SettingsActivity.java" 'getRunningTargets()'
    require_text "$ROOT_DIR/app/build.gradle.kts" 'io.github.libxposed:service:101.0.0'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/ModuleMain.java" 'fail-closed: skipping system_server hooks'
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/ModuleMain.java" 'runtime_system_server_version_code'
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/ModuleMain.java" 'markRuntime('
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/ModuleMain.java" 'BootLoopProtector'
    require_text "$ROOT_DIR/app/build.gradle.kts" 'SOURCE_REVISION'

    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/config/ModuleConfig.java" 'NOTIFICATION_MODE_OFF = 0'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/config/ModuleConfig.java" 'getInt("notification_mode", NOTIFICATION_MODE_OFF)'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/SystemUiHooks.java" 'notification hooks not installed: mode Off'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/SystemUiHooks.java" 'toLowerCase(Locale.ROOT)'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java" 'setContractedChild'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java" 'onNotificationUpdated'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java" 'android.contains.customView'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java" 'supportedStandardStyle'
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java" 'template == null && notification.contentView != null'
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java" 'scaleIconsRecursive'
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java" 'setScaleX('
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java" 'setScaleY('
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java" '"onLayout"'
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java" '"updateLimits"'
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java" '"getCollapsedHeight"'
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NotificationHooks.java" '"getMinHeight"'

    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/SystemUiResourceHooks.java" 'notification_min_height'
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/SystemUiResourceHooks.java" 'notification_icon_size'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/SystemUiResourceHooks.java" 'Collections.synchronizedMap(new WeakHashMap<>())'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/SystemUiResourceHooks.java" 'resourceKeyCache.get(resources)'

    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/StatusBarHooks.java" 'StatusIconContainer'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/StatusBarHooks.java" 'NotificationIconContainer'
    reject_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/StatusBarHooks.java" 'forceTopGravity'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NetworkTrafficController.java" 'newSingleThreadScheduledExecutor'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NetworkTrafficController.java" '0x80000000'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NetworkTrafficController.java" 'DOWNLOAD_COLOR'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/NetworkTrafficController.java" 'UPLOAD_COLOR'

    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/HookUtil.java" 'cls.getDeclaredMethods()'
    require_absent 'app/src/main/java/dev/pixeldenseui/safety/BootLoopProtector.java'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/config/ModuleConfig.java" 'getInt("qs_density_percent", 50)'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/config/ModuleConfig.java" 'getInt("qs_columns", 8)'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/config/ModuleConfig.java" 'getInt("qs_tile_height_percent", 100)'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/config/ModuleConfig.java" 'getInt("qs_columns_landscape", 12)'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/config/ModuleConfig.java" 'getInt("clock_position", 1), 0, 3'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/hooks/SystemUiResourceHooks.java" 'common_tile_default_tile_height'
    require_text "$ROOT_DIR/app/src/main/java/dev/pixeldenseui/config/ModuleConfig.java" 'getBoolean("clamp_cutout_safe_inset", false)'
    require_text "$ROOT_DIR/docs/RRO_EVALUATION.md" 'will **not** replace or patch `SystemUIGoogle.apk`'

    # Preserve the established release trust boundary while extending runtime checks.
    require_text "$ROOT_DIR/.github/workflows/release.yml" 'workflow_dispatch:'
    require_text "$ROOT_DIR/.github/workflows/release.yml" 'environment: release'
    require_text "$ROOT_DIR/.github/workflows/release.yml" 'KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}'
    require_text "$ROOT_DIR/.github/workflows/release.yml" 'KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}'
    require_text "$ROOT_DIR/.github/workflows/release.yml" 'KEY_ALIAS: ${{ secrets.KEY_ALIAS }}'
    require_text "$ROOT_DIR/.github/workflows/release.yml" 'KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}'
    require_text "$ROOT_DIR/.github/workflows/release.yml" 'target_code=$((source_code + 1))'
    require_text "$ROOT_DIR/.github/workflows/release.yml" 'chore(release): prepare ${RELEASE_TAG}'
    require_text "$ROOT_DIR/.github/workflows/release.yml" 'git push --force-with-lease='
    require_text "$ROOT_DIR/.github/workflows/release.yml" 'verify --verbose --print-certs'

    require_absent 'ReleaseKey.jks'
    require_absent 'keystore.properties'
    require_absent 'SNAPSHOT_MANIFEST.sha256'

    if grep -R --line-number --fixed-strings 'NO_CUTOUT' "$ROOT_DIR/app/src/main/java" >/dev/null 2>&1; then
        fail 'Java source unexpectedly contains the aggressive cutout-removal path.'
    else
        pass
    fi

    if grep -R --line-number -E '(KEYSTORE_BASE64|KEYSTORE_PASSWORD|KEY_ALIAS|KEY_PASSWORD)[[:space:]]*=[[:space:]]*[^$<{[:space:]]+' \
        "$ROOT_DIR" --exclude-dir=.git --exclude='verify.sh' >/dev/null 2>&1; then
        fail 'Potential hard-coded signing secret detected.'
    else
        pass
    fi

    if grep -Fq -- 'secrets.SIGNING_KEY' "$ROOT_DIR/.github/workflows/release.yml" \
        || grep -Fq -- 'secrets.KEY_STORE_PASSWORD' "$ROOT_DIR/.github/workflows/release.yml" \
        || grep -Fq -- 'secrets.ALIAS' "$ROOT_DIR/.github/workflows/release.yml"; then
        fail 'Manual release workflow still references retired signing-secret names.'
    else
        pass
    fi

    java_count=$(find "$ROOT_DIR/app/src/main/java" -type f -name '*.java' -print | wc -l)
    if [[ $java_count =~ ^[0-9]+$ ]] && ((java_count >= 13)); then
        pass
    else
        fail "Unexpected Java source count: $java_count"
    fi

    printf '\n==================================================\n'
    if ((errors == 0)); then printf 'RESULT:       SUCCESS\n'; else printf 'RESULT:       FAILED\n'; fi
    printf 'CHECKS:       %d\n' "$checks"
    printf 'ERRORS:       %d\n' "$errors"
    printf 'JAVA SOURCES: %s\n' "$java_count"
    printf 'SCOPES:       system, android, SystemUI, Pixel Launcher\n'
    printf 'LIBXPOSED:    API 101\n'
    printf 'NOTIFICATIONS: off/silent/all; no hot-layout traversal\n'
    printf 'TRAFFIC:      cellular overlay + background sampler\n'
    printf 'SETTINGS:     immediate read-only UI while app service is pending\n'
    printf 'RESTART GUARD: framework-native only; injected remote state is read-only\n'
    printf 'RELEASE:      workflow_dispatch + auto-version + signed APK\n'
    printf 'CUTOUT MODE:  retain; clamp opt-in\n'
    printf '==================================================\n'

    ((errors == 0))
}

main "$@"
