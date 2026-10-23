#!/bin/sh
# Multiplayer check (max 15 min) on LOADER=neoforge|fabric: starts the dev server (<loader>/run-net/, the dev player
# "Dev" is an operator, the scene's slot data pack enabled in the new world: Curios or Trinkets), waits for it, runs the
# client scene port/NetScene that joins it, then stops the server and prints the checks and errors of both. Extra
# gradle arguments are passed on to both.
. "$(dirname "$0")/common.sh"
NET="$LOADER/run-net"
SLOG=build-netserver-$LOADER.log
CLOG=build-netclient-$LOADER.log
kill_dev
rm -rf "$NET/world" "$SLOG" "$CLOG"
mkdir -p "$NET" && echo "eula=true" > "$NET/eula.txt"
printf 'online-mode=false\nspawn-protection=0\ndifficulty=peaceful\ninitial-enabled-packs=vanilla,file/shb_slots\n' > "$NET/server.properties"
pack="$NET/world/datapacks/shb_slots"
mkdir -p "$pack"
echo '{"pack":{"pack_format":48,"description":"Spartan HUD: Baubles test slots"}}' > "$pack/pack.mcmeta"
slots="curio head necklace back body bracelet hands ring belt charm"
if [ "$LOADER" = fabric ]; then
	# Trinkets: the ten slots in the chest group (test_<name>, apart from Trinkets' own), in this order; ring and curio with two
	mkdir -p "$pack/data/trinkets/slots/chest" "$pack/data/trinkets/entities"
	order=0; list=""
	for s in $slots; do
		order=$((order+1)); amount=1
		case $s in ring|curio) amount=2 ;; esac
		echo "{\"order\":$order,\"amount\":$amount,\"icon\":\"trinkets:gui/slots/ring\",\"validator_predicates\":[\"trinkets:all\"]}" > "$pack/data/trinkets/slots/chest/test_$s.json"
		list="$list${list:+,}\"chest/test_$s\""
	done
	echo "{\"entities\":[\"minecraft:player\"],\"slots\":[$list]}" > "$pack/data/trinkets/entities/shb_test.json"
else
	mkdir -p "$pack/data/shb_test/curios/entities" "$pack/data/shb_test/curios/slots" "$pack/data/curios/tags/item"
	echo '{"entities":["minecraft:player"],"slots":["curio","head","necklace","back","body","bracelet","hands","ring","belt","charm"]}' > "$pack/data/shb_test/curios/entities/player.json"
	echo '{"size":2}' > "$pack/data/shb_test/curios/slots/ring.json"
	echo '{"size":2}' > "$pack/data/shb_test/curios/slots/curio.json"
	echo '{"values":["minecraft:gold_nugget","minecraft:emerald"]}' > "$pack/data/curios/tags/item/ring.json"
	echo '{"values":["minecraft:heart_of_the_sea"]}' > "$pack/data/curios/tags/item/necklace.json"
	echo '{"values":["minecraft:elytra"]}' > "$pack/data/curios/tags/item/back.json"
fi
# The offline UUID of "Dev"
uuid=$(python3 -c 'import hashlib,uuid;h=bytearray(hashlib.md5(b"OfflinePlayer:Dev").digest());h[6]=h[6]&0x0f|0x30;h[8]=h[8]&0x3f|0x80;print(uuid.UUID(bytes=bytes(h)))')
echo "[{\"uuid\":\"$uuid\",\"name\":\"Dev\",\"level\":4,\"bypassesPlayerLimit\":false}]" > "$NET/ops.json"
./gradlew ":$LOADER:runNetServer" --console=plain "$@" > "$SLOG" 2>&1 &
wait_started "$SLOG" 'Done \(|Minecraft Crash Report|BUILD FAILED' || { kill_dev; exit 124; }
run_watched "$CLOG" 600 ":$LOADER:runNetClient" "$@"
kill_dev; sleep 3
wait 2>/dev/null
grep -E '\[scene\] (CHECK|DONE)' "$CLOG" | sed -E 's/^.*\[scene\] //'
echo "--- server: mods and packs"
grep -E 'spartanhudbaubles|Loaded [0-9]+ curio|trinkets' "$SLOG" | sed -E 's/^\[[^]]*\] //' | cut -c1-200 | head -8
echo "--- server errors"
grep -E '/(ERROR|FATAL)\]|Caused by|Exception|lost connection|kicked' "$SLOG" | sed -E 's/^\[[^]]*\] //' | cut -c1-300 | sort | uniq -c | sort -rn | head -20
echo "--- client errors"
grep -E '/(ERROR|FATAL)\]|Caused by|Exception|Disconnected' "$CLOG" | sed -E 's/^\[[^]]*\] //' | cut -c1-300 | sort | uniq -c | sort -rn | head -20
