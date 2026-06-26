#!/usr/bin/env bash
# Scaffolds lucide-<name>.imageset asset folders from lucide-static v1.21.0.
# Usage: ./fetch-lucide-icons.sh name1 name2 ...
set -euo pipefail
ASSETS="$(cd "$(dirname "$0")/.." && pwd)/Psy/Resources/Assets.xcassets"
VER="1.21.0"
for name in "$@"; do
  dir="$ASSETS/lucide-$name.imageset"
  mkdir -p "$dir"
  url="https://unpkg.com/lucide-static@$VER/icons/$name.svg"
  if ! curl -fsSL "$url" -o "$dir/$name.svg"; then
    echo "MISSING: $name (HTTP error from $url)" >&2
    rmdir "$dir" 2>/dev/null || true
    continue
  fi
  cat > "$dir/Contents.json" <<JSON
{
  "images" : [ { "filename" : "$name.svg", "idiom" : "universal" } ],
  "info" : { "author" : "xcode", "version" : 1 },
  "properties" : { "preserves-vector-representation" : true, "template-rendering-intent" : "template" }
}
JSON
  echo "OK: $name"
done
