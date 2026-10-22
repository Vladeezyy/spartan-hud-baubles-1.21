package com.oblivioussp.spartanhudbaubles.util;

import java.util.Locale;

import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

/**
 * Contains all the Skin types currently available in this mod
 * @author ObliviousSpartan
 *
 */
public enum SkinType implements TranslatableEnum
{
	NONE,
	HOTBAR_STYLE,
	TOOLTIP_STYLE,
	MINIMAL;
	
	@Override
	public Component getTranslatedName()
	{
		return Component.translatable(ModSpartanHUDBaubles.ID + ".configuration.skin_type." + name().toLowerCase(Locale.ROOT));
	}
	
	public static String[] getValidConfigValues()
	{
		SkinType[] validValues = SkinType.values();
		String[] validStrings = new String[validValues.length];
		for(int i = 0; i < validValues.length; i++)
		{
			SkinType value = validValues[i];
			validStrings[i] = value.toString();
		}
		return validStrings;
	}
}
