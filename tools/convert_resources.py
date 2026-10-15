#!/usr/bin/env python3
"""Rebuilds the mod's resources from the original Spartan HUD: Baubles 1.0.0 (1.12.2) sources.

Usage: tools/convert_resources.py [<original repo>]   (default ~/Desktop/mod-scout/src/SpartanHUD-Baubles)

- textures/gui/hud.png: copied as is (the hotbar skin; the HUD element cuts it into pieces);
- textures/logo.png -> logo.png at the jar root (logoFile in neoforge.mods.toml);
- lang/en_us.lang (1.12 "key=value") -> lang/en_us.json with the keys of NeoForge's config screen
  ("config.spartanhudbaubles:client.alignment" -> "spartanhudbaubles.configuration.alignment");
- tools/lang/<lang>.json is merged on top: the strings the port adds (tooltips, enum values, new options) and the
  other languages. Every language must end up with exactly the keys of en_us.
"""
import json
import shutil
import sys
from pathlib import Path

MOD_ID = 'spartanhudbaubles'
ROOT = Path(__file__).resolve().parent.parent
ORIGINAL = Path(sys.argv[1]) if len(sys.argv) > 1 else Path.home() / 'Desktop/mod-scout/src/SpartanHUD-Baubles'
SRC = ORIGINAL / 'src/main/resources'
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets' / MOD_ID
OVERLAY = ROOT / 'tools/lang'


def convert_key(key):
    prefix = f'config.{MOD_ID}:'
    if not key.startswith(prefix):
        raise ValueError(f'unexpected key {key}')
    name = key[len(prefix):]
    if name.startswith('client.'):
        name = name[len('client.'):]
    return f'{MOD_ID}.configuration.{name}'


def read_lang(path):
    entries = {}
    for line in path.read_text(encoding='utf-8-sig').splitlines():
        line = line.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        key, value = line.split('=', 1)
        entries[convert_key(key.strip())] = value.strip()
    return entries


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(dict(sorted(data.items())), ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def main():
    (ASSETS / 'textures/gui').mkdir(parents=True, exist_ok=True)
    shutil.copyfile(SRC / f'assets/{MOD_ID}/textures/gui/hud.png', ASSETS / 'textures/gui/hud.png')
    shutil.copyfile(SRC / f'assets/{MOD_ID}/textures/logo.png', RES / 'logo.png')

    english = read_lang(SRC / f'assets/{MOD_ID}/lang/en_us.lang')
    english.update(json.loads((OVERLAY / 'en_us.json').read_text(encoding='utf-8')))
    write_json(ASSETS / 'lang/en_us.json', english)
    problems = 0
    for overlay in sorted(OVERLAY.glob('*.json')):
        if overlay.stem == 'en_us':
            continue
        data = json.loads(overlay.read_text(encoding='utf-8'))
        missing = sorted(set(english) - set(data))
        extra = sorted(set(data) - set(english))
        if missing or extra:
            problems += 1
            print(f'{overlay.stem}: missing {missing}, extra {extra}')
        write_json(ASSETS / f'lang/{overlay.stem}.json', data)
    print(f'{len(english)} English strings, {len(list(OVERLAY.glob("*.json")))} languages, {problems} with problems')
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
