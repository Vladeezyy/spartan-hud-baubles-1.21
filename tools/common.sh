#!/bin/sh
# Shared by the test scripts: LOADER=neoforge (default) or fabric, JAVA_HOME, killing dev runs, the run dir.
cd "$(dirname "$0")/.."
LOADER=${LOADER:-neoforge}
RUN="$LOADER/run"
if [ -z "$JAVA_HOME" ] && ! command -v java >/dev/null 2>&1; then
    for j in "$HOME"/.gradle/jdks/*21* /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home; do [ -d "$j/bin" ] && JAVA_HOME="$j" && break; done
    export JAVA_HOME
fi
# The game options of the single-loader runs (GUI scale, sounds off) for the screenshots
mkdir -p "$RUN"
[ -f "$RUN/options.txt" ] || { [ -f run/options.txt ] && cp run/options.txt "$RUN/options.txt"; }
# Stops this project's dev runs (Git Bash pkill does not see Windows' java.exe)
kill_dev() {
    case "$(uname -s)" in
        MINGW*|MSYS*) powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | ? { \$_.CommandLine -match 'spartan-hud-baubles' -and \$_.CommandLine -notmatch 'GradleDaemon|GradleWrapperMain' } | % { Stop-Process -Id \$_.ProcessId -Force }" 2>/dev/null ;;
        *) pkill -f 'spartan-hud-baubles.*(fml.modFolders|loom-cache|net.fabricmc)' 2>/dev/null ;;
    esac
}
# Runs a gradle run task in the background; a crash report stops the game. $1: log, $2: timeout (s), the rest: gradle.
# Hang watchdog: once the game has started, a log that stops growing for $STALL seconds (default 90) is a hung run
# (a modal screen, a deadlock): the run is stopped, "HUNG" and the last lines go to the output and the log, exit 124.
run_watched() {
    log=$1; timeout=$2; shift 2
    stall=${STALL:-90}
    perl -e "alarm $timeout; exec @ARGV" ./gradlew "$@" --console=plain > "$log" 2>&1 &
    gradle_pid=$!
    last_size=-1; last_change=$(date +%s); hung=0
    while kill -0 $gradle_pid 2>/dev/null; do
        if grep -q -- '---- Minecraft Crash Report ----' "$log" 2>/dev/null; then
            sleep 3
            kill_dev
        fi
        size=$(wc -c < "$log" 2>/dev/null || echo 0)
        now=$(date +%s)
        if [ "$size" != "$last_size" ]; then
            last_size=$size; last_change=$now
        # the game is running (the build and the start before it may be quiet for longer)
        elif [ $((now - last_change)) -ge "$stall" ] && grep -qE 'Render thread|Server thread|\[scene\]' "$log" 2>/dev/null; then
            hung=1
            tail_lines=$(grep -vE '^\s*$' "$log" | tail -4 | cut -c1-300)
            echo "HUNG: no log output for ${stall}s, stopping the run. Last lines:" | tee -a "$log"
            echo "$tail_lines"
            kill_dev; sleep 3; kill $gradle_pid 2>/dev/null; pkill -P $gradle_pid 2>/dev/null
            break
        fi
        sleep 2
    done
    wait $gradle_pid 2>/dev/null
    code=$?
    [ $hung = 1 ] && code=124
    echo "exit $code"
    return $code
}
# Waits until a server started in the background is up: "Done", a crash, a failed build or $2 (an extra grep -E
# pattern) in log $1. A log that stops growing for $STALL seconds (default 90) before that is a hung start: "HUNG", 124.
# At most 10 min ("TIMEOUT", 124).
wait_started() {
    log=$1; pattern='Done \(|Minecraft Crash Report|crash report|Crash report|BUILD FAILED|Failed to start|Exception in server tick'
    [ -n "$2" ] && pattern="$pattern|$2"
    stall=${STALL:-90}
    start=$(date +%s); last_size=-1; last_change=$start
    while :; do
        grep -qE "$pattern" "$log" 2>/dev/null && return 0
        size=$(wc -c < "$log" 2>/dev/null || echo 0)
        now=$(date +%s)
        if [ "$size" != "$last_size" ]; then
            last_size=$size; last_change=$now
        elif [ $((now - last_change)) -ge "$stall" ] && grep -qE 'Server thread|main/' "$log" 2>/dev/null; then
            tail_lines=$(grep -vE '^\s*$' "$log" | tail -4 | cut -c1-300)
            echo "HUNG: the server wrote nothing for ${stall}s before it was up. Last lines:" | tee -a "$log"
            echo "$tail_lines"
            return 124
        fi
        if [ $((now - start)) -ge 600 ]; then
            echo "TIMEOUT: the server was not up in 10 min" | tee -a "$log"
            return 124
        fi
        sleep 2
    done
}
errors() {
    grep -E '/(ERROR|FATAL)\]|Caused by|Exception' "$1" | grep -v 'NoSuchFileException: server.properties' | sed -E 's/^\[[^]]*\] //' | cut -c1-300 | sort | uniq -c | sort -rn | head -30
}
