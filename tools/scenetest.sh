#!/bin/sh
# Runs a client test scene (max 15 min): -Pscene=dev (port/DevScene, default) or -Pscene=stress (port/StressScene).
# Screenshots in run/screenshots/scene_*.png; prints the functional checks, untranslated config strings and the errors
# of the run. A crash report stops the client. Extra gradle arguments are passed on (e.g. -Pscene_lang=ru_ru,
# -Pwith_compat).
cd "$(dirname "$0")/.."
rm -rf run/saves/shb_scene run/saves/shb_stress run/screenshots/scene_*.png build-scene.log
perl -e 'alarm 900; exec @ARGV' ./gradlew runScene --console=plain "$@" > build-scene.log 2>&1 &
gradle_pid=$!
while kill -0 $gradle_pid 2>/dev/null; do
	if grep -q -- '---- Minecraft Crash Report ----' build-scene.log 2>/dev/null; then
		sleep 3
		pkill -f 'fml.modFolders=spartanhudbaubles.*' 2>/dev/null
	fi
	sleep 2
done
wait $gradle_pid
echo "exit $?"
grep -E '\[scene\] (CHECK|DONE|PERF)' build-scene.log | sed -E 's/^.*\[scene\] //'
ls run/screenshots/ 2>/dev/null | grep scene_
echo "--- untranslated config strings"
grep -A40 'translation' build-scene.log | grep -E '"spartanhudbaubles\.' | sort -u | head -20
echo "--- errors"
grep -E '/(ERROR|FATAL)\]|Caused by|Exception' build-scene.log | grep -v 'NoSuchFileException: server.properties' | sed -E 's/^\[[^]]*\] //' | cut -c1-300 | sort | uniq -c | sort -rn | head -30
