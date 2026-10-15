package com.oblivioussp.spartanhudbaubles.client.event;

import com.oblivioussp.spartanhudbaubles.client.gui.AlignmentHelper.Alignment;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * An event that fires when rendering a specific bauble (an equipped Curio) to allow rendering extra things under or over the specific bauble.<br>
 * This event is not {@link ICancellableEvent cancellable}<br>
 * Use the {@link Pre} or {@link Post} events to subscribe to the event bus<br>
 * This event is fired on the {@link NeoForge#EVENT_BUS}, on the client only<br>
 * Port: the GUI and resolution of 1.12 are replaced by {@link GuiGraphics} (its {@code guiWidth()} and {@code guiHeight()}
 * are the scaled resolution); the Curios slot type and index of the bauble are new.
 * @author ObliviousSpartan
 *
 */
public class RenderBaubleOnHUDEvent extends Event
{
	protected final ItemStack itemStack;
	protected final String slotIdentifier;
	protected final int slotIndex;
	protected final int positionX, positionY;
	protected final Alignment alignment;
	protected final boolean isVertical;
	protected final GuiGraphics guiGraphics;
	protected final Font font;

	private RenderBaubleOnHUDEvent(ItemStack stack, String slotId, int index, int posX, int posY, Alignment align, boolean vertical, GuiGraphics graphics, Font font)
	{
		itemStack = stack;
		slotIdentifier = slotId;
		slotIndex = index;
		positionX = posX;
		positionY = posY;
		alignment = align;
		isVertical = vertical;
		guiGraphics = graphics;
		this.font = font;
	}

	public ItemStack getItemStack()
	{
		return itemStack;
	}

	/**
	 * @return The Curios slot type the bauble is equipped in, e.g. {@code ring}
	 */
	public String getSlotIdentifier()
	{
		return slotIdentifier;
	}

	/**
	 * @return The index of the slot within its slot type
	 */
	public int getSlotIndex()
	{
		return slotIndex;
	}

	public int getPositionX()
	{
		return positionX;
	}

	public int getPositionY()
	{
		return positionY;
	}

	public Alignment getAlignment()
	{
		return alignment;
	}

	public boolean isVertical()
	{
		return isVertical;
	}

	public GuiGraphics getGuiGraphics()
	{
		return guiGraphics;
	}

	public Font getFont()
	{
		return font;
	}

	public static class Pre extends RenderBaubleOnHUDEvent
	{
		public Pre(ItemStack stack, String slotId, int index, int posX, int posY, Alignment align, boolean vertical, GuiGraphics graphics, Font font)
		{
			super(stack, slotId, index, posX, posY, align, vertical, graphics, font);
		}
	}

	public static class Post extends RenderBaubleOnHUDEvent
	{
		public Post(ItemStack stack, String slotId, int index, int posX, int posY, Alignment align, boolean vertical, GuiGraphics graphics, Font font)
		{
			super(stack, slotId, index, posX, posY, align, vertical, graphics, font);
		}
	}
}
