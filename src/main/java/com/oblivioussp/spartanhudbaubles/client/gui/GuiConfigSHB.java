package com.oblivioussp.spartanhudbaubles.client.gui;

import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * Config GUI class that adds the HUD Preview button (port: on NeoForge's config screen). The values edited here apply
 * right away, so the preview shows them without saving first; the file is saved when the screen is closed.
 * @author ObliviousSpartan
 *
 */
public class GuiConfigSHB extends ConfigurationScreen.ConfigurationSectionScreen
{
	public GuiConfigSHB(ConfigurationScreen parent, ModConfig.Type type, ModConfig modConfig, Component title)
	{
		super(parent, type, modConfig, title);
	}

	@Override
	protected void init()
	{
		super.init();
		Component hudEdit = Component.translatable(ModSpartanHUDBaubles.ID + ".configuration.preview_hud");
		int width = font.width(hudEdit) + 20;
		addRenderableWidget(Button.builder(hudEdit, button -> minecraft.setScreen(new GuiPreviewHUD(this))).bounds(5, 5, width, 20).build());
	}
}
