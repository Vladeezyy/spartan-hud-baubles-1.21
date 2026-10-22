package com.oblivioussp.spartanhudbaubles.neoforge;

import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;
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
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@Mod(value = ModSpartanHUDBaubles.ID, dist = Dist.CLIENT)
public class SpartanHUDBaublesNeoForge
{
	public SpartanHUDBaublesNeoForge(IEventBus modBus, ModContainer container)
	{
		container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
		// The config GUI in the mods menu, with the HUD Preview button
		container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new ConfigurationScreen(container, parent, GuiConfigSHB::new));
		// The original drew after the experience bar
		modBus.addListener((RegisterGuiLayersEvent ev) -> ev.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, HudElementBaubles.LAYER, HudElementBaubles::onHUDOverlay));
		modBus.addListener(FMLClientSetupEvent.class, ev -> Log.info("Finished client setup!"));
	}
}
