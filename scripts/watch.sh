#!/usr/bin/env bash
set -euo pipefail

cmd=(./gradlew run --no-daemon)

if command -v watchexec >/dev/null 2>&1; then
  exec watchexec -r \
    -w core/src \
    -w examples/basic/src \
    -w examples/basic/routes \
    -w build.gradle.kts \
    -w settings.gradle.kts \
    -w gradle.properties \
    -- "${cmd[@]}"
fi

if command -v entr >/dev/null 2>&1; then
  find core/src examples/basic/src examples/basic/routes \
    -type f \( -name '*.java' -o -name '*.kts' \) -print \
    | entr -r "${cmd[@]}"
  exit 0
fi

cat <<'EOF' >&2
watch.sh requires one of:
  - watchexec (recommended): https://github.com/watchexec/watchexec
  - entr: https://eradman.com/entrproject/

Then rerun:
  ./scripts/watch.sh
EOF
exit 1

