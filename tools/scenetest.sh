#!/bin/sh
# Runs a client test scene (max 15 min) on LOADER=neoforge|fabric: -Pscene=dev (port/DevScene, default) or
# -Pscene=stress (port/StressScene). Screenshots in <loader>/run/screenshots/scene_*.png; prints the functional checks,
# untranslated config strings and the errors of the run. A crash report stops the client. Extra gradle arguments are
# passed on (e.g. -Pscene_lang=ru_ru, -Pwith_compat).
. "$(dirname "$0")/common.sh"
LOG=build-scene-$LOADER.log
rm -rf "$RUN/saves/shb_scene" "$RUN/saves/shb_stress" "$RUN"/screenshots/scene_*.png "$LOG"
# Every run starts with the default config; the stress scene with a broken config file (the original crashed on an unknown skin_type)
rm -f "$RUN/config/spartanhudbaubles-client.toml"
case "$*" in *scene=stress*)
	mkdir -p "$RUN/config"
	printf 'skin_type = "FOO"\nalignment = 5\noffset_x = 99999\n' > "$RUN/config/spartanhudbaubles-client.toml" ;;
esac
run_watched "$LOG" 900 ":$LOADER:runScene" "$@"
grep -E '\[scene\] (CHECK|DONE|PERF)' "$LOG" | sed -E 's/^.*\[scene\] //'
ls "$RUN/screenshots/" 2>/dev/null | grep scene_
echo "--- untranslated config strings"
grep -A40 'translation' "$LOG" | grep -E '"spartanhudbaubles\.' | sort -u | head -20
echo "--- errors"
errors "$LOG"
