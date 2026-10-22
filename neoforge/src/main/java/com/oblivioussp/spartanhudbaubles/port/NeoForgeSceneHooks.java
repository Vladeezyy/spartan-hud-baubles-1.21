package com.oblivioussp.spartanhudbaubles.port;

import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;
import com.oblivioussp.spartanhudbaubles.client.event.RenderBaubleOnHUDEvent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** The NeoForge events of the test scenes (dev runs only: port/ is not in the jar). */
@EventBusSubscriber(modid = ModSpartanHUDBaubles.ID, value = Dist.CLIENT)
public final class NeoForgeSceneHooks
{
	static
	{
		DevScene.accessories = new CuriosSceneAccessories();
	}

	@SubscribeEvent
	public static void onTick(ClientTickEvent.Post event)
	{
		DevScene.onTick();
		StressScene.onTick();
		NetScene.onTick();
	}

	@SubscribeEvent
	public static void onScreenRendered(ScreenEvent.Render.Post event)
	{
		DevScene.onScreenRendered(event.getScreen());
	}

	@SubscribeEvent
	public static void onRenderGuiPre(RenderGuiEvent.Pre event)
	{
		DevScene.onFrame();
	}

	@SubscribeEvent
	public static void onRenderGuiPost(RenderGuiEvent.Post event)
	{
		StressScene.onFrame();
	}

	@SubscribeEvent
	public static void onBaublePre(RenderBaubleOnHUDEvent.Pre event)
	{
		DevScene.onBaublePre(event.getPositionX(), event.getPositionY());
	}

	@SubscribeEvent
	public static void onBaublePost(RenderBaubleOnHUDEvent.Post event)
	{
		DevScene.onBaublePost();
	}
}
