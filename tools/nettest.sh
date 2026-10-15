#!/bin/sh
# Multiplayer check (max 15 min): starts the dev server (run-net/, the dev player "Dev" is an operator, the scene's
# Curios slot data pack enabled in the new world), waits for it, runs the client scene port/NetScene that joins it,
# then stops the server and prints the checks and errors of both. Extra gradle arguments are passed on to both.
cd "$(dirname "$0")/.."
pkill -f 'fml.modFolders=spartanhudbaubles' 2>/dev/null
rm -rf run-net/world build-netserver.log build-netclient.log
mkdir -p run-net && echo "eula=true" > run-net/eula.txt
printf 'online-mode=false\nspawn-protection=0\ndifficulty=peaceful\ninitial-enabled-packs=vanilla,file/shb_slots\n' > run-net/server.properties
pack=run-net/world/datapacks/shb_slots
mkdir -p $pack/data/shb_test/curios/entities $pack/data/shb_test/curios/slots $pack/data/curios/tags/item
echo '{"pack":{"pack_format":48,"description":"Spartan HUD: Baubles test slots"}}' > $pack/pack.mcmeta
echo '{"entities":["minecraft:player"],"slots":["curio","head","necklace","back","body","bracelet","hands","ring","belt","charm"]}' > $pack/data/shb_test/curios/entities/player.json
echo '{"size":2}' > $pack/data/shb_test/curios/slots/ring.json
echo '{"size":2}' > $pack/data/shb_test/curios/slots/curio.json
echo '{"values":["minecraft:gold_nugget","minecraft:emerald"]}' > $pack/data/curios/tags/item/ring.json
echo '{"values":["minecraft:heart_of_the_sea"]}' > $pack/data/curios/tags/item/necklace.json
echo '{"values":["minecraft:elytra"]}' > $pack/data/curios/tags/item/back.json
# The offline UUID of "Dev"
uuid=$(python3 -c 'import hashlib,uuid;h=bytearray(hashlib.md5(b"OfflinePlayer:Dev").digest());h[6]=h[6]&0x0f|0x30;h[8]=h[8]&0x3f|0x80;print(uuid.UUID(bytes=bytes(h)))')
echo "[{\"uuid\":\"$uuid\",\"name\":\"Dev\",\"level\":4,\"bypassesPlayerLimit\":false}]" > run-net/ops.json
./gradlew runNetServer --console=plain "$@" > build-netserver.log 2>&1 &
i=0
until grep -qE 'Done \(|Minecraft Crash Report|BUILD FAILED' build-netserver.log 2>/dev/null || [ $i -gt 300 ]; do sleep 2; i=$((i+1)); done
perl -e 'alarm 600; exec @ARGV' ./gradlew runNetClient --console=plain "$@" > build-netclient.log 2>&1
pkill -f 'fml.modFolders=spartanhudbaubles' 2>/dev/null; sleep 3; pkill -9 -f 'fml.modFolders=spartanhudbaubles' 2>/dev/null
wait 2>/dev/null
grep -E '\[scene\] (CHECK|DONE)' build-netclient.log | sed -E 's/^.*\[scene\] //'
echo "--- server: mods and packs"
grep -E 'spartanhudbaubles|Loaded [0-9]+ curio' build-netserver.log | sed -E 's/^\[[^]]*\] //' | cut -c1-200 | head -8
echo "--- server errors"
grep -E '/(ERROR|FATAL)\]|Caused by|Exception|lost connection|kicked' build-netserver.log | sed -E 's/^\[[^]]*\] //' | cut -c1-300 | sort | uniq -c | sort -rn | head -20
echo "--- client errors"
grep -E '/(ERROR|FATAL)\]|Caused by|Exception|Disconnected' build-netclient.log | sed -E 's/^\[[^]]*\] //' | cut -c1-300 | sort | uniq -c | sort -rn | head -20
