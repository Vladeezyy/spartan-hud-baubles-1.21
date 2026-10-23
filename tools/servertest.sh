#!/bin/sh
# Starts the dev server (LOADER=neoforge|fabric) on a fresh world, waits until it is up or has failed (max 10 min),
# stops it, prints a summary. Extra gradle arguments are passed on.
. "$(dirname "$0")/common.sh"
LOG=build-server-$LOADER.log
kill_dev
rm -rf "$RUN/world" "$LOG"
echo "eula=true" > "$RUN/eula.txt"
./gradlew ":$LOADER:runServer" --console=plain "$@" > "$LOG" 2>&1 &
wait_started "$LOG" 'Done \(|Minecraft Crash Report|crash report has been saved|Failed to start|Registry loading errors|BUILD FAILED|FAILED TO BIND'
sleep 3
kill_dev; sleep 2
wait 2>/dev/null
grep -E 'Done \(|Minecraft Crash Report|crash report has been saved|Failed to start|Registry loading errors|FAILED TO BIND|BUILD FAILED' "$LOG" | head -3
echo "--- errors / warnings from the mod or data"
grep -E '/(ERROR|FATAL|WARN)\]|Caused by|Exception' "$LOG" | grep -viE 'Ambiguity between arguments|Unable to resolve|Configuration file .* is not correct' | sed -E 's/^\[[^]]*\] //' | cut -c1-300 | sort | uniq -c | sort -rn | head -40
