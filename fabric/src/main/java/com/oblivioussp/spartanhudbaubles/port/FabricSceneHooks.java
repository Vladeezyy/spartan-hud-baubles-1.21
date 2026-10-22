package com.oblivioussp.spartanhudbaubles.port;

import com.oblivioussp.spartanhudbaubles.client.event.RenderBaubleOnHUDCallback;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

/** The Fabric events of the test scenes (dev runs only: port/ is not in the jar; the client entry point loads this by name). */
public final class FabricSceneHooks
{
	public static void init()
	{
		DevScene.accessories = new TrinketsSceneAccessories();
		ClientTickEvents.END_CLIENT_TICK.register(mc ->
		{
			DevScene.onTick();
			StressScene.onTick();
			NetScene.onTick();
		});
		ScreenEvents.AFTER_INIT.register((mc, screen, width, height) -> ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, delta) -> DevScene.onScreenRendered(s)));
		// After the whole HUD (NeoForge: RenderGuiEvent.Pre of the next frame and RenderGuiEvent.Post)
		HudRenderCallback.EVENT.register((graphics, delta) ->
		{
			DevScene.onFrame();
			StressScene.onFrame();
		});
		RenderBaubleOnHUDCallback.PRE.register((stack, slot, index, x, y, alignment, vertical, graphics, font) -> DevScene.onBaublePre(x, y));
		RenderBaubleOnHUDCallback.POST.register((stack, slot, index, x, y, alignment, vertical, graphics, font) -> DevScene.onBaublePost());
	}
}
