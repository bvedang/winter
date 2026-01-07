#!/usr/bin/env bash
set -euo pipefail

./gradlew :examples:basic:run --no-daemon > /tmp/winter-example.log 2>&1 &
PID=$!

cleanup() {
  kill "$PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

for _ in {1..30}; do
  if curl -fsS "http://127.0.0.1:8080/" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

curl -fsS "http://127.0.0.1:8080/" >/dev/null
curl -fsS "http://127.0.0.1:8080/users" >/dev/null
curl -fsS "http://127.0.0.1:8080/users/123" >/dev/null
curl -fsS "http://127.0.0.1:8080/users/123/posts" >/dev/null
curl -fsS "http://127.0.0.1:8080/books/123" | rg -q "\"category\":\"books\"" || { echo "nested dynamic root route failed" >&2; exit 1; }

headers="$(curl -sS -I "http://127.0.0.1:8080/" | tr -d '\r')"
echo "$headers" | rg -qi "^HTTP/1\\.1 200" || { echo "Expected 200 for HEAD /" >&2; exit 1; }

headers="$(curl -sS -D - -o /dev/null -H "Origin: http://example.com" "http://127.0.0.1:8080/" | tr -d '\r')"
echo "$headers" | rg -qi "^Access-Control-Allow-Origin: \\*$" || { echo "missing CORS allow-origin header" >&2; exit 1; }

code="$(curl -sS -o /dev/null -w "%{http_code}" -X OPTIONS -H "Origin: http://example.com" -H "Access-Control-Request-Method: POST" -H "Access-Control-Request-Headers: x-test" "http://127.0.0.1:8080/users")"
if [[ "$code" != "204" ]]; then
  echo "Expected 204 from OPTIONS preflight, got $code" >&2
  exit 1
fi

headers="$(curl -sS -D - -o /dev/null -X PUT "http://127.0.0.1:8080/users/123" | tr -d '\r')"
echo "$headers" | rg -qi "^HTTP/1\\.1 405" || { echo "Expected 405 for PUT /users/123" >&2; exit 1; }
echo "$headers" | rg -qi "^Allow: " || { echo "Expected Allow header for 405" >&2; exit 1; }

json="$(curl -fsS -H "x-test: a" -H "x-test: b" --cookie "session=abc" "http://127.0.0.1:8080/inspect?q=1&q=2")"
echo "$json" | rg -q "\"qFirst\":\"1\"" || { echo "inspect missing qFirst" >&2; exit 1; }
echo "$json" | rg -q "\"qAll\":\\[\"1\",\"2\"\\]" || { echo "inspect missing qAll" >&2; exit 1; }
echo "$json" | rg -q "\"headerFirst\":\"a\"" || { echo "inspect missing headerFirst" >&2; exit 1; }
echo "$json" | rg -q "\"headerAll\":\\[\"a\",\"b\"\\]" || { echo "inspect missing headerAll" >&2; exit 1; }
echo "$json" | rg -q "\"cookie\":\"abc\"" || { echo "inspect missing cookie" >&2; exit 1; }

code="$(curl --path-as-is -sS -o /dev/null -w "%{http_code}" "http://127.0.0.1:8080/../../../etc/passwd")"
if [[ "$code" != "404" ]]; then
  echo "Expected 404 for path traversal attempt, got $code" >&2
  exit 1
fi

code="$(curl -sS -o /dev/null -w "%{http_code}" "http://127.0.0.1:8080/teapot")"
if [[ "$code" != "418" ]]; then
  echo "Expected 418 from /teapot, got $code" >&2
  exit 1
fi

echo "OK"
