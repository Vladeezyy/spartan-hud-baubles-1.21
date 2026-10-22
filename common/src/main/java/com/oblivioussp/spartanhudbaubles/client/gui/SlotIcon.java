package com.oblivioussp.spartanhudbaubles.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * The icon of an empty accessory slot: a sprite of the block atlas (Curios slot icons) or a 16 x 16 texture of its own
 * (Trinkets slot icons, {@code textures/gui/slots/*.png}).
 */
public record SlotIcon(ResourceLocation location, boolean atlasSprite)
{
	public static SlotIcon sprite(ResourceLocation sprite)
	{
		return new SlotIcon(sprite, true);
	}

	public static SlotIcon texture(ResourceLocation texture)
	{
		return new SlotIcon(texture, false);
	}

	public void draw(GuiGraphics graphics, int x, int y)
	{
		if(atlasSprite)
			graphics.blit(x, y, 0, 16, 16, Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(location));
		else
			graphics.blit(location, x, y, 0, 0, 16, 16, 16, 16);
	}

	/** Whether the icon is missing (the missing texture would be drawn) */
	public boolean isMissing(Minecraft mc)
	{
		if(atlasSprite)
			return mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(location).contents().name().equals(MissingTextureAtlasSprite.getLocation());
		return mc.getResourceManager().getResource(location).isEmpty();
	}
}
