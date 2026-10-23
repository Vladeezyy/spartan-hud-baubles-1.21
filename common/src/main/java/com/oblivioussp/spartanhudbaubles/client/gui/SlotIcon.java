package com.oblivioussp.spartanhudbaubles.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/**
 * The icon of an empty accessory slot: a sprite of the GUI atlas (Curios slot icons; Curios 14 put them there, before they were on the block atlas) or a 16 x 16 texture of its own
 * (Trinkets slot icons, {@code textures/gui/slots/*.png}).
 */
public record SlotIcon(Identifier location, boolean atlasSprite)
{
	public static SlotIcon sprite(Identifier sprite)
	{
		return new SlotIcon(sprite, true);
	}

	public static SlotIcon texture(Identifier texture)
	{
		return new SlotIcon(texture, false);
	}

	public void draw(GuiGraphicsExtractor graphics, int x, int y)
	{
		if(atlasSprite)
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, location, x, y, 16, 16);
		else
			graphics.blit(RenderPipelines.GUI_TEXTURED, location, x, y, 0, 0, 16, 16, 16, 16);
	}

	/** Whether the icon is missing (the missing texture would be drawn) */
	public boolean isMissing(Minecraft mc)
	{
		if(atlasSprite)
			return mc.getAtlasManager().getAtlasOrThrow(AtlasIds.GUI).getSprite(location).contents().name().equals(MissingTextureAtlasSprite.getLocation());
		return mc.getResourceManager().getResource(location).isEmpty();
	}
}
