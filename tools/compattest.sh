#!/bin/sh
# Compatibility run on LOADER=neoforge|fabric: the client scene with -Pwith_compat (Sodium, Iris with a shader pack
# from run/shaderpacks, Lithium, FerriteCore, Jade; 26.3 has no ModernFix or EMI). Output: build-compat-<loader>.out, screenshots in
# build/compat-<loader>/.
. "$(dirname "$0")/common.sh"
mkdir -p "$RUN/shaderpacks" "$RUN/config"
cp run/shaderpacks/*.zip "$RUN/shaderpacks/" 2>/dev/null
[ -f run/config/iris.properties ] && cp run/config/iris.properties "$RUN/config/"
# NO_SHADERS=1: Iris without its shader pack (Iris 1.11.7 for NeoForge 26.3 crashes in its render passes with one on:
# "Missing uniform InSampler")
[ -n "$NO_SHADERS" ] && sed -i.bak 's/^enableShaders=.*/enableShaders=false/' "$RUN/config/iris.properties" && rm -f "$RUN/config/iris.properties.bak"
# NeoForge 21.11 wraps the GPU device in its validation layer in dev runs; Sodium and Iris cast it to GlDevice and crash
C="$RUN/config/neoforge-client.toml"
if [ -f "$C" ] && grep -q enableB3DValidationLayer "$C"; then sed -i.bak 's/^enableB3DValidationLayer.*/enableB3DValidationLayer = false/' "$C"; else echo 'enableB3DValidationLayer = false' >> "$C"; fi
OUT=build-compat-$LOADER.out
tools/scenetest.sh -Pwith_compat > $OUT 2>&1
rm -rf build/compat-$LOADER && mkdir -p build/compat-$LOADER && cp "$RUN"/screenshots/scene_*.png build/compat-$LOADER/ 2>/dev/null
echo "=== $OUT: $(grep -c 'CHECK' $OUT) checks, $(grep -oE 'DONE, failed checks: [0-9]+' $OUT)"
grep 'CHECK FAIL' $OUT | head -10
sed -n '/--- untranslated/,$p' $OUT | head -14
# The other dev runs keep NeoForge's validation layer
[ -f "$C" ] && sed -i.bak 's/^enableB3DValidationLayer.*/enableB3DValidationLayer = true/' "$C" && rm -f "$C.bak"
