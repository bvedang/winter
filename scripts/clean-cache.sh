#!/usr/bin/env bash
set -euo pipefail

java_tmp_dir="$(
  java -XshowSettings:properties -version 2>&1 \
    | awk -F'= ' '/java\\.io\\.tmpdir/ {print $2; exit}' \
    | tr -d '\r' \
    || true
)"

tmp_root="${java_tmp_dir:-${TMPDIR:-/tmp}}"
cache_dir="${tmp_root%/}/winter-route-cache"

if [[ "${1:-}" == "--print" ]]; then
  echo "$cache_dir"
  exit 0
fi

if [[ ! -d "$cache_dir" ]]; then
  echo "No cache dir found at: $cache_dir"
  exit 0
fi

echo "Removing: $cache_dir"
rm -rf -- "$cache_dir"
echo "OK"
