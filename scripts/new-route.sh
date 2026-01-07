#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  ./scripts/new-route.sh <path> [--app examples/basic]

Examples:
  ./scripts/new-route.sh /
  ./scripts/new-route.sh /users
  ./scripts/new-route.sh /users/[id]
  ./scripts/new-route.sh /users/[id]/posts
  ./scripts/new-route.sh /health.java        # exact file route (leaf)

Notes:
  - Default target is `examples/basic/routes/`.
  - By default, paths create directory routes with `index.java` (Next.js-style).
EOF
}

route=""
app_dir="examples/basic"

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --app) app_dir="${2:-}"; shift 2 ;;
    *) route="$1"; shift ;;
  esac
done

if [[ -z "$route" ]]; then
  usage
  exit 1
fi

base="${app_dir%/}/routes"

route="/${route#/}"
route="${route%%\?*}"

if [[ "$route" == *"/.."* || "$route" == *"../"* || "$route" == *"/."* || "$route" == *"./"* ]]; then
  echo "Refusing to create route with dot-segments: $route" >&2
  exit 1
fi

if [[ "$route" == "/" ]]; then
  target="$base/index.java"
elif [[ "$route" == *.java ]]; then
  target="$base/${route#/}"
else
  target="$base/${route#/}/index.java"
fi

mkdir -p "$(dirname "$target")"

if [[ -e "$target" ]]; then
  echo "Already exists: $target" >&2
  exit 1
fi

cat > "$target" <<'EOF'
import winter.Ctx;

import java.util.Map;

public class Route {
    public Object get(Ctx ctx) {
        return Map.of("ok", true);
    }
}
EOF

echo "Created: $target"

