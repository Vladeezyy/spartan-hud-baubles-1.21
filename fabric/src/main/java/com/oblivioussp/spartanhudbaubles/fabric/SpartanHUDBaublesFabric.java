package com.oblivioussp.spartanhudbaubles.fabric;

import net.fabricmc.api.ModInitializer;

/**
 * Does nothing in a game: the mod only runs on the client. The dev server of the multiplayer check
 * (tools/nettest.sh, {@code spartanhudbaubles.scene=netserver}) gets the scene's equip command here; that class is
 * in port/, which is not in the jar.
 */
public class SpartanHUDBaublesFabric implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		if(!"netserver".equals(System.getProperty("spartanhudbaubles.scene")))
			return;
		try
		{
			Class.forName("com.oblivioussp.spartanhudbaubles.port.FabricNetServerHooks").getMethod("init").invoke(null);
		}
		catch(ReflectiveOperationException e)
		{
			throw new IllegalStateException("The test scenes are not in this build", e);
		}
	}
}
