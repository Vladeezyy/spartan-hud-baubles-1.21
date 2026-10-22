#!/bin/sh
# Compatibility run on LOADER=neoforge|fabric: the client scene with -Pwith_compat (Sodium, Iris with a shader pack
# from run/shaderpacks, Lithium, FerriteCore, ModernFix, Jade, EMI). Output: build-compat-<loader>.out, screenshots in
# build/compat-<loader>/.
. "$(dirname "$0")/common.sh"
mkdir -p "$RUN/shaderpacks" "$RUN/config"
cp run/shaderpacks/*.zip "$RUN/shaderpacks/" 2>/dev/null
[ -f run/config/iris.properties ] && cp run/config/iris.properties "$RUN/config/"
OUT=build-compat-$LOADER.out
tools/scenetest.sh -Pwith_compat > $OUT 2>&1
rm -rf build/compat-$LOADER && mkdir -p build/compat-$LOADER && cp "$RUN"/screenshots/scene_*.png build/compat-$LOADER/ 2>/dev/null
echo "=== $OUT: $(grep -c 'CHECK' $OUT) checks, $(grep -oE 'DONE, failed checks: [0-9]+' $OUT)"
grep 'CHECK FAIL' $OUT | head -10
sed -n '/--- untranslated/,$p' $OUT | head -14
