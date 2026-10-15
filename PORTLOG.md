# Spartan HUD: Baubles — port log

Port of **Spartan HUD: Baubles 1.0.0** (ObliviousSpartan, Apache-2.0) from Minecraft 1.12.2 (Forge 14, Baubles 1.5.2)
to **Minecraft 1.21.1, NeoForge 21.1** (built with 21.1.262, `neoforge` range `[21.1.197,)`) on **Curios** (built and
tested with 9.5.1, `curios` range `[9.5.1,)`). Mod id `spartanhudbaubles` and package unchanged. Client-side only.

License: the original is Apache-2.0; the port is LGPL-3.0-only (`LICENSE`), the original's license text is kept in
`licenses/LICENSE-APACHE` (no `NOTICE` in the original), `licenses/COPYING` is the GPL-3.0 the LGPL refers to. All
three are in the jar (`LICENSE_spartanhudbaubles`, `licenses/spartanhudbaubles/`).

## Sources

| What | Where |
| --- | --- |
| 1.12.2, 1.0.0 (the spec) | github.com/ObliviousSpartan/SpartanHUD-Baubles, branch `main`, `8a074f4` (the only branch; released as `SpartanHUDBaubles-1.12.2-1.0.0.jar` on CurseForge, 2021-12-14) |
| Issues | the same repository's issue tracker (both read 2026-10-15) |
| Curios 1.21.1 | `top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1` (its `api` jar to compile, the mod in the dev runs) |

## How it was made

- **Java**: ported class by class. `AlignmentHelper`, `SkinType`, `Defaults`, `HudElement` and the layout maths are the
  original's; `HudElementBaubles`, `GuiPreviewHUD`, `GuiConfigSHB`, `Config` and `RenderBaubleOnHUDEvent` are rewritten
  for the 1.21 APIs with the original's behaviour and comments.
- **Resources**: `python3 -I tools/convert_resources.py [<original checkout>]` rebuilds them: `hud.png` and the logo as
  they are, the 1.12 `en_us.lang` converted to `en_us.json` with the keys of NeoForge's config screen
  (`config.spartanhudbaubles:client.alignment` → `spartanhudbaubles.configuration.alignment`), and `tools/lang/*.json`
  on top (the port's new strings and the other five languages). Never edit `src/main/resources/assets` by hand.
- **Tests** (long, run in the background):
  - `tools/scenetest.sh` — client scene `port/DevScene` (83 checks, screenshots; `-Pscene_lang=ru_ru`, `-Pwith_compat`);
  - `tools/scenetest.sh -Pscene=stress` — `port/StressScene` (broken config file, 74 Curios, frame rate, churn, every
    GUI scale, small window, extreme values, rejoining);
  - `tools/nettest.sh` — `port/NetScene`: a dev client joins a dev dedicated server, Curios are equipped with `/curios`;
  - `prodtest/serve.sh` — the release jar on real NeoForge servers (server dirs not in git).

  The scenes are inert unless their system property is set and are not in the jar. They give the player Curio slots
  with a data pack enabled when the world is created (Curios 9.5.1 does not send new slot types to a player already
  in the world after `/reload`), and check that the HUD element is really drawn by comparing the frame with one drawn
  without it.

## Changes forced by 1.21

- **Baubles → Curios.** Baubles had 7 fixed slots; Curios has slot types defined by mods and data packs, any number of
  each. The element shows the equipped Curios in the order of the Curios slot types (as in the Curios inventory), leaves
  out slot types that Curios hides and slots that are disabled. Cosmetic slots are not shown (Baubles had none).
- **Icons**: the original drew its icon and the preview's empty slots from Baubles' `expanded_inventory.png`. The port
  uses the Curios inventory button (`curios:textures/gui/curios/button.png`) and the generic Curio slot icon.
- **Config**: Forge 1.12's `Configuration` → `ModConfigSpec`, file `config/spartanhudbaubles-client.toml`, same option
  names (colours are still a section with `red`, `green`, `blue`, `alpha`). Values are read when the HUD renders,
  so changes apply at once. Unknown values (a wrong `skin_type`) are reset by NeoForge instead of crashing the game
  (the original called `SkinType.valueOf` on the raw string), numbers are clamped to their range.
- **Config GUI**: Forge's `GuiConfig` → NeoForge's `ConfigurationScreen` with a `GuiConfigSHB` section screen that adds
  the **Preview HUD** button at the top left, as before. Skin types and alignments have translated names.
- **Rendering**: `RenderGameOverlayEvent.Post(EXPERIENCE)` → a GUI layer `spartanhudbaubles:baubles` above
  `experience_level`. Hidden with F1, for spectators, with `disable_hud` and behind the Preview HUD screen.
- **Preview HUD**: the mock vanilla HUD (hotbar, offhand slot, crosshair, experience bar, armor, hearts, food, the
  item name) is drawn from the 1.21 HUD sprites. Its **Done** button floated above the crosshair; it is now at the top
  under a "Preview HUD" title, and moves back to the old spot only when the HUD element itself is at the top centre.
- **API**: `RenderBaubleOnHUDEvent.Pre` / `Post` are NeoForge events on `NeoForge.EVENT_BUS`; `ScaledResolution`,
  `GuiIngame` and `FontRenderer` became `GuiGraphics` (its `guiWidth()` / `guiHeight()`) and `Font`; the Curios slot
  type and index are new.

## Bugs of the original fixed

| Issue | Problem | Fix |
| --- | --- | --- |
| [#1](https://github.com/ObliviousSpartan/SpartanHUD-Baubles/issues/1) | The hotbar skin was drawn for 1 to 7 slots only; with more (mods adding bauble slots) the items floated without a background. | The skin is cut into a first slot, a middle slot and a last slot and the middle is repeated: any number of slots. For 1–7 slots the pixels are identical to the original's (checked against `hud.png`). |
| [#1](https://github.com/ObliviousSpartan/SpartanHUD-Baubles/issues/1) | Request: show the empty slots too. | New option `show_empty_slots`: empty slots show their Curios slot icon. |
| [#1](https://github.com/ObliviousSpartan/SpartanHUD-Baubles/issues/1) | Question: is durability updated in real time? | Yes: the element reads the synced Curios every frame (tested on a dedicated server). |
| [#2](https://github.com/ObliviousSpartan/SpartanHUD-Baubles/issues/2) | A ring that broke in its slot stayed on the HUD until the Baubles inventory was opened. | Curios syncs stacks that change in place; the scenes check that a Curio shrunk to nothing in its slot leaves the HUD within ticks, in singleplayer and on a dedicated server. |
| — (code) | A wrong `skin_type` or `alignment` in the config file crashed the game at start (`valueOf` on the raw string; found by reading the code, not reproduced on 1.12.2). | NeoForge validates the values (see above); the stress scene starts with such a file. |

New for Curios, since it has no slot limit: the slots continue in a new row (or column) when the screen is full, and
the option `max_slots_per_line` sets a shorter row. The 7×7 grid in `hud.png`, unused by the original, gives the
multi-row hotbar skin.

The second part of issue #1 (support for "Bring Me the Rings?") and issue #2's mod (Potion Fingers) are 1.12.2 mods;
on 1.21 any mod that adds Curios slots or items is supported.

## Not ported / not verified

- No 1.21 counterpart is missing: everything of 1.0.0 is in the port.
- Accessories (another 1.21 accessory API) is not supported directly; with its Curios compatibility layer it should work
  through the Curios API (not tested).
- Curios versions older than 9.5.1 are not tested, hence the range.
