#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_DIR="$PROJECT_DIR/docs/images/readme"
REFERENCE_DIR="$PROJECT_DIR/app/src/screenshotTestDebug/reference"
LOGO_SOURCE="$PROJECT_DIR/design/void-launcher-icon.svg"

MAGICK_BIN="${MAGICK_BIN:-magick}"
MODE="all"

usage() {
    echo "Usage: $0 [--logo-only]"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --logo-only)
            MODE="logo-only"
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

command -v "$MAGICK_BIN" >/dev/null 2>&1 || {
    echo "Required command not found: $MAGICK_BIN" >&2
    exit 1
}

mkdir -p "$OUTPUT_DIR"

"$MAGICK_BIN" \
    -background none \
    "$LOGO_SOURCE" \
    -strip \
    -resize 256x256 \
    -define png:compression-level=9 \
    "$OUTPUT_DIR/logo.png"

echo "Generated $OUTPUT_DIR/logo.png"

if [[ "$MODE" == "logo-only" ]]; then
    exit 0
fi

"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" :app:updateDebugScreenshotTest

find_reference() {
    local preview_name="$1"
    local matches=()

    while IFS= read -r path; do
        matches+=("$path")
    done < <(find "$REFERENCE_DIR" -type f -name "*${preview_name}*.png" -print)

    if [[ "${#matches[@]}" -ne 1 ]]; then
        echo "Expected one reference image for $preview_name, found ${#matches[@]}." >&2
        exit 1
    fi

    echo "${matches[0]}"
}

publish_screenshot() {
    local preview_name="$1"
    local output_name="$2"
    local source_image
    source_image="$(find_reference "$preview_name")"

    "$MAGICK_BIN" \
        "$source_image" \
        -strip \
        -colorspace sRGB \
        -resize 540x \
        -define png:compression-level=9 \
        "$OUTPUT_DIR/$output_name.png"

    echo "Generated $OUTPUT_DIR/$output_name.png"
}

publish_screenshot "readmeHome" "home"
publish_screenshot "homeEmpty" "home-empty"
publish_screenshot "readmeDrawer" "drawer"
publish_screenshot "readmeSchedule" "schedule"
publish_screenshot "readmeCustomize" "customize"
