#!/bin/sh
# Release check: the release jar on the real NeoForge servers in prodtest/*/ (prodtest/serve.sh), alone and with Curios
# (a client mod: a server must start with it too). Outputs: prodtest/server-<version>-*.out. (1.21.11: NeoForge only,
# Trinkets has no 1.21.11 version.)
. "$(dirname "$0")/common.sh"
MOD=spartanhudbaubles
./gradlew :neoforge:jar -q || exit 1
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
