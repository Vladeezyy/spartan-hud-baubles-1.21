package com.oblivioussp.spartanhudbaubles.port;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.mojang.datafixers.util.Pair;
import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;
import com.oblivioussp.spartanhudbaubles.client.gui.AlignmentHelper.Alignment;
import com.oblivioussp.spartanhudbaubles.client.gui.HudElementBaubles;
import com.oblivioussp.spartanhudbaubles.util.Config;
import com.oblivioussp.spartanhudbaubles.util.Log;
import com.oblivioussp.spartanhudbaubles.util.SkinType;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Stress scene ({@code tools/scenetest.sh -Pscene=stress}), inert unless {@code spartanhudbaubles.scene} is
 * {@code stress}: a broken config file from before the start (tools/scenetest.sh writes it), 74 Curio slots, frame
 * rate with and without the HUD element, Curios changing every tick, every GUI scale and a small window with every
 * alignment, extreme config values, leaving and rejoining the world.
 */
public final class StressScene
{
	private static final boolean ENABLED = DevScene.MODE.equals("stress");
	private static final String WORLD = "shb_stress";
	private static final int CURIO_SLOTS = 64;
	private static final List<Pair<Integer, Consumer<Minecraft>>> STEPS = new ArrayList<>();
	private static final Random RANDOM = new Random(20261013L);
	private static int phase;
	private static boolean opening;
	private static int tick;
	private static int frames;
	private static int churnTicks;
	private static double fpsWithout, fpsWith;
	private static int equippedBeforeLeaving;

	static
	{
		int t = 20;
		at(t, mc -> {
			// tools/scenetest.sh wrote skin_type = "FOO", offset_x = 99999 and alignment = 5 before the start
			check("broken config file: unknown values back to the defaults, offset clamped to 1000, no crash", Config.SKIN_TYPE.get() == SkinType.HOTBAR_STYLE && Config.OFFSET_X.get() == 1000 && Config.ALIGNMENT.get() == Alignment.BOTTOM_RIGHT,
					Config.SKIN_TYPE.get() + ", " + Config.OFFSET_X.get() + ", " + Config.ALIGNMENT.get());
			Config.OFFSET_X.set(0);
			DevScene.server(mc, StressScene::setup);
		});
		at(t += 20, mc -> {
			int slots = DevScene.accessories.slots(mc.player).stream().mapToInt(SceneAccessories.Slot::size).sum();
			check("74 Curio slots on the client", slots == CURIO_SLOTS + 10, slots);
			DevScene.server(mc, p -> fill(p, 1.0));
		});
		// Frame rate: 100 ticks without the HUD element, 100 with 74 Curios
		at(t += 20, mc -> {
			Config.DISABLE_HUD.set(true);
			frames = 0;
		});
		at(t += 100, mc -> {
			fpsWithout = frames / 5.0;
			Config.DISABLE_HUD.set(false);
			frames = 0;
		});
		at(t += 100, mc -> {
			fpsWith = frames / 5.0;
			HudElementBaubles hud = HudElementBaubles.getActive();
			Log.info("[scene] PERF frames per second: " + fpsWithout + " without the HUD element, " + fpsWith + " with " + hud.getLastRendered().size() + " Curios");
			check("74 Curios drawn", hud.getLastRendered().size() == CURIO_SLOTS + 10 && DevScene.framePre.size() == CURIO_SLOTS + 10, hud.getLastRendered().size());
			check("frame rate with 74 Curios at least 80% of the one without", fpsWith >= fpsWithout * 0.8, fpsWith + " vs " + fpsWithout);
			DevScene.shot(mc, "stress_75");
		});
		// Every GUI scale and a small window: with 74 Curios and every alignment the element stays on the screen
		for(int scale = 1; scale <= 4; scale++)
		{
			int guiScale = scale;
			at(t += 5, mc -> {
				mc.options.guiScale().set(guiScale);
				mc.resizeDisplay();
			});
			t = alignments(t, "GUI scale " + scale);
		}
		at(t += 5, mc -> {
			mc.options.guiScale().set(0);
			GLFW.glfwSetWindowSize(mc.getWindow().handle(), 854, 480);
		});
		t = alignments(t + 10, "small window 854x480, auto GUI scale");
		at(t += 5, mc -> DevScene.shot(mc, "stress_small"));
		at(t += 5, mc -> {
			GLFW.glfwSetWindowSize(mc.getWindow().handle(), 1600, 900);
			mc.options.guiScale().set(3);
			mc.resizeDisplay();
			Config.ALIGNMENT.set(Alignment.BOTTOM_RIGHT);
			Config.ROTATE_VERTICAL.set(false);
		});
		// Curios change every tick (equipped, removed, shrunk, damaged)
		at(t += 10, mc -> churnTicks = 200);
		at(t += 210, mc -> {
			int onClient = countEquipped(mc.player);
			int shown = HudElementBaubles.getActive().getLastRendered().size();
			check("after 200 ticks of changes the HUD matches the client inventory", onClient == shown && DevScene.framePre.size() == shown, shown + " shown, " + onClient + " equipped");
		});
		// Extreme config values
		at(t += 5, mc -> {
			Config.OFFSET_X.set(-1000);
			Config.OFFSET_Y.set(1000);
		});
		at(t += 5, mc -> {
			check("offsets -1000 / 1000: off the screen, still rendered without errors", DevScene.framePre.size() == HudElementBaubles.getActive().getLastRendered().size(), DevScene.framePre.size());
			Config.OFFSET_X.set(0);
			Config.OFFSET_Y.set(0);
			Config.MAX_SLOTS_PER_LINE.set(1);
			Config.SHOW_EMPTY_SLOTS.set(true);
			Config.SKIN_TYPE.set(SkinType.TOOLTIP_STYLE);
		});
		at(t += 5, mc -> {
			HudElementBaubles hud = HudElementBaubles.getActive();
			check("1 slot per line, 74 lines with the empty slots", hud.getWidth() == 22 && hud.getHeight() == 22 + 18 * (CURIO_SLOTS + 10 - 1), hud.getWidth() + "x" + hud.getHeight());
			Config.MAX_SLOTS_PER_LINE.set(64);
			Config.SHOW_EMPTY_SLOTS.set(false);
			Config.SKIN_TYPE.set(SkinType.HOTBAR_STYLE);
		});
		// Leave and rejoin: the Curios saved with the world are shown again
		at(t += 5, mc -> {
			// Curios checks its slot validators (the curios:<slot> item tags) at login and moves invalid items out:
			// equip the scene's tagged items before leaving
			DevScene.server(mc, p -> {
				p.getInventory().clearContent();
				DevScene.equip(p, DevScene.allItems());
			});
		});
		at(t += 10, mc -> {
			equippedBeforeLeaving = HudElementBaubles.getActive().getLastRendered().size();
			phase = 1;
			tick = 0;
			mc.level.disconnect(Component.empty());
			mc.disconnect(new TitleScreen(), false);
		});
	}

	private static void at(int t, Consumer<Minecraft> action)
	{
		STEPS.add(Pair.of(t, action));
	}

	private static void check(String name, boolean ok, Object detail)
	{
		DevScene.check(name, ok, detail);
	}

	/** Every alignment, horizontal and vertical, at the current window and GUI scale: one check */
	private static int alignments(int t, String name)
	{
		List<String> bad = new ArrayList<>();
		for(Alignment alignment : Alignment.values())
			for(boolean vertical : new boolean[] { false, true })
			{
				at(t += 2, mc -> {
					Config.ALIGNMENT.set(alignment);
					Config.ROTATE_VERTICAL.set(vertical);
				});
				at(t += 2, mc -> {
					HudElementBaubles hud = HudElementBaubles.getActive();
					int gw = mc.getWindow().getGuiScaledWidth(), gh = mc.getWindow().getGuiScaledHeight();
					int x = hud.getLastX(), y = hud.getLastY();
					// The centre column keeps the original's gap to the crosshair / hotbar, so it may run off the bottom
					boolean centreColumn = alignment.getHorizontal() == com.oblivioussp.spartanhudbaubles.client.gui.AlignmentHelper.HorizontalAlignment.CENTER && alignment != Alignment.TOP_CENTER;
					boolean onScreen = x >= 0 && x + hud.getWidth() <= gw && (centreColumn || y >= 0 && y + hud.getHeight() <= gh);
					if(!onScreen || DevScene.framePre.size() != hud.getLastRendered().size())
						bad.add(alignment + (vertical ? " vertical" : "") + " " + x + "," + y + " " + hud.getWidth() + "x" + hud.getHeight() + " on " + gw + "x" + gh);
				});
			}
		at(t += 1, mc -> check(name + ": every alignment on the screen with 74 Curios", bad.isEmpty(), bad));
		return t;
	}

	private static void setup(ServerPlayer player)
	{
		var server = player.level().getServer();
		server.overworld().getGameRules().set(GameRules.SPAWN_MOBS, false, server);
		server.overworld().getGameRules().set(GameRules.ADVANCE_TIME, false, server);
		player.level().setDayTime(6000);
		player.setInvulnerable(true);
		player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), java.util.Set.of(), 0.0F, -60.0F, true);
	}

	/** Fills every accessory slot with a chance of {@code share} with different items */
	private static void fill(ServerPlayer player, double share)
	{
		List<Item> items = BuiltInRegistries.ITEM.stream().filter(i -> i != Items.AIR).toList();
		int n = 0;
		for(SceneAccessories.Slot slot : DevScene.accessories.slots(player))
			for(int i = 0; i < slot.size(); i++, n++)
				slot.set(i, RANDOM.nextDouble() < share ? new ItemStack(items.get((n * 37) % items.size())) : ItemStack.EMPTY);
	}

	/** One random change: equip, remove, shrink or damage */
	private static void churn(ServerPlayer player)
	{
		List<Item> items = BuiltInRegistries.ITEM.stream().filter(i -> i != Items.AIR).toList();
		List<SceneAccessories.Slot> slots = DevScene.accessories.slots(player);
		for(int k = 0; k < 5; k++)
		{
			SceneAccessories.Slot handler = slots.get(RANDOM.nextInt(slots.size()));
			int slot = RANDOM.nextInt(handler.size());
			ItemStack stack = handler.get(slot);
			switch(RANDOM.nextInt(4))
			{
				case 0 -> handler.set(slot, new ItemStack(items.get(RANDOM.nextInt(items.size())), 1 + RANDOM.nextInt(64)));
				case 1 -> handler.set(slot, ItemStack.EMPTY);
				case 2 -> stack.shrink(1);
				default -> {
					if(stack.isDamageableItem())
						stack.setDamageValue(RANDOM.nextInt(stack.getMaxDamage()));
				}
			}
		}
	}

	private static int countEquipped(net.minecraft.world.entity.LivingEntity entity)
	{
		int n = 0;
		for(SceneAccessories.Slot slot : DevScene.accessories.slots(entity))
			for(int i = 0; i < slot.size(); i++)
				if(!slot.get(i).isEmpty())
					n++;
		return n;
	}

	/** After a frame of the GUI (NeoForge RenderGuiEvent.Post, Fabric HudRenderCallback) */
	public static void onFrame()
	{
		if(ENABLED)
			frames++;
	}

	// The loader modules call this after every client tick (port/*SceneHooks)
	public static void onTick()
	{
		if(!ENABLED)
			return;
		Minecraft mc = Minecraft.getInstance();
		if(mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)
		{
			mc.options.onboardAccessibility = false;
			mc.options.save();
			mc.setScreen(new TitleScreen());
			return;
		}
		if(mc.getOverlay() != null)
			return;
		if(mc.player == null && mc.screen instanceof TitleScreen && !opening)
		{
			opening = true;
			if(phase == 0)
			{
				GLFW.glfwSetWindowSize(mc.getWindow().handle(), 1600, 900);
				mc.options.guiScale().set(3);
				mc.options.cloudStatus().set(CloudStatus.OFF);
				mc.options.enableVsync().set(false);
				mc.options.framerateLimit().set(260);
				mc.options.pauseOnLostFocus = false;
				mc.resizeDisplay();
				try
				{
					// 64 generic curio slots
					DevScene.accessories.writeSlotsDataPack(mc.getLevelSource().getBaseDir().resolve(WORLD).resolve("datapacks/shb_slots"), DevScene.itemsBySlot(), CURIO_SLOTS);
				}
				catch(IOException e)
				{
					check("test data pack written", false, e);
				}
				WorldDataConfiguration packs = new WorldDataConfiguration(new net.minecraft.world.level.DataPackConfig(List.of("vanilla", "mod_data", "file/shb_slots"), List.of()),
						net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
				LevelSettings settings = new LevelSettings(WORLD, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(FeatureFlags.DEFAULT_FLAGS), packs);
				mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261013L, false, false), WorldPresets::createNormalWorldDimensions, new TitleScreen());
			}
			else
				mc.createWorldOpenFlows().openWorld(WORLD, () -> mc.setScreen(new TitleScreen()));
			return;
		}
		if(mc.player == null || mc.getSingleplayerServer() == null)
			return;
		opening = false;
		mc.options.pauseOnLostFocus = false;
		mc.getToastManager().clear();
		mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
		if(mc.screen instanceof PauseScreen)
			mc.setScreen(null);
		tick++;
		if(phase == 1)
		{
			if(tick == 30)
				DevScene.server(mc, p -> Log.info("[scene] rejoined: server has " + countEquipped(p) + " Curios, " + p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(10)).size() + " items on the ground, " + p.getInventory().getNonEquipmentItems().stream().filter(st -> !st.isEmpty()).count() + " stacks in the inventory"));
			if(tick == 40)
			{
				int shown = HudElementBaubles.getActive().getLastRendered().size();
				check("rejoined the world: the same " + equippedBeforeLeaving + " Curios shown", shown == equippedBeforeLeaving && shown > 0, shown + ", client has " + countEquipped(mc.player));
				DevScene.shot(mc, "stress_rejoined");
			}
			if(tick == 45)
			{
				Log.info("[scene] DONE, failed checks: " + DevScene.failures);
				mc.stop();
			}
			return;
		}
		if(churnTicks > 0)
		{
			churnTicks--;
			DevScene.server(mc, StressScene::churn);
		}
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
}
