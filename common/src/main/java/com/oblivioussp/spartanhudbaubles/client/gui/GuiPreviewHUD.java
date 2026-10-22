package com.oblivioussp.spartanhudbaubles.client.gui;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Preview HUD GUI class which shows config changes recently made using the Config GUI
 * @author ObliviousSpartan
 *
 */
public class GuiPreviewHUD extends Screen
{
	private static final ResourceLocation HOTBAR_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar");
	private static final ResourceLocation HOTBAR_OFFHAND_LEFT_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_left");
	private static final ResourceLocation HOTBAR_OFFHAND_RIGHT_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_right");
	private static final ResourceLocation CROSSHAIR_SPRITE = ResourceLocation.withDefaultNamespace("hud/crosshair");
	private static final ResourceLocation EXPERIENCE_BAR_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
	private static final ResourceLocation ARMOR_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_full");
	private static final ResourceLocation HEART_CONTAINER_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/container");
	private static final ResourceLocation HEART_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/full");
	private static final ResourceLocation FOOD_EMPTY_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_empty");
	private static final ResourceLocation FOOD_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/food_full");

	private final Screen parentScreen;
	private HudElementBaubles hud;
	private final Component hint = Component.translatable(ModSpartanHUDBaubles.ID + ".configuration.preview_hud.hint");
	// Where the title and the hint are drawn (top, or the middle when the HUD element is at the top centre)
	private int textY = 8;

	public GuiPreviewHUD(Screen parent)
	{
		super(Component.translatable(ModSpartanHUDBaubles.ID + ".configuration.preview_hud"));
		parentScreen = parent;
		hud = new HudElementBaubles(true);
	}

	/**
	 * Port: the original had a lone Done button above the crosshair; a page with nothing but one button looked like a
	 * leftover, so the preview now closes with a click anywhere (or Escape) and says so at the top
	 */
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		onClose();
		return true;
	}

	@Override
	public void onClose()
	{
		minecraft.setScreen(parentScreen);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
	{
		// In game the world and its HUD stay visible behind the preview
		if(minecraft.level == null)
			super.renderBackground(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
	{
		int textWidth = Math.max(font.width(title), font.width(hint)) + 8;
		textY = overlapsHud(width / 2 - textWidth / 2, 4, textWidth, 26) ? height / 2 - 50 : 8;
		super.render(graphics, mouseX, mouseY, partialTicks);
		graphics.drawCenteredString(font, title, width / 2, textY, 0xFFFFFF);
		graphics.drawCenteredString(font, hint, width / 2, textY + 12, 0xA0A0A0);

		HumanoidArm offhandSide = minecraft.options.mainHand().get().getOpposite();

		// Draw the mock default Hud (note that any Hud changing mods will not change this...)
		RenderSystem.enableBlend();

		if(minecraft.level == null)
		{
			// Draw the Hotbar
			int halfWidth = width / 2;
			int halfHeight = height / 2;
			graphics.blitSprite(HOTBAR_SPRITE, halfWidth - 91, height - 22, 182, 22);
			if(offhandSide == HumanoidArm.LEFT)
				graphics.blitSprite(HOTBAR_OFFHAND_LEFT_SPRITE, halfWidth - 91 - 29, height - 23, 29, 24);
			else
				graphics.blitSprite(HOTBAR_OFFHAND_RIGHT_SPRITE, halfWidth + 91, height - 23, 29, 24);

			// Draw the crosshair
			RenderSystem.blendFuncSeparate(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.ONE, DestFactor.ZERO);
			graphics.blitSprite(CROSSHAIR_SPRITE, (width - 15) / 2, (height - 15) / 2, 15, 15);
			RenderSystem.defaultBlendFunc();

			// Draw the XP bar
			graphics.blitSprite(EXPERIENCE_BAR_BACKGROUND_SPRITE, halfWidth - 91, height - 32 + 3, 182, 5);

			// Draw Armor
			int armorHeight = height - 49;
			for(int i = 0; i < 10; i++)
			{
				graphics.blitSprite(ARMOR_FULL_SPRITE, halfWidth - 91 + i * 8, armorHeight, 9, 9);
			}

			// Draw Health
			int healthHeight = height - 39;
			for(int i = 0; i < 10; i++)
			{
				graphics.blitSprite(HEART_CONTAINER_SPRITE, halfWidth - 91 + i * 8, healthHeight, 9, 9);
				graphics.blitSprite(HEART_FULL_SPRITE, halfWidth - 91 + i * 8, healthHeight, 9, 9);
			}

			// Draw Hunger
			int hungerHeight = height - 39;
			for(int i = 0; i < 10; i++)
			{
				graphics.blitSprite(FOOD_EMPTY_SPRITE, halfWidth + 82 - i * 8, hungerHeight, 9, 9);
				graphics.blitSprite(FOOD_FULL_SPRITE, halfWidth + 82 - i * 8, hungerHeight, 9, 9);
			}

			// Draw the hover text for recently equipped items
			Component hoverText = Component.translatable(ModSpartanHUDBaubles.ID + ".configuration.hover_text_example");
			int hoverTextSize = font.width(hoverText);
			int hoverX = (width - hoverTextSize) / 2;
			int hoverY = height - 59;
			graphics.drawString(font, hoverText, hoverX, hoverY, 0xFFFFFFFF);
		}

		RenderSystem.disableBlend();
		hud.render(graphics);
	}

	private boolean overlapsHud(int x, int y, int w, int h)
	{
		int hudX = hud.getLastX(), hudY = hud.getLastY();
		return hudX < x + w && x < hudX + hud.getWidth() && hudY < y + h && y < hudY + hud.getHeight();
	}
	
	public int getTextY()
	{
		return textY;
	}
	
	public HudElementBaubles getHud()
	{
		return hud;
	}
}
