package com.oblivioussp.spartanhudbaubles.neoforge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.oblivioussp.spartanhudbaubles.client.event.RenderBaubleOnHUDEvent;
import com.oblivioussp.spartanhudbaubles.client.gui.AlignmentHelper.Alignment;
import com.oblivioussp.spartanhudbaubles.client.gui.HudElementBaubles.Bauble;
import com.oblivioussp.spartanhudbaubles.client.gui.SlotIcon;
import com.oblivioussp.spartanhudbaubles.platform.Platform;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/** NeoForge with Curios */
public class NeoForgePlatform implements Platform
{
	// Port: the Curios inventory button replaces the Baubles icon, and the generic Curio slot icon the empty Bauble
	// slot of the preview (both were drawn from Baubles' expanded_inventory.png)
	private static final ResourceLocation CURIOS_ICON = ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "textures/gui/curios/button.png");
	private static final SlotIcon DUMMY_SLOT_ICON = SlotIcon.sprite(ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "slot/empty_curio_slot"));

	@Override
	public String name()
	{
		return "neoforge";
	}

	/** In the order of the Curios slot types (as in the Curios inventory); hidden slot types and disabled slots left out */
	@Override
	public List<Bauble> collectBaubles(Player player, boolean showEmpty)
	{
		List<Bauble> baubles = new ArrayList<Bauble>();
		CuriosApi.getCuriosInventory(player).ifPresent(inventory ->
		{
			List<ICurioStacksHandler> handlers = new ArrayList<ICurioStacksHandler>(inventory.getCurios().values());
			handlers.sort(Comparator.comparingInt((ICurioStacksHandler handler) -> slotType(player, handler.getIdentifier()).map(ISlotType::getOrder).orElse(Integer.MAX_VALUE))
					.thenComparing(ICurioStacksHandler::getIdentifier));
			for(ICurioStacksHandler handler : handlers)
			{
				if(!handler.isVisible())
					continue;
				IDynamicStackHandler stacks = handler.getStacks();
				NonNullList<Boolean> activeStates = handler.getActiveStates();
				for(int i = 0; i < stacks.getSlots(); i++)
				{
					if(i < activeStates.size() && !activeStates.get(i))
						continue;
					ItemStack checkStack = stacks.getStackInSlot(i);
					if(!checkStack.isEmpty())
						baubles.add(new Bauble(handler.getIdentifier(), i, checkStack, null));
					else if(showEmpty)
						baubles.add(new Bauble(handler.getIdentifier(), i, ItemStack.EMPTY, slotType(player, handler.getIdentifier()).map(type -> SlotIcon.sprite(type.getIcon())).orElse(DUMMY_SLOT_ICON)));
				}
			}
		});
		return baubles;
	}

	private static Optional<ISlotType> slotType(Player player, String identifier)
	{
		return CuriosApi.getSlot(identifier, player.level());
	}

	@Override
	public void drawModIcon(GuiGraphics graphics, int x, int y)
	{
		graphics.blit(CURIOS_ICON, x, y, 0, 0, 10, 10, 10, 10);
	}

	@Override
	public SlotIcon dummySlotIcon()
	{
		return DUMMY_SLOT_ICON;
	}

	@Override
	public void onRenderBauble(boolean post, ItemStack stack, String slotIdentifier, int slotIndex, int x, int y, Alignment alignment, boolean vertical, GuiGraphics graphics, Font font)
	{
		NeoForge.EVENT_BUS.post(post ? new RenderBaubleOnHUDEvent.Post(stack, slotIdentifier, slotIndex, x, y, alignment, vertical, graphics, font)
				: new RenderBaubleOnHUDEvent.Pre(stack, slotIdentifier, slotIndex, x, y, alignment, vertical, graphics, font));
	}
}
