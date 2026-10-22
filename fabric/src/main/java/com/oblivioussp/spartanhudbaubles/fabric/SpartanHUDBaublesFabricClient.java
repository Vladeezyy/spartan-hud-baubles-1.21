package com.oblivioussp.spartanhudbaubles.fabric;

import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;
import com.oblivioussp.spartanhudbaubles.client.gui.GuiConfigSHB;
import com.oblivioussp.spartanhudbaubles.util.Config;
import com.oblivioussp.spartanhudbaubles.util.Log;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.client.ConfigScreenFactoryRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/** The mod only runs on the client; the HUD element is drawn by mixin/GuiMixin (above the experience bar). */
public class SpartanHUDBaublesFabricClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		NeoForgeConfigRegistry.INSTANCE.register(ModSpartanHUDBaubles.ID, ModConfig.Type.CLIENT, Config.SPEC);
		// The config GUI (Mod Menu), with the HUD Preview button
		ConfigScreenFactoryRegistry.INSTANCE.register(ModSpartanHUDBaubles.ID, (modId, parent) -> new ConfigurationScreen(modId, parent, GuiConfigSHB::new));
		Log.info("Finished client setup!");

		// The test scenes (dev runs only, not in the jar)
		if(!System.getProperty("spartanhudbaubles.scene", "").isEmpty())
		{
			try
			{
				Class.forName("com.oblivioussp.spartanhudbaubles.port.FabricSceneHooks").getMethod("init").invoke(null);
			}
			catch(ReflectiveOperationException e)
			{
				throw new IllegalStateException("The test scenes are not in this build", e);
			}
		}
	}
}
