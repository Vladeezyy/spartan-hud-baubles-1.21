package com.oblivioussp.spartanhudbaubles.port;

import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;
import com.oblivioussp.spartanhudbaubles.client.gui.HudElementBaubles;
import com.oblivioussp.spartanhudbaubles.util.Log;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Multiplayer check ({@code tools/nettest.sh}), inert unless {@code spartanhudbaubles.scene} is {@code net}: the dev
 * client joins a dev dedicated server whose world has the scene's slot data pack, equips Curios with the
 * {@code /curios} command and checks that the HUD element shows what the server synced: the items, a changed
 * durability, a removed Curio. The mod itself only runs on the client.
 */
@EventBusSubscriber(modid = ModSpartanHUDBaubles.ID, value = Dist.CLIENT)
public final class NetScene
{
	private static final boolean ENABLED = DevScene.MODE.equals("net");
	private static int tick;

	@SubscribeEvent
	public static void onTick(ClientTickEvent.Post event)
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
		if(mc.player == null || mc.getConnection() == null)
			return;
		mc.options.pauseOnLostFocus = false;
		mc.getToasts().clear();
		tick++;
		try
		{
			step(mc);
		}
		catch(RuntimeException e)
		{
			DevScene.check("net step at tick " + tick + " ran", false, e);
			Log.error("[scene] net step failed", e);
		}
	}

	private static void command(Minecraft mc, String command)
	{
		mc.player.connection.sendCommand(command);
	}

	private static void step(Minecraft mc)
	{
		switch(tick)
		{
			case 40 -> {
				DevScene.check("net: joined a dedicated server", !mc.isLocalServer() && mc.getSingleplayerServer() == null, mc.getCurrentServer() == null ? "?" : mc.getCurrentServer().ip);
				command(mc, "gamemode survival");
				command(mc, "time set noon");
				command(mc, "gamerule doDaylightCycle false");
				command(mc, "effect give @s minecraft:resistance infinite 4 true");
			}
			case 60 -> {
				int slots = CuriosApi.getCuriosInventory(mc.player).map(i -> i.getCurios().size()).orElse(0);
				DevScene.check("net: the slot data pack reached the client (10 slot types)", slots == 10, slots);
				command(mc, "curios replace ring 0 Dev with minecraft:gold_nugget");
				command(mc, "curios replace ring 1 Dev with minecraft:emerald");
				command(mc, "curios replace necklace 0 Dev with minecraft:heart_of_the_sea");
				command(mc, "curios replace back 0 Dev with minecraft:elytra[minecraft:damage=200]");
				command(mc, "curios replace curio 1 Dev with minecraft:amethyst_shard 16");
			}
			case 90 -> {
				HudElementBaubles hud = HudElementBaubles.getActive();
				var shown = hud == null ? java.util.List.<String>of() : DevScene.slots(hud.getLastRendered());
				DevScene.check("net: HUD shows the Curios equipped on the server", shown.equals(java.util.List.of("curio#1", "necklace#0", "back#0", "ring#0", "ring#1")) && DevScene.framePre.size() == 5, shown);
				DevScene.check("net: synced stack count and damage", DevScene.clientStack(mc, "curio", 1).getCount() == 16 && DevScene.clientStack(mc, "back", 0).getDamageValue() == 200,
						DevScene.clientStack(mc, "curio", 1) + ", " + DevScene.clientStack(mc, "back", 0).getDamageValue());
				DevScene.shot(mc, "net");
				command(mc, "curios replace back 0 Dev with minecraft:elytra[minecraft:damage=400]");
				command(mc, "curios replace ring 1 Dev with minecraft:air");
			}
			case 120 -> {
				HudElementBaubles hud = HudElementBaubles.getActive();
				DevScene.check("net: new durability shown without opening the inventory", DevScene.clientStack(mc, "back", 0).getDamageValue() == 400, DevScene.clientStack(mc, "back", 0).getDamageValue());
				DevScene.check("net: removed ring leaves the HUD", !DevScene.slots(hud.getLastRendered()).contains("ring#1") && DevScene.framePre.size() == 4, DevScene.slots(hud.getLastRendered()));
			}
			case 130 -> {
				Log.info("[scene] DONE, failed checks: " + DevScene.failures);
				mc.level.disconnect();
				mc.disconnect(new TitleScreen());
				mc.stop();
			}
			default -> {}
		}
	}
}
