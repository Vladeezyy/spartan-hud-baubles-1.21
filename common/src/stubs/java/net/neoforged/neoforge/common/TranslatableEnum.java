package net.neoforged.neoforge.common;

import net.minecraft.network.chat.Component;

/**
 * Compile-time stub of NeoForge's TranslatableEnum for the common project (not in any jar): at runtime NeoForge has
 * the real one, and so has Forge Config API Port on Fabric. Its own API jar for the common code is intermediary-mapped,
 * so this interface (which names a Minecraft class) cannot be compiled against from it.
 */
public interface TranslatableEnum
{
	Component getTranslatedName();
}
