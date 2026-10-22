package com.oblivioussp.spartanhudbaubles.platform;

import java.util.List;
import java.util.ServiceLoader;

import com.oblivioussp.spartanhudbaubles.client.gui.AlignmentHelper.Alignment;
import com.oblivioussp.spartanhudbaubles.client.gui.HudElementBaubles.Bauble;
import com.oblivioussp.spartanhudbaubles.client.gui.SlotIcon;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What the common code needs from the mod loader and its accessory mod (NeoForge: Curios, Fabric: Trinkets), found
 * with the {@link ServiceLoader}.
 */
public interface Platform
{
	Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
			.orElseThrow(() -> new IllegalStateException("No Spartan HUD: Baubles platform implementation"));

	/** "neoforge" or "fabric". */
	String name();

	/**
	 * The accessories of the player in the slot order of the accessory mod (as in its inventory). Hidden slot types and
	 * disabled slots are left out; empty slots only with {@code showEmpty}.
	 */
	List<Bauble> collectBaubles(Player player, boolean showEmpty);

	/** The accessory mod's icon next to the HUD (10 x 10; the original drew the Baubles icon) */
	void drawModIcon(GuiGraphics graphics, int x, int y);

	/** The slots of the preview (and of slot types without an icon of their own) */
	SlotIcon dummySlotIcon();

	/** Before and after an accessory is drawn (NeoForge: RenderBaubleOnHUDEvent.Pre / Post, Fabric: RenderBaubleOnHUDCallback) */
	void onRenderBauble(boolean post, ItemStack stack, String slotIdentifier, int slotIndex, int x, int y, Alignment alignment, boolean vertical, GuiGraphics graphics, Font font);
}
