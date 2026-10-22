package com.oblivioussp.spartanhudbaubles.fabric;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.oblivioussp.spartanhudbaubles.client.event.RenderBaubleOnHUDCallback;
import com.oblivioussp.spartanhudbaubles.client.gui.AlignmentHelper.Alignment;
import com.oblivioussp.spartanhudbaubles.client.gui.HudElementBaubles.Bauble;
import com.oblivioussp.spartanhudbaubles.client.gui.SlotIcon;
import com.oblivioussp.spartanhudbaubles.platform.Platform;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Fabric with Trinkets */
public class FabricPlatform implements Platform
{
	// The Trinkets ring slot icon stands for Trinkets next to the HUD, as the Curios button does on NeoForge
	private static final ResourceLocation TRINKETS_ICON = ResourceLocation.fromNamespaceAndPath("trinkets", "textures/gui/slots/ring.png");
	private static final SlotIcon DUMMY_SLOT_ICON = SlotIcon.texture(ResourceLocation.fromNamespaceAndPath("trinkets", "textures/gui/slots/empty.png"));

	@Override
	public String name()
	{
		return "fabric";
	}

	/** In the order of the Trinkets inventory: groups by their order, then their slots by theirs (both then by name) */
	@Override
	public List<Bauble> collectBaubles(Player player, boolean showEmpty)
	{
		List<Bauble> baubles = new ArrayList<Bauble>();
		TrinketsApi.getTrinketComponent(player).ifPresent(component ->
		{
			Map<String, SlotGroup> groups = component.getGroups();
			Map<String, Map<String, TrinketInventory>> inventories = component.getInventory();
			List<SlotGroup> sortedGroups = new ArrayList<>(groups.values());
			sortedGroups.sort(Comparator.comparingInt(SlotGroup::getOrder).thenComparing(SlotGroup::getName));
			for(SlotGroup group : sortedGroups)
			{
				Map<String, TrinketInventory> groupInventories = inventories.get(group.getName());
				if(groupInventories == null)
					continue;
				List<SlotType> types = new ArrayList<>(group.getSlots().values());
				types.sort(Comparator.comparingInt(SlotType::getOrder).thenComparing(SlotType::getName));
				for(SlotType type : types)
				{
					TrinketInventory inventory = groupInventories.get(type.getName());
					if(inventory == null)
						continue;
					for(int i = 0; i < inventory.getContainerSize(); i++)
					{
						ItemStack stack = inventory.getItem(i);
						if(!stack.isEmpty())
							baubles.add(new Bauble(type.getId(), i, stack, null));
						else if(showEmpty)
							baubles.add(new Bauble(type.getId(), i, ItemStack.EMPTY, SlotIcon.texture(type.getIcon())));
					}
				}
			}
		});
		return baubles;
	}

	@Override
	public void drawModIcon(GuiGraphics graphics, int x, int y)
	{
		graphics.blit(TRINKETS_ICON, x, y, 10, 10, 0.0F, 0.0F, 16, 16, 16, 16);
	}

	@Override
	public SlotIcon dummySlotIcon()
	{
		return DUMMY_SLOT_ICON;
	}

	@Override
	public void onRenderBauble(boolean post, ItemStack stack, String slotIdentifier, int slotIndex, int x, int y, Alignment alignment, boolean vertical, GuiGraphics graphics, Font font)
	{
		(post ? RenderBaubleOnHUDCallback.POST : RenderBaubleOnHUDCallback.PRE).invoker().onRender(stack, slotIdentifier, slotIndex, x, y, alignment, vertical, graphics, font);
	}
}
