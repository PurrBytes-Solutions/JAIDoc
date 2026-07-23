#!/bin/bash
# Generates the "Current Versions" section for documentation/JDK-DISTRIBUTION.md
# by fetching the latest JDK update versions from Oracle's release notes.
#
# Usage:
#   bash scripts/generate-jdk-versions-doc.sh
#
# The script outputs the markdown content and instructions for the AI to
# automatically update the documentation file.

set -euo pipefail

USER_AGENT="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

# Config: JDK version -> release notes URL + parsing pattern
declare -A JDK_URLS=(
    [8]="https://www.oracle.com/java/technologies/javase/8u-relnotes.html"
    [11]="https://www.oracle.com/java/technologies/javase/11u-relnotes.html"
    [17]="https://www.oracle.com/java/technologies/javase/17u-relnotes.html"
    [21]="https://www.oracle.com/java/technologies/javase/21u-relnotes.html"
    [25]="https://www.oracle.com/java/technologies/javase/25u-relnotes.html"
)

declare -A JDK_PATTERNS=(
    [8]='href="/java/technologies/javase/8u([0-9]+)-relnotes'
    [11]='JDK 11\.0\.([0-9]+)'
    [17]='JDK 17\.0\.([0-9]+)'
    [21]='JDK 21\.0\.([0-9]+)'
    [25]='JDK 25\.0\.([0-9]+)'
)

# Ordered list of JDK versions to fetch
JDK_VERSIONS=(8 11 17 21 25)

# Fetch and parse versions for each JDK
declare -A JDK_VERSIONS_MAP

for version in "${JDK_VERSIONS[@]}"; do
    echo "Fetching JDK $version..."
    html=$(curl -s -A "$USER_AGENT" "$JDK_URLS[$version]")
    # Extract update numbers, sort descending, unique
    update_numbers=$(echo "$html" | grep -oP "${JDK_PATTERNS[$version]}" | \
        sed 's/.*JDK [0-9]*\.0\.//' | \
        sort -rn | uniq)

    JDK_VERSIONS_MAP[$version]="$update_numbers"

    if [[ -z "$update_numbers" ]]; then
        echo "Error fetching JDK $version: no matching versions found" >&2
        exit 1
    fi
done

# Format version strings based on JDK type
format_versions() {
    local jdk_version=$1
    shift
    local update_numbers=("$@")

    if [[ "$jdk_version" == "8" ]]; then
        # JDK 8 format: 8uNNN
        local formatted=()
        for num in "${update_numbers[@]}"; do
            formatted+=("8u${num}")
        done
        printf '%s\n' "${formatted[@]}" | paste -sd', '
    else
        # Modern JDK format: X.0.N
        local formatted=()
        for num in "${update_numbers[@]}"; do
            formatted+=("${jdk_version}.0.${num}")
        done
        printf '%s\n' "${formatted[@]}" | paste -sd', '
    fi
}

# Build the markdown content for the Current Versions section
{
    echo "## Current Versions"
    echo ""
    echo "<!-- BEGIN: JDK Current Versions -->"
    echo "The following update versions are currently available on [Oracle's JDK Release Notes](https://www.oracle.com/java/technologies/javase/jdk-relnotes-index.html):"
    echo ""

    for version in "${JDK_VERSIONS[@]}"; do
        echo ""
        echo "### JDK $version"
        echo ""

        # Read update numbers into array
        mapfile -t updates <<< "${JDK_VERSIONS_MAP[$version]}"

        formatted=$(format_versions "$version" "${updates[@]}")
        echo "Update versions: $formatted"
    done

    echo "<!-- END: JDK Current Versions -->"
}
