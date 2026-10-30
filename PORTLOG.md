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
  - `tools/scenetest.sh` — client scene `port/DevScene` (84 checks, screenshots; `-Pscene_lang=ru_ru`, `-Pwith_compat`, `-Pneo_version=21.1.197 -Pno_jei`);
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
  item name) is drawn from the 1.21 HUD sprites. The original's only widget there was a **Done** button above the
  crosshair, which looked like a leftover on an otherwise empty page; the preview now closes with a click anywhere or
  Escape, and a title with that hint is drawn at the top (in the middle when the HUD element is at the top centre).
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

## Test results (2026-10-15)

| Run | Result |
| --- | --- |
| `tools/scenetest.sh` (NeoForge 21.1.262, Curios 9.5.1, JEI) | 84/84 checks, no errors (also in Russian) |
| `tools/scenetest.sh -Pneo_version=21.1.197 -Pno_jei` (the oldest NeoForge of the range, on the client; JEI 19.64 needs 21.1.238+) | 84/84 |
| `tools/scenetest.sh -Pwith_compat` (+ Sodium 0.6.13, Iris 1.8.12, Lithium, FerriteCore, ModernFix, Jade, EMI) | 84/84; the log errors are EMI + JEI duplicate recipe ids and Sodium mixins for absent mods |
| `tools/scenetest.sh -Pscene=stress` | 14/14; 74 Curios on the HUD: 84–96 % of the frame rate without the element (two runs); 94.5–96.7 % after the per-tick slot list (2026-10-17) |
| `tools/nettest.sh` (dev dedicated server) | 6/6, no errors on either side |
| Release jar, real NeoForge 21.1.197 and 21.1.259 servers, alone and with Curios 9.5.1 | starts ("Done"), 0 errors |

## Performance check (2026-10-17)

- The HUD collected and sorted the Curios slots every frame (a Curios slot type lookup for every comparison of the
  sort). The list is now collected once per game tick and drawn from that between ticks: the slots only change with
  the game ticks. 74 Curios: 94.5-96.7 % of the frame rate without the element (84-96 % before), stress scene 14/14.


## Multiloader: NeoForge (Curios) + Fabric (Trinkets) 1.21.1 (2026-10-22, branch `multiloader`)

- Layout as in the Spartan Weaponry port: `common/`, `neoforge/`, `fabric/`. The accessory mod is behind
  `platform/Platform` (collecting the slots, the mod icon, the dummy slot icon, the render event): Curios on NeoForge,
  Trinkets 3.10.0 on Fabric. A slot identifier is the accessory mod's: a Curios slot type (`ring`) or a Trinkets slot
  (`hand/ring`). Empty slot icons: `client/gui/SlotIcon` (a block atlas sprite for Curios, a texture for Trinkets).
- Fabric: the HUD is drawn by a mixin at the end of `Gui.renderExperienceLevel` (NeoForge: the layer above
  `VanillaGuiLayers.EXPERIENCE_LEVEL`; Fabric API 1.21.1 has no GUI layers); the config and its screen with the
  Preview HUD button come from Forge Config API Port (its port of NeoForge's `ConfigurationScreen`; Mod Menu opens
  it); `RenderBaubleOnHUDCallback.PRE` / `POST` is the Fabric event for other mods (NeoForge: `RenderBaubleOnHUDEvent`).
- The common code names NeoForge's `TranslatableEnum`; Forge Config API Port's API jar for common code is
  intermediary-mapped, so the common project compiles against a stub in `common/src/stubs` (in no jar).
- Test scenes: the accessory mod of a scene is `port/SceneAccessories` (Curios / Trinkets). On Fabric the test slots
  are `chest/test_<name>` (Trinkets' own chest/back and chest/necklace would set their order). Trinkets 3.10.0's
  `/trinkets set ... <count>` fails (the count is read as "amount"), so the Fabric dev server of the multiplayer check
  has its own `/shbtest set` (port/FabricNetServerHooks, loaded only with `spartanhudbaubles.scene=netserver`).
- Tests, NeoForge / Fabric: client scene 84/84 / 84/84, stress 13/13 / 13/13 (frame rate with 74 accessories
  298 / 297 FPS, without the HUD element 303 / 306), multiplayer 6/6 / 6/6.

## NeoForge and Fabric release check (2026-10-22, 1.0.0.3)

- Release jar on real NeoForge servers 21.1.197 and 21.1.259, with Curios 9.5.1 and alone, and on a dedicated Fabric
  server with Trinkets 3.10.0: starts clean. The client scene on a Fabric production client (intermediary names): 84/84.
- The first production client run crashed with "The test scenes are not in this build" (`ClassNotFoundException` of
  `port/FabricSceneHooks`, though it is in the scenes jar). Loom's `ClientProductionRunTask` adds the project's
  `remapJar` to `mods` by itself, so the release jar and the scenes jar, both `spartanhudbaubles`, were in one run and
  Fabric picked the release jar. The production client tasks of all four Spartan mods now use `mods.setFrom`.
- `fabric.mod.json` in the jar was not valid JSON: the `\n` in the description became a raw newline when Gradle
  expanded the template (Fabric Loader accepts it, strict parsers do not). Written as `\\n` now.
- Compatibility on both loaders (Sodium 0.6.13, Iris 1.8 with Complementary Reimagined r5.9.3, Lithium, FerriteCore,
  ModernFix, Jade; JEI on NeoForge, EMI on Fabric): client scene 84/84. Only the shader pack's and other mods' optional
  mixin messages in the logs.
- No stress scene and no FPS measurements in this run (owner's call).

## 1.21.11 (branch mc/1.21.11, 2026-10-22, 1.0.0.3+1.21.11)

NeoForge 21.11.48, Curios 14.0.0+1.21.11, JEI 27.48. NeoForge only: Trinkets has no 1.21.11 version, so the Fabric
module is not in this branch. No Parchment for 1.21.11 (plain Mojang names).

- `ResourceLocation` → `Identifier`.
- GUI drawing on render pipelines:
  - `blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, texW, texH)`;
  - `blitSprite(RenderPipelines.GUI_TEXTURED, …)`, the preview's crosshair with `RenderPipelines.CROSSHAIR`;
  - `RenderSystem.enableBlend` / `blendFuncSeparate` are gone.
  - The old 9-argument `blit(texture, x, y, u, v, w, h, texW, texH)` still compiles: it is now
    `blit(texture, x0, x1, y0, y1, u0, u1, v0, v1)` with float UVs, and drew nothing (the Curios button next to the
    element, caught by the scene's frame comparison).
- **Curios 14 keeps the slot icons on the GUI atlas** (before: the block atlas). `SlotIcon` draws them with
  `blitSprite` and checks them on `AtlasIds.GUI`. With the block atlas every empty slot showed the missing texture
  ("every empty slot has its slot icon (10 missing)").
- **Curios 14's `/curios replace` only knows the slot types present when the server started**
  (`CurioArgumentType.slotIds`, filled on `ServerAboutToStartEvent`). The slots of the world's data pack gave "Unknown
  curio slot type ring". The multiplayer check equips through its own dev command (`port/NeoForgeNetServerHooks`, a
  `@Mod(dist = DEDICATED_SERVER)` class in the test sources) with the Curios API.
- Test scenes:
  - `KeyEvent` / `MouseButtonEvent`;
  - `GameRules` in `level.gamerules` (`ADVANCE_TIME`, `ADVANCE_WEATHER`, `SPAWN_MOBS`);
  - `getRespawnData().pos()`;
  - asynchronous `Screenshot.takeScreenshot(target, image -> …)`;
  - `Window.handle()`, `getToastManager()`;
  - `Items.IRON_CHAIN`.
- Compatibility runs: NeoForge 21.11 wraps the GPU device in its validation layer in dev runs
  (`enableB3DValidationLayer` in `neoforge-client.toml`). Sodium 0.8 and Iris 1.10 cast it to `GlDevice` and crash
  (`ValidationGpuDevice cannot be cast to GlDevice`). `tools/compattest.sh` turns the layer off for the run and on
  again after it.
- The frame comparison of the scene allows one changed pixel where nothing should be drawn: under shaders the world
  moves between the baseline frame and the checked one (a 2 × 2 corner had 1 pixel changed).
- Checks:
  - client scene 84/84, stress 13/13, multiplayer 6/6;
  - release jar on a real NeoForge 21.11.48 server with Curios and alone: starts clean;
  - compatibility (Sodium 0.8.14, Iris 1.10.8 with Complementary Reimagined r5.9.3, Lithium 0.21.4, FerriteCore 8.2.0,
    Jade 21.1.7, JEI 27.48): client scene 84/84 with the shader pack on;
  - no FPS measurements (owner's call).

## 26.1.2 (branch mc/26.1.2, 2026-10-23, 1.0.0.3+26.1.2)

NeoForge 26.1.2.122, Curios 15.0.0+26.1.2, JEI 29.47, Java 25. NeoForge only: Trinkets has no 26.1.2 version. No
Parchment for 26.x (no obfuscation).

- GUI drawing is extraction: `GuiGraphicsExtractor` with `text`, `item`, `blit`/`blitSprite` on pipelines; the
  element's own `render(GuiGraphicsExtractor)` keeps its name (the GUI port script renamed it to `extractRenderState`).
- **The slot data pack of the test world is made from item types**: `new ItemStack` at the title screen crashes on 26.1
  ("Components not bound yet"), so the scene lists items, not stacks, before the world exists.
- Test scenes: `LevelSettings(name, mode, new DifficultySettings(...), cheats, packs)`; time and weather by command (the
  time API moved to clocks); `Minecraft#resizeGui`; `ItemInput#createItemStack(count)` in the multiplayer command.
- Checks:
  - client scene 84/84, multiplayer 6/6;
  - release jar on a real NeoForge 26.1.2.122 server with Curios and alone: starts clean;
  - compatibility (Sodium 0.9.2, Iris 1.11.4 with Complementary Reimagined r5.9.3, Lithium 0.24.7, FerriteCore 9.0.0,
    Jade 26.1.12, ModernFix 5.27.24, JEI 29.47): client scene 84/84 with the shader pack on;
  - no stress scene and no FPS measurements on 26.1.2 (owner's call).

## Port bug: crash when an accessory is taken out (2026-10-31, [our issue #1](https://github.com/Vladeezyy/spartan-hud-baubles-1.21/issues/1))

- Reported on NeoForge 1.21.1 with a trinket that adds slots: taking it out crashed the game with
  `NullPointerException: Cannot invoke "SlotIcon.draw(...)" because the return value of "HudElementBaubles$Bauble.icon()" is null`.
- Cause (all versions, both loaders): the element collects the slots once per tick, and the list holds the live stacks.
  Taking an item out in the inventory splits its stack, so the stack in the list turns empty in place between two
  ticks; the element drew it as an empty slot, but a filled slot is collected without an icon. Not specific to items
  that add slots: any item taken out between ticks while the HUD draws.
- Fix: an empty stack without an icon draws nothing until the next tick collects the slots again (one frame at most).
  No extra work per frame.
- Test: the client scene empties the last ring's stack inside a frame, from the Pre event of the first ring (as the
  inventory does), and checks that the frame goes on. Without the fix the scene crashes with the reported NPE.
- Client scene 85/85 on NeoForge 26.1.2.
