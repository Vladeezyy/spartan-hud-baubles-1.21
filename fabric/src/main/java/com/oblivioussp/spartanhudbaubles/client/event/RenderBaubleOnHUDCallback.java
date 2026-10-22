package com.oblivioussp.spartanhudbaubles.client.event;

import com.oblivioussp.spartanhudbaubles.client.gui.AlignmentHelper.Alignment;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric's RenderBaubleOnHUDEvent (the NeoForge version's event): fires when rendering a specific bauble (an equipped
 * Trinket) to allow rendering extra things under ({@link #PRE}) or over ({@link #POST}) it, on the client only. The
 * slot identifier is the Trinkets slot ({@code group/slot}, e.g. {@code hand/ring}); {@link GuiGraphics#guiWidth()} and
 * {@link GuiGraphics#guiHeight()} are the scaled resolution.
 */
@FunctionalInterface
public interface RenderBaubleOnHUDCallback
{
	Event<RenderBaubleOnHUDCallback> PRE = EventFactory.createArrayBacked(RenderBaubleOnHUDCallback.class, listeners -> (stack, slotIdentifier, slotIndex, x, y, alignment, vertical, graphics, font) ->
	{
		for(RenderBaubleOnHUDCallback listener : listeners)
			listener.onRender(stack, slotIdentifier, slotIndex, x, y, alignment, vertical, graphics, font);
	});

	Event<RenderBaubleOnHUDCallback> POST = EventFactory.createArrayBacked(RenderBaubleOnHUDCallback.class, listeners -> (stack, slotIdentifier, slotIndex, x, y, alignment, vertical, graphics, font) ->
	{
		for(RenderBaubleOnHUDCallback listener : listeners)
			listener.onRender(stack, slotIdentifier, slotIndex, x, y, alignment, vertical, graphics, font);
	});

	void onRender(ItemStack stack, String slotIdentifier, int slotIndex, int x, int y, Alignment alignment, boolean vertical, GuiGraphics graphics, Font font);
}
