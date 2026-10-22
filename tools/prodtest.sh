#!/bin/sh
# Release check: the jars as players have them. NeoForge: the release jar on the real NeoForge servers in prodtest/*/
# (prodtest/serve.sh), alone and with Curios (a client mod: a server must start with it too). Fabric: the release jar on
# a real Fabric server with Trinkets (:fabric:prodServer), then the client scene and the stress scene on a production
# client (intermediary names; :fabric:prodScene / prodStress run a jar that is the release jar plus port/). Outputs:
# prodtest/server-<version>-*.out, build-prod*-fabric.log.
. "$(dirname "$0")/common.sh"
MOD=spartanhudbaubles
./gradlew :neoforge:jar :fabric:remapJar -q || exit 1
VERSION=$(grep '^mod_version=' gradle.properties | cut -d= -f2)+$(grep '^minecraft_version=' gradle.properties | cut -d= -f2)
JAR=neoforge/build/libs/$MOD-neoforge-$VERSION.jar
for dir in prodtest/*/; do
	[ -f "$dir/run.sh" ] || continue
	v=$(basename "$dir")
	find "$dir/mods" -iname "*hudbaubles*.jar" -delete 2>/dev/null
	cp "$JAR" "$dir/mods/"
	CURIOS=$(ls "$dir"/mods/curios-*.jar 2>/dev/null | head -1)
	echo "=== NeoForge server $v with Curios: $(ls "$dir/mods" | tr '\n' ' ')"
	(cd "$dir" && sh ../serve.sh) && cp "$dir/server.out" prodtest/server-$v-curios.out
	if [ -n "$CURIOS" ]; then
		mv "$CURIOS" prodtest/
		echo "=== NeoForge server $v alone: $(ls "$dir/mods" | tr '\n' ' ')"
		(cd "$dir" && sh ../serve.sh) && cp "$dir/server.out" prodtest/server-$v-alone.out
		mv prodtest/$(basename "$CURIOS") "$dir/mods/"
	fi
done
echo "=== Fabric server"
mkdir -p fabric/run-prod-server && echo "eula=true" > fabric/run-prod-server/eula.txt
rm -rf fabric/run-prod-server/world
LOG=build-prodserver-fabric.log
./gradlew :fabric:prodServer --console=plain > "$LOG" 2>&1 &
i=0
until grep -qE 'Done \(|Crash report|crash report|BUILD FAILED|Exception in server tick' "$LOG" 2>/dev/null || [ $i -gt 300 ]; do sleep 2; i=$((i+1)); done
sleep 3
pkill -f "fabric.addMods=.*$MOD-fabric" 2>/dev/null; sleep 3
grep -E 'Done \(|Crash report|crash report|BUILD FAILED' "$LOG" | head -3
grep -E '/(ERROR|FATAL)\]|Caused by|Exception' "$LOG" | grep -vE 'server.properties|No data fixer registered' | cut -c1-250 | sort | uniq -c | sort -rn | head -10
mkdir -p fabric/run-prod/config
[ -f fabric/run-prod/options.txt ] || cp fabric/run/options.txt fabric/run-prod/ 2>/dev/null
for t in Scene Stress; do
	rm -rf fabric/run-prod/saves/shb_scene fabric/run-prod/saves/shb_stress fabric/run-prod/screenshots
	rm -f fabric/run-prod/config/$MOD-client.toml
	# The stress scene starts with a broken config file (the original crashed on an unknown skin_type)
	[ $t = Stress ] && printf 'skin_type = "FOO"\nalignment = 5\noffset_x = 99999\n' > fabric/run-prod/config/$MOD-client.toml
	LOG=build-prod$(echo $t | tr 'A-Z' 'a-z')-fabric.log
	run_watched "$LOG" 1500 ":fabric:prod$t"
	echo "=== Fabric production client, $t: $(grep -c '\] CHECK' "$LOG") checks, $(grep -oE 'DONE, failed checks: [0-9]+' "$LOG")"
	grep -E 'CHECK FAIL|PERF' "$LOG" | sed 's/.*\[scene\] //' | head -20
	grep -E 'Mixin apply failed|InvalidInjection|Caused by' "$LOG" | cut -c1-250 | sort -u | head -10
done
