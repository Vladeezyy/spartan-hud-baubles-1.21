package com.oblivioussp.spartanhudbaubles;

import com.oblivioussp.spartanhudbaubles.client.gui.GuiConfigSHB;
import com.oblivioussp.spartanhudbaubles.client.gui.HudElementBaubles;
import com.oblivioussp.spartanhudbaubles.util.Config;
import com.oblivioussp.spartanhudbaubles.util.Log;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-side only: on a dedicated server the mod is loaded but this class is never constructed.
 */
@Mod(value = ModSpartanHUDBaubles.ID, dist = Dist.CLIENT)
public class ModSpartanHUDBaubles
{
	public static final String ID = "spartanhudbaubles";
	public static final String NAME = "Spartan HUD: Baubles";

	public ModSpartanHUDBaubles(IEventBus modBus, ModContainer container)
	{
		container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
		// The config GUI in the mods menu, with the HUD Preview button
		container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new ConfigurationScreen(container, parent, GuiConfigSHB::new));
		modBus.addListener(HudElementBaubles::onRegisterGuiLayers);
		modBus.addListener(FMLClientSetupEvent.class, ev -> Log.info("Finished client setup!"));
	}
}
