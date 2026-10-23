package com.oblivioussp.spartanhudbaubles.port;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;
import com.oblivioussp.spartanhudbaubles.client.gui.AlignmentHelper.Alignment;
import com.oblivioussp.spartanhudbaubles.client.gui.GuiPreviewHUD;
import com.oblivioussp.spartanhudbaubles.client.gui.HudElementBaubles;
import com.oblivioussp.spartanhudbaubles.client.gui.HudElementBaubles.Bauble;
import com.oblivioussp.spartanhudbaubles.util.Config;
import com.oblivioussp.spartanhudbaubles.util.Log;
import com.oblivioussp.spartanhudbaubles.util.SkinType;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Development scene, inert unless the {@code spartanhudbaubles.scene} system property is {@code dev}
 * ({@code ./gradlew runScene}, {@code tools/scenetest.sh}): creates the world {@code shb_scene}, gives the player
 * Curio slots with a data pack, equips Curios and checks the HUD element (what is collected, where it is, that the
 * skin and items are really drawn: the frame is compared with one drawn without the HUD), the config values, the config
 * GUI and the Preview HUD screen through real clicks, and the two issues of the original. Logs {@code [scene] CHECK},
 * saves {@code run/screenshots/scene_*.png}, then quits. {@code -Dspartanhudbaubles.scene.lang=ru_ru} switches the
 * language.
 */
public final class DevScene
{
	static final String MODE = System.getProperty("spartanhudbaubles.scene", "");
	/** The accessory mod of the scenes (set by the loader's port/*SceneHooks) */
	static SceneAccessories accessories;
	private static final boolean ENABLED = MODE.equals("dev");
	static final String LANG = System.getProperty("spartanhudbaubles.scene.lang", "en_us");
	private static final String WORLD = "shb_scene";
	private static final List<Pair<Integer, Consumer<Minecraft>>> STEPS = new ArrayList<>();
	private static boolean opened;
	private static boolean menuPhase;
	private static int tick;
	private static int menuTick;
	static int failures;
	// The baubles drawn in the last frame: RenderBaubleOnHUDEvent.Pre positions, and how many Post events followed
	static final List<int[]> framePre = new ArrayList<>();
	static int framePost;
	private static final List<int[]> collectingPre = new ArrayList<>();
	private static int collectingPost;
	private static int[] baseline;
	private static int baselineWidth;
	private static ItemStack brokenRing;
	// Ticks left in which the open screen is logged and saved (closing the config GUI)
	private static int watchScreens;
	private static int watchShots;

	static
	{
		int t = 40;
		at(t, mc -> server(mc, DevScene::setup));
		at(t += 20, DevScene::checkSlots);
		// Nothing equipped: nothing drawn
		t = measure(t + 5, "no curios", () -> {}, mc -> {
			check("no curios: nothing collected", HudElementBaubles.getActive() == null || HudElementBaubles.getActive().getLastRendered().isEmpty(), "");
			check("no curios: no bauble rendered", framePre.isEmpty(), framePre.size());
		});
		// The original's screenshot: two rings with the default config
		at(t += 20, mc -> server(mc, p -> equip(p, twoRings())));
		t = measure(t + 20, "two rings", () -> {}, mc -> {
			HudElementBaubles hud = HudElementBaubles.getActive();
			int gw = mc.getWindow().getGuiScaledWidth(), gh = mc.getWindow().getGuiScaledHeight();
			check("two rings: collected in slot order", slots(hud.getLastRendered()).equals(expected("ring#0", "ring#1")), slots(hud.getLastRendered()));
			check("two rings: bottom right, 40 x 22", hud.getLastX() == gw - 40 && hud.getLastY() == gh - 22 && hud.getWidth() == 40 && hud.getHeight() == 22,
					hud.getLastX() + "," + hud.getLastY() + " " + hud.getWidth() + "x" + hud.getHeight() + " on " + gw + "x" + gh);
			check("two rings: Pre/Post events at the slots", framePre.size() == 2 && framePost == 2 && framePre.get(0)[0] == gw - 37 && framePre.get(1)[0] == gw - 19 && framePre.get(0)[1] == gh - 19,
					positions(framePre) + ", post " + framePost);
			checkDrawn(mc, "two rings: hotbar skin drawn", hud.getLastX(), hud.getLastY(), hud.getWidth(), hud.getHeight(), true);
			checkDrawn(mc, "two rings: accessory mod icon drawn left of it", hud.getLastX() - 11, hud.getLastY() + 6, 10, 10, true);
		});
		at(t += 2, mc -> shot(mc, "default_two"));
		// Seven: the most Baubles had
		at(t += 3, mc -> server(mc, p -> equip(p, firstOf(allItems(), 7))));
		t = measure(t + 15, "seven", () -> {}, mc -> {
			HudElementBaubles hud = HudElementBaubles.getActive();
			check("seven: 130 x 22 as in the original", hud.getWidth() == 130 && hud.getHeight() == 22 && framePre.size() == 7, hud.getWidth() + "x" + hud.getHeight() + ", " + framePre.size() + " drawn");
			checkDrawn(mc, "seven: hotbar skin drawn", hud.getLastX(), hud.getLastY(), hud.getWidth(), hud.getHeight(), true);
		});
		at(t += 2, mc -> shot(mc, "seven"));
		// Twelve: the original drew no hotbar skin for more than seven (issue #1)
		at(t += 3, mc -> server(mc, p -> equip(p, allItems())));
		t = measure(t + 15, "twelve", () -> {}, mc -> {
			HudElementBaubles hud = HudElementBaubles.getActive();
			check("twelve: collected in slot order", slots(hud.getLastRendered()).equals(expected("curio#0", "curio#1", "head#0", "necklace#0", "back#0", "body#0", "bracelet#0", "hands#0", "ring#0", "ring#1", "belt#0", "charm#0")),
					slots(hud.getLastRendered()));
			check("twelve: one row, 220 x 22", hud.getWidth() == 220 && hud.getHeight() == 22 && framePre.size() == 12, hud.getWidth() + "x" + hud.getHeight());
			checkDrawn(mc, "twelve: hotbar skin drawn for more than 7 slots (issue #1)", hud.getLastX(), hud.getLastY(), hud.getWidth(), hud.getHeight(), true);
			checkDrawn(mc, "twelve: last slot's skin drawn", hud.getLastX() + hud.getWidth() - 19, hud.getLastY(), 19, 22, true);
			ItemStack amethyst = clientStack(mc, "curio", 1);
			check("twelve: stack count synced (16 amethyst shards)", amethyst.getCount() == 16, amethyst);
		});
		at(t += 2, mc -> shot(mc, "twelve"));
		// Durability shown and updated live (question in issue #1)
		at(t += 3, mc -> server(mc, p -> stack(p, "back", 0).setDamageValue(400)));
		at(t += 10, mc -> check("durability: the elytra's damage reaches the client", clientStack(mc, "back", 0).getDamageValue() == 400, clientStack(mc, "back", 0).getDamageValue()));
		// Issue #2: a Curio that breaks in its slot (the stack shrinks without the slot being set) leaves the HUD
		at(t += 3, mc -> server(mc, p -> {
			brokenRing = stack(p, "ring", 1);
			brokenRing.shrink(1);
		}));
		at(t += 10, mc -> {
			HudElementBaubles hud = HudElementBaubles.getActive();
			check("broken ring leaves the HUD without opening the inventory (issue #2)", !slots(hud.getLastRendered()).contains(expected("ring#1").get(0)) && framePre.size() == 11, slots(hud.getLastRendered()));
		});
		// More lines
		t = measure(t + 5, "wrapped", () -> Config.MAX_SLOTS_PER_LINE.set(5), mc -> {
			HudElementBaubles hud = HudElementBaubles.getActive();
			check("max 5 per line: 11 slots in 3 rows, 94 x 58", hud.getWidth() == 94 && hud.getHeight() == 58, hud.getWidth() + "x" + hud.getHeight());
			check("max 5 per line: 6th slot starts row 2", framePre.size() == 11 && framePre.get(5)[0] == framePre.get(0)[0] && framePre.get(5)[1] == framePre.get(0)[1] + 18, positions(framePre));
			checkDrawn(mc, "max 5 per line: grid skin drawn", hud.getLastX(), hud.getLastY(), hud.getWidth(), hud.getHeight(), true);
		});
		at(t += 2, mc -> shot(mc, "wrapped"));
		t = measure(t + 3, "wrapped vertical", () -> Config.ROTATE_VERTICAL.set(true), mc -> {
			HudElementBaubles hud = HudElementBaubles.getActive();
			check("vertical, max 5: 3 columns, 58 x 94", hud.getWidth() == 58 && hud.getHeight() == 94, hud.getWidth() + "x" + hud.getHeight());
			checkDrawn(mc, "vertical, max 5: grid skin drawn", hud.getLastX(), hud.getLastY(), hud.getWidth(), hud.getHeight(), true);
		});
		at(t += 2, mc -> {
			shot(mc, "wrapped_vertical");
			Config.ROTATE_VERTICAL.set(false);
			Config.MAX_SLOTS_PER_LINE.set(0);
		});
		// Empty slots (issue #1 asks for them)
		at(t += 3, mc -> server(mc, p -> equip(p, twoRings())));
		t = measure(t + 15, "empty slots", () -> Config.SHOW_EMPTY_SLOTS.set(true), mc -> {
			HudElementBaubles hud = HudElementBaubles.getActive();
			List<Bauble> shown = hud.getLastRendered();
			check("show_empty_slots: all 12 slots, 2 of them with an item", shown.size() == 12 && shown.stream().filter(b -> !b.stack().isEmpty()).count() == 2 && framePre.size() == 2, slots(shown) + ", drawn " + framePre.size());
			long missing = shown.stream().filter(b -> b.stack().isEmpty()).filter(b -> b.icon().isMissing(mc)).count();
			check("show_empty_slots: every empty slot has its slot icon", missing == 0, missing + " missing");
			checkDrawn(mc, "show_empty_slots: skin drawn", hud.getLastX(), hud.getLastY(), hud.getWidth(), hud.getHeight(), true);
		});
		at(t += 2, mc -> {
			shot(mc, "empty_slots");
			Config.SHOW_EMPTY_SLOTS.set(false);
		});
		// Skins
		at(t += 3, mc -> server(mc, p -> equip(p, firstOf(allItems(), 6))));
		t = measure(t + 15, "tooltip skin", () -> Config.SKIN_TYPE.set(SkinType.TOOLTIP_STYLE), mc -> {
			HudElementBaubles hud = HudElementBaubles.getActive();
			checkDrawn(mc, "tooltip skin drawn", hud.getLastX(), hud.getLastY(), hud.getWidth(), hud.getHeight(), true);
		});
		at(t += 2, mc -> shot(mc, "skin_tooltip"));
		t = measure(t + 3, "minimal skin", () -> Config.SKIN_TYPE.set(SkinType.MINIMAL), mc -> {
			HudElementBaubles hud = HudElementBaubles.getActive();
			// The gradient starts transparent at the top: check its lower half
			checkDrawn(mc, "minimal skin drawn", hud.getLastX(), hud.getLastY() + hud.getHeight() / 2, hud.getWidth(), hud.getHeight() / 2, true);
		});
		at(t += 2, mc -> shot(mc, "skin_minimal"));
		t = measure(t + 3, "no skin", () -> Config.SKIN_TYPE.set(SkinType.NONE), mc -> {
			HudElementBaubles hud = HudElementBaubles.getActive();
			check("no skin: items still drawn", framePre.size() == 6, framePre.size());
			checkDrawn(mc, "no skin: no accessory mod icon", hud.getLastX() - 11, hud.getLastY() + 6, 10, 10, false);
			checkDrawn(mc, "no skin: no frame (corner pixel)", hud.getLastX(), hud.getLastY(), 2, 2, false);
		});
		at(t += 2, mc -> shot(mc, "skin_none"));
		at(t += 3, mc -> Config.SKIN_TYPE.set(SkinType.HOTBAR_STYLE));
		// Alignments: every one stays on the screen and keeps clear of the crosshair and the hotbar as in the original
		for(Alignment alignment : Alignment.values())
			for(boolean vertical : new boolean[] { false, true })
				t = measure(t + 4, alignment + (vertical ? " vertical" : ""), () -> {
					Config.ALIGNMENT.set(alignment);
					Config.ROTATE_VERTICAL.set(vertical);
				}, mc -> checkAlignment(mc, alignment, vertical));
		at(t += 12, mc -> {
			Config.ALIGNMENT.set(Alignment.TOP_LEFT);
			Config.ROTATE_VERTICAL.set(true);
			Config.SKIN_TYPE.set(SkinType.TOOLTIP_STYLE);
		});
		at(t += 5, mc -> shot(mc, "top_left_vertical_tooltip"));
		at(t += 3, mc -> {
			Config.ALIGNMENT.set(Alignment.CENTER);
			Config.ROTATE_VERTICAL.set(false);
			Config.SKIN_TYPE.set(SkinType.HOTBAR_STYLE);
		});
		at(t += 5, mc -> shot(mc, "center"));
		// Offsets
		at(t += 3, mc -> {
			Config.ALIGNMENT.set(Alignment.BOTTOM_RIGHT);
			Config.OFFSET_X.set(-10);
			Config.OFFSET_Y.set(-30);
		});
		at(t += 5, mc -> {
			HudElementBaubles hud = HudElementBaubles.getActive();
			int gw = mc.getWindow().getGuiScaledWidth(), gh = mc.getWindow().getGuiScaledHeight();
			check("offsets -10 / -30 move the element", hud.getLastX() == gw - hud.getWidth() - 10 && hud.getLastY() == gh - 22 - 30, hud.getLastX() + "," + hud.getLastY());
			Config.OFFSET_X.set(0);
			Config.OFFSET_Y.set(0);
		});
		// Hidden
		at(t += 3, mc -> Config.DISABLE_HUD.set(true));
		at(t += 5, mc -> check("disable_hud: nothing drawn", framePre.isEmpty(), framePre.size()));
		at(t += 1, mc -> {
			Config.DISABLE_HUD.set(false);
			setHudHidden(mc, true);
		});
		at(t += 5, mc -> check("F1 (hidden GUI): nothing drawn", framePre.isEmpty(), framePre.size()));
		at(t += 1, mc -> {
			setHudHidden(mc, false);
			server(mc, p -> p.setGameMode(GameType.SPECTATOR));
		});
		at(t += 10, mc -> check("spectator: nothing drawn", framePre.isEmpty(), framePre.size()));
		at(t += 1, mc -> server(mc, p -> p.setGameMode(GameType.SURVIVAL)));
		at(t += 10, mc -> check("survival again: drawn", framePre.size() == 6, framePre.size()));
		// The config GUI from the mods list, the Preview HUD button, a value changed by a click
		// 26.3: with no parent NeoForge's config screen closes itself inside Gui.setScreen on the way back (NPE in setScreen);
		// in game it is opened from the pause menu's Mods list, so the scene opens it over the pause menu
		at(t += 3, mc -> openConfig(mc, new net.minecraft.client.gui.screens.PauseScreen(true)));
		at(t += 10, mc -> check("config GUI opens on the client settings with the Preview HUD button", isConfigScreen(mc.gui.screen()) && previewButton(mc.gui.screen()) != null, mc.gui.screen()));
		at(t += 2, mc -> shot(mc, "config"));
		at(t += 3, mc -> click(mc, previewButton(mc.gui.screen())));
		at(t += 5, mc -> {
			check("Preview HUD button opens the preview", mc.gui.screen() instanceof GuiPreviewHUD, mc.gui.screen());
			check("preview: no in-game HUD element behind it", framePre.isEmpty(), framePre.size());
		});
		at(t += 3, mc -> shot(mc, "preview_ingame"));
		at(t += 3, mc -> {
			if(mc.gui.screen() instanceof GuiPreviewHUD preview)
			{
				check("preview: 7 dummy slots, bottom right", preview.getHud().getWidth() == 130 && preview.getHud().getLastX() == mc.getWindow().getGuiScaledWidth() - 130, preview.getHud().getLastX());
				check("preview: no lone button, title and hint at the top", preview.children().isEmpty() && preview.getTextY() == 8, preview.children().size() + ", text at " + preview.getTextY());
			}
			mc.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
		});
		at(t += 5, mc -> {
			check("Escape returns to the config GUI", isConfigScreen(mc.gui.screen()), mc.gui.screen());
			click(mc, valueButton(mc.gui.screen(), Config.SKIN_TYPE.get()));
		});
		at(t += 5, mc -> {
			check("clicking the skin type button changes the value (hotbar -> tooltip)", Config.SKIN_TYPE.get() == SkinType.TOOLTIP_STYLE, Config.SKIN_TYPE.get());
			click(mc, previewButton(mc.gui.screen()));
		});
		at(t += 5, mc -> shot(mc, "preview_ingame_tooltip"));
		at(t += 3, mc -> Config.ALIGNMENT.set(Alignment.TOP_CENTER));
		at(t += 3, mc -> {
			if(mc.gui.screen() instanceof GuiPreviewHUD preview)
				check("preview: title and hint move out of a top centre HUD", preview.getTextY() == preview.height / 2 - 50, preview.getTextY());
			shot(mc, "preview_top_center");
			Config.ALIGNMENT.set(Alignment.BOTTOM_RIGHT);
		});
		at(t += 3, mc -> {
			// A click anywhere closes the preview
			mc.gui.screen().mouseClicked(new MouseButtonEvent(100, 100, new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
		});
		at(t += 5, mc -> {
			check("a click anywhere closes the preview", isConfigScreen(mc.gui.screen()), mc.gui.screen());
			mc.gui.screen().onClose();
			watchScreens = 12;
		});
		at(t += 10, mc -> {
			String file = readConfigFile();
			check("closing the config GUI saves the file", (mc.gui.screen() == null || mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) && file.contains("skin_type = \"TOOLTIP_STYLE\""), file.lines().filter(l -> l.contains("skin_type")).findFirst().orElse(file.isEmpty() ? "no file" : "no skin_type"));
			mc.gui.setScreen(null);
			Config.SKIN_TYPE.set(SkinType.HOTBAR_STYLE);
			Config.SPEC.save();
			checkTranslations();
		});
		// The world's README screenshot: survival HUD with the Curios
		at(t += 3, mc -> server(mc, p -> equip(p, firstOf(allItems(), 5))));
		at(t += 15, mc -> shot(mc, "hud"));
		at(t += 3, mc -> {
			// Then the preview from the main menu (the mock HUD)
			menuPhase = true;
			mc.level.disconnect(Component.empty());
			mc.disconnect(new TitleScreen(), false);
		});
	}

	static void at(int t, Consumer<Minecraft> action)
	{
		STEPS.add(Pair.of(t, action));
	}

	/**
	 * Draws one frame without the HUD element, applies the config change, then compares (10 ticks in all)
	 */
	static int measure(int t, String name, Runnable configure, Consumer<Minecraft> checks)
	{
		at(t, mc -> {
			configure.run();
			Config.DISABLE_HUD.set(true);
		});
		at(t + 4, mc -> {
			captureBaseline(mc);
			Config.DISABLE_HUD.set(false);
		});
		at(t + 8, checks);
		return t + 8;
	}

	// The loader modules call this after every client tick (port/*SceneHooks)
	public static void onTick()
	{
		if(!ENABLED)
			return;
		Minecraft mc = Minecraft.getInstance();
		if(!opened && mc.gui.screen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)
		{
			mc.options.onboardAccessibility = false;
			mc.options.save();
			mc.gui.setScreen(new TitleScreen());
			return;
		}
		// The language switch reloads the resources; the world is created after that, not during it
		if(!opened && mc.gui.overlay() != null)
			return;
		// NeoForge 26.2: load warnings (e.g. another mod's deprecated logoFile) open LoadingErrorScreen before the menu
		if(!opened && mc.gui.screen() != null && mc.gui.screen().getClass().getSimpleName().equals("LoadingErrorScreen"))
		{
			Log.info("[scene] NeoForge load warnings screen, going on to the main menu");
			mc.gui.setScreen(new TitleScreen());
			return;
		}
		if(!opened && mc.gui.screen() instanceof TitleScreen)
		{
			if(!mc.options.languageCode.equals(LANG))
			{
				mc.options.languageCode = LANG;
				mc.getLanguageManager().setSelected(LANG);
				mc.options.save();
				mc.reloadResourcePacks();
				return;
			}
			opened = true;
			mc.getWindow().setWindowed(1600, 900);
			mc.options.guiScale().set(3);
			mc.options.cloudStatus().set(CloudStatus.OFF);
			mc.options.pauseOnLostFocus = false;
			mc.resizeGui();
			// The slot data pack is in the world from the start (Curios does not send new slot types to a player who is
			// already in the world after /reload; mods' slots exist at login anyway)
			try
			{
				writeSlotsDataPack(mc.getLevelSource().getBaseDir().resolve(WORLD).resolve("datapacks/shb_slots"));
			}
			catch(IOException e)
			{
				check("test data pack written", false, e);
			}
			WorldDataConfiguration packs = new WorldDataConfiguration(new net.minecraft.world.level.DataPackConfig(List.of("vanilla", "mod_data", "file/shb_slots"), List.of()),
					net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
			LevelSettings settings = new LevelSettings(WORLD, GameType.SURVIVAL, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, packs);
			mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261013L, false, false), WorldPresets::createNormalWorldDimensions, new TitleScreen());
			return;
		}
		if(watchScreens > 0)
		{
			watchScreens--;
			Log.info("[scene] screen after closing: " + (mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getName()));
			if(mc.gui.screen() != null && !(mc.gui.screen() instanceof TitleScreen))
				shot(mc, "closing_" + watchShots++);
		}
		if(menuPhase)
		{
			menuSteps(mc);
			return;
		}
		if(mc.player == null || mc.getSingleplayerServer() == null)
			return;
		mc.options.pauseOnLostFocus = false;
		mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
		mc.gui.toastManager().clear();
		if(mc.gui.screen() instanceof PauseScreen)
			mc.gui.setScreen(null);
		tick++;
		for(Pair<Integer, Consumer<Minecraft>> step : STEPS)
		{
			if(step.getFirst() == tick)
			{
				Log.info("[scene] tick " + tick);
				try
				{
					step.getSecond().accept(mc);
				}
				catch(RuntimeException | LinkageError e)
				{
					check("step at tick " + tick + " ran", false, e);
					Log.error("[scene] step failed", e);
				}
			}
		}
	}

	/** The main menu: the config GUI from the title screen and its Preview HUD with the mock vanilla HUD */
	private static void menuSteps(Minecraft mc)
	{
		if(!(mc.gui.screen() instanceof TitleScreen) && menuTick == 0)
			return;
		menuTick++;
		try
		{
			switch(menuTick)
			{
				case 20 -> openConfig(mc, mc.gui.screen());
				case 30 -> click(mc, previewButton(mc.gui.screen()));
				case 40 -> {
					check("main menu: Preview HUD opens without a world", mc.gui.screen() instanceof GuiPreviewHUD && mc.level == null, mc.gui.screen());
					shot(mc, "preview_menu");
				}
				case 50 -> {
					mc.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
					mc.gui.screen().onClose();
					watchScreens = 8;
				}
				case 60 -> {
					check("main menu: back on the title screen", mc.gui.screen() instanceof TitleScreen, mc.gui.screen());
					Log.info("[scene] DONE, failed checks: " + failures);
					mc.stop();
				}
				default -> {}
			}
		}
		catch(RuntimeException e)
		{
			check("menu step " + menuTick + " ran", false, e);
			Log.error("[scene] menu step failed", e);
		}
	}

	private static String lastScreen = "";
	private static int screenShots;

	/** Logs and saves every new look of a screen (its class and widgets), to see every screen the player sees */
	/** After a screen is drawn (NeoForge ScreenEvent.Render.Post, Fabric ScreenEvents.afterRender) */
	public static void onScreenRendered(Screen screen)
	{
		if(!ENABLED)
			return;
		List<AbstractWidget> found = new ArrayList<>();
		collectWidgets(screen.children(), found);
		String signature = screen.getClass().getSimpleName() + " " + found.stream().filter(w -> w.visible).map(w -> w.getMessage().getString() + "@" + w.getX() + "," + w.getY()).toList();
		if(signature.equals(lastScreen))
			return;
		lastScreen = signature;
		Log.info("[scene] SCREEN " + screenShots + " " + signature);
		shot(Minecraft.getInstance(), "screen_" + screenShots++);
	}

	/** Between two frames of the GUI (NeoForge RenderGuiEvent.Pre, Fabric: after the HUD is drawn) */
	public static void onFrame()
	{
		if(MODE.isEmpty())
			return;
		framePre.clear();
		framePre.addAll(collectingPre);
		framePost = collectingPost;
		collectingPre.clear();
		collectingPost = 0;
	}

	/** RenderBaubleOnHUDEvent.Pre (Fabric: RenderBaubleOnHUDCallback.PRE) */
	public static void onBaublePre(int x, int y)
	{
		if(!MODE.isEmpty())
			collectingPre.add(new int[] { x, y });
	}

	public static void onBaublePost()
	{
		if(!MODE.isEmpty())
			collectingPost++;
	}

	static void server(Minecraft mc, Consumer<ServerPlayer> action)
	{
		MinecraftServer server = mc.getSingleplayerServer();
		server.execute(() ->
		{
			try
			{
				action.accept(server.getPlayerList().getPlayer(mc.player.getUUID()));
			}
			catch(RuntimeException | LinkageError e)
			{
				check("server step ran", false, e);
				Log.error("[scene] server step failed", e);
			}
		});
	}

	static void check(String name, boolean ok, Object detail)
	{
		if(!ok)
			failures++;
		Log.info("[scene] CHECK " + (ok ? "OK   " : "FAIL ") + name + " (" + detail + ")");
	}

	static void shot(Minecraft mc, String name)
	{
		Screenshot.grab(mc.gameDirectory, "scene_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> Log.info("[scene] " + c.getString()));
	}

	// ------------------------------------------------------------------------------------------------------ world

	private static void setup(ServerPlayer player)
	{
		MinecraftServer server = player.level().getServer();
		server.overworld().getGameRules().set(GameRules.SPAWN_MOBS, false, server);
		server.overworld().getGameRules().set(GameRules.ADVANCE_TIME, false, server);
		server.overworld().getGameRules().set(GameRules.ADVANCE_WEATHER, false, server);
		player.level().getServer().getCommands().performPrefixedCommand(player.level().getServer().createCommandSourceStack().withSuppressedOutput(), "time set 6000");
		player.level().getServer().getCommands().performPrefixedCommand(player.level().getServer().createCommandSourceStack().withSuppressedOutput(), "weather clear");
		player.setPermanentlyInvulnerable(true);
		BlockPos spawn = player.level().getRespawnData().pos();
		int y = player.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
		player.teleportTo(player.level(), spawn.getX() + 0.5, y, spawn.getZ() + 0.5, java.util.Set.of(), 30.0F, 5.0F, true);
	}

	/** Gives players all ten slot types (two ring and two curio slots) and makes the scene's items valid in them */
	static void writeSlotsDataPack(Path pack) throws IOException
	{
		accessories.writeSlotsDataPack(pack, itemTypesBySlot(), 2);
	}

	/**
	 * The scene's item types by slot type, for the data pack. 26.x: the data pack is written at the title screen, where
	 * an ItemStack cannot be made yet ("Components not bound yet" until a world's registries are loaded).
	 */
	static Map<String, List<net.minecraft.world.item.Item>> itemTypesBySlot()
	{
		Map<String, List<net.minecraft.world.item.Item>> items = new LinkedHashMap<>();
		items.put("curio", List.of(Items.CLOCK, Items.AMETHYST_SHARD));
		items.put("head", List.of(Items.TURTLE_HELMET));
		items.put("necklace", List.of(Items.HEART_OF_THE_SEA));
		items.put("back", List.of(Items.ELYTRA));
		items.put("body", List.of(Items.ECHO_SHARD));
		items.put("bracelet", List.of(Items.IRON_CHAIN));
		items.put("hands", List.of(Items.SHEARS));
		items.put("ring", List.of(Items.GOLD_NUGGET, Items.EMERALD));
		items.put("belt", List.of(Items.LEAD));
		items.put("charm", List.of(Items.TOTEM_OF_UNDYING));
		return items;
	}

	/** The scene's Curios by slot type, in the Curios slot order */
	static Map<String, List<ItemStack>> itemsBySlot()
	{
		ItemStack elytra = new ItemStack(Items.ELYTRA);
		elytra.setDamageValue(200);
		ItemStack shears = new ItemStack(Items.SHEARS);
		shears.setDamageValue(180);
		Map<String, List<ItemStack>> items = new LinkedHashMap<>();
		items.put("curio", List.of(new ItemStack(Items.CLOCK), new ItemStack(Items.AMETHYST_SHARD, 16)));
		items.put("head", List.of(new ItemStack(Items.TURTLE_HELMET)));
		items.put("necklace", List.of(new ItemStack(Items.HEART_OF_THE_SEA)));
		items.put("back", List.of(elytra));
		items.put("body", List.of(new ItemStack(Items.ECHO_SHARD)));
		items.put("bracelet", List.of(new ItemStack(Items.IRON_CHAIN)));
		items.put("hands", List.of(shears));
		items.put("ring", List.of(new ItemStack(Items.GOLD_NUGGET), new ItemStack(Items.EMERALD)));
		items.put("belt", List.of(new ItemStack(Items.LEAD)));
		items.put("charm", List.of(new ItemStack(Items.TOTEM_OF_UNDYING)));
		return items;
	}

	static Map<String, List<ItemStack>> allItems()
	{
		return itemsBySlot();
	}

	static Map<String, List<ItemStack>> twoRings()
	{
		return Map.of("ring", itemsBySlot().get("ring"));
	}

	/** The first n Curios in the slot order */
	static Map<String, List<ItemStack>> firstOf(Map<String, List<ItemStack>> items, int n)
	{
		Map<String, List<ItemStack>> result = new LinkedHashMap<>();
		int left = n;
		for(Map.Entry<String, List<ItemStack>> entry : items.entrySet())
		{
			if(left <= 0)
				break;
			List<ItemStack> stacks = entry.getValue().subList(0, Math.min(left, entry.getValue().size()));
			result.put(entry.getKey(), stacks);
			left -= stacks.size();
		}
		return result;
	}

	/** Empties every accessory slot of the player, then equips the given accessories (by scene slot name) */
	static void equip(ServerPlayer player, Map<String, List<ItemStack>> items)
	{
		for(SceneAccessories.Slot slot : accessories.slots(player))
			for(int i = 0; i < slot.size(); i++)
				slot.set(i, ItemStack.EMPTY);
		items.forEach((name, stacks) -> {
			SceneAccessories.Slot slot = accessories.slot(player, name);
			for(int i = 0; i < stacks.size(); i++)
				slot.set(i, stacks.get(i).copy());
		});
	}

	/** The live stack in a slot (scene slot name) */
	static ItemStack stack(net.minecraft.world.entity.LivingEntity entity, String name, int index)
	{
		return accessories.slot(entity, name).get(index);
	}

	static ItemStack clientStack(Minecraft mc, String name, int index)
	{
		return stack(mc.player, name, index);
	}

	static void checkSlots(Minecraft mc)
	{
		server(mc, p -> Log.info("[scene] server: packs " + p.level().getServer().getPackRepository().getSelectedIds() + ", " + accessories.describe(p)));
		Map<String, Integer> sizes = new LinkedHashMap<>();
		for(SceneAccessories.Slot slot : accessories.slots(mc.player))
			sizes.put(slot.id(), slot.size());
		check("data pack: the client has the 10 slot types, 2 ring and 2 curio slots", sizes.size() == 10 && sizes.getOrDefault(accessories.id("ring"), 0) == 2 && sizes.getOrDefault(accessories.id("curio"), 0) == 2, sizes);
	}

	/** The HUD's names ("id#index") of scene slots given as "name#index" */
	static List<String> expected(String... slots)
	{
		return java.util.Arrays.stream(slots).map(slot -> accessories.id(slot.substring(0, slot.indexOf('#'))) + slot.substring(slot.indexOf('#'))).toList();
	}

	static boolean isConfigScreen(Screen screen)
	{
		return screen != null && screen.getClass().getSimpleName().equals("GuiConfigSHB");
	}

	// ------------------------------------------------------------------------------------------------------ checks

	static List<String> slots(List<Bauble> baubles)
	{
		return baubles.stream().map(b -> b.slotIdentifier() + "#" + b.slotIndex()).toList();
	}

	static String positions(List<int[]> list)
	{
		return list.stream().map(p -> p[0] + "," + p[1]).toList().toString();
	}

	private static void checkAlignment(Minecraft mc, Alignment alignment, boolean vertical)
	{
		HudElementBaubles hud = HudElementBaubles.getActive();
		int gw = mc.getWindow().getGuiScaledWidth(), gh = mc.getWindow().getGuiScaledHeight();
		int x = hud.getLastX(), y = hud.getLastY(), w = hud.getWidth(), h = hud.getHeight();
		boolean onScreen = x >= 0 && y >= 0 && x + w <= gw && y + h <= gh;
		boolean edge = switch(alignment.getHorizontal()) {
			case LEFT -> x == 0;
			case RIGHT -> x + w == gw;
			case CENTER -> Math.abs(x + w / 2 - gw / 2) <= 1;
		};
		// The original keeps the centre column clear of the crosshair and the hotbar
		boolean clear = switch(alignment) {
			case CENTER -> y == gh / 2 + 26;
			case BOTTOM_CENTER -> y + h == gh - 65;
			default -> true;
		};
		String name = "alignment " + alignment + (vertical ? " vertical" : "");
		check(name + ": on screen, at its edge" + (clear ? "" : ", clear of crosshair/hotbar"), onScreen && edge && clear && (vertical ? w == 22 && h == 112 : w == 112 && h == 22),
				x + "," + y + " " + w + "x" + h + " on " + gw + "x" + gh);
		checkDrawn(mc, name + ": drawn", x, y, w, h, true);
	}

	private static void captureBaseline(Minecraft mc)
	{
		// The frame arrives once the GPU has copied it (before the next scene step)
		Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image ->
		{
			try(image)
			{
				baselineWidth = image.getWidth();
				baseline = image.getPixels();
			}
		});
	}

	/**
	 * Compares the rectangle (GUI coordinates) of the current frame with the baseline frame drawn without the HUD
	 * element: with {@code drawn}, most pixels on its outline must differ; without, almost none
	 */
	static void checkDrawn(Minecraft mc, String name, int gx, int gy, int gw, int gh, boolean drawn)
	{
		double scale = mc.getWindow().getGuiScale();
		Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image ->
		{
			int changed = 0, total = 0;
			try(image)
			{
				if(baseline == null || image.getWidth() != baselineWidth)
				{
					check(name, false, "no baseline frame");
					return;
				}
				int[] now = image.getPixels();
				for(int ix = gx; ix < gx + gw; ix++)
					for(int iy = gy; iy < gy + gh; iy++)
					{
						boolean outline = ix == gx || iy == gy || ix == gx + gw - 1 || iy == gy + gh - 1;
						if(!outline && gw > 4 && gh > 4)
							continue;
						int px = (int)((ix + 0.5) * scale), py = (int)((iy + 0.5) * scale);
						if(px < 0 || py < 0 || px >= image.getWidth() || py >= image.getHeight())
							continue;
						total++;
						if(differs(now[py * image.getWidth() + px], baseline[py * baselineWidth + px]))
							changed++;
					}
			}
			double share = total == 0 ? 0 : (double)changed / total;
			// Not drawn: one changed pixel is allowed (shaders animate the world between the baseline and this frame; a frame
			// or skin would change all of a small area)
			check(name, drawn ? share >= 0.7 : share <= 0.1 || changed <= 1, changed + " of " + total + " outline pixels changed");
		});
	}

	private static boolean differs(int a, int b)
	{
		int d = 0;
		for(int shift = 0; shift < 24; shift += 8)
			d += Math.abs((a >> shift & 0xFF) - (b >> shift & 0xFF));
		return d > 24;
	}

	// ------------------------------------------------------------------------------------------------------ GUI

	static void openConfig(Minecraft mc, Screen parent)
	{
		mc.gui.setScreen(accessories.configScreen(mc, parent));
	}

	static AbstractWidget previewButton(Screen screen)
	{
		String label = Language.getInstance().getOrDefault(ModSpartanHUDBaubles.ID + ".configuration.preview_hud");
		return widget(screen, label);
	}

	/** The cycle button showing the enum value */
	static AbstractWidget valueButton(Screen screen, SkinType value)
	{
		return widget(screen, value.getTranslatedName().getString());
	}

	static AbstractWidget widget(Screen screen, String label)
	{
		if(screen == null)
			return null;
		List<AbstractWidget> found = new ArrayList<>();
		collectWidgets(screen.children(), found);
		return found.stream().filter(w -> w.getMessage().getString().contains(label)).findFirst().orElse(null);
	}

	private static void collectWidgets(List<? extends GuiEventListener> children, List<AbstractWidget> found)
	{
		for(GuiEventListener child : children)
		{
			if(child instanceof AbstractWidget widget)
				found.add(widget);
			if(child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container)
				collectWidgets(container.children(), found);
		}
	}

	/** A real click: mouse down and up in the middle of the widget */
	static void click(Minecraft mc, AbstractWidget widget)
	{
		if(widget == null)
		{
			check("widget to click found", false, mc.gui.screen());
			return;
		}
		double x = widget.getX() + widget.getWidth() / 2.0, y = widget.getY() + widget.getHeight() / 2.0;
		mc.gui.screen().mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
		if(mc.gui.screen() != null)
			mc.gui.screen().mouseReleased(new MouseButtonEvent(x, y, new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)));
	}

	private static String readConfigFile()
	{
		try
		{
			return Files.readString(accessories.configDir().resolve(ModSpartanHUDBaubles.ID + "-client.toml"));
		}
		catch(IOException e)
		{
			return "";
		}
	}

	/** Every string of the mod exists in the current language (en_us has them all) */
	private static void checkTranslations()
	{
		List<String> keys = new ArrayList<>();
		for(String key : List.of("preview_hud", "preview_hud.hint", "hover_text_example", "disable_hud", "skin_type", "alignment", "offset_x", "offset_y", "rotate_vertical",
				"show_baubles_icon", "show_empty_slots", "max_slots_per_line", "tooltip_background_colour", "tooltip_frame_top_colour",
				"tooltip_frame_bottom_colour", "minimal_top_colour", "minimal_bottom_colour", "red", "green", "blue", "alpha", "title"))
			keys.add(ModSpartanHUDBaubles.ID + ".configuration." + key);
		for(SkinType skin : SkinType.values())
			keys.add(((net.minecraft.network.chat.contents.TranslatableContents)skin.getTranslatedName().getContents()).getKey());
		for(Alignment alignment : Alignment.values())
			keys.add(((net.minecraft.network.chat.contents.TranslatableContents)alignment.getTranslatedName().getContents()).getKey());
		List<String> missing = keys.stream().filter(k -> !Language.getInstance().has(k)).toList();
		check("translations: " + keys.size() + " strings present in " + LANG, missing.isEmpty(), missing);
	}

	static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath(ModSpartanHUDBaubles.ID, path);
	}

	/** 26.2: the HUD is hidden through Minecraft.gui.hud, which only toggles */
	private static void setHudHidden(net.minecraft.client.Minecraft mc, boolean hidden)
	{
		if(mc.gui.hud.isHidden() != hidden)
			mc.gui.hud.toggle();
	}
}
