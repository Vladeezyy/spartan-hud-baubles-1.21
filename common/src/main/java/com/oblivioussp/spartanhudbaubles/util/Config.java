package com.oblivioussp.spartanhudbaubles.util;

import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;
import com.oblivioussp.spartanhudbaubles.client.gui.AlignmentHelper.Alignment;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Config class containing all setting for this mod (config/spartanhudbaubles-client.toml). The values are read when
 * the HUD renders, so changes made in the Config GUI show up right away, also in the Preview HUD screen.
 * @author ObliviousSpartan
 *
 */
public class Config
{
	public static final ModConfigSpec SPEC;

	public static final BooleanValue DISABLE_HUD;
	public static final BooleanValue SHOW_BAUBLES_ICON;
	public static final ColourValue TOOLTIP_BACKGROUND_COLOUR;
	public static final ColourValue TOOLTIP_FRAME_TOP_COLOUR;
	public static final ColourValue TOOLTIP_FRAME_BOTTOM_COLOUR;
	public static final ColourValue MINIMAL_TOP_COLOUR;
	public static final ColourValue MINIMAL_BOTTOM_COLOUR;
	public static final EnumValue<SkinType> SKIN_TYPE;
	public static final EnumValue<Alignment> ALIGNMENT;
	public static final IntValue OFFSET_X;
	public static final IntValue OFFSET_Y;
	public static final BooleanValue ROTATE_VERTICAL;
	// Port: Curios can give a player any number of slots (Baubles had 7)
	public static final BooleanValue SHOW_EMPTY_SLOTS;
	public static final IntValue MAX_SLOTS_PER_LINE;

	/**
	 * Helper class to allow individual editing of each color channel in the config
	 * @author ObliviousSpartan
	 *
	 */
	public static class ColourValue
	{
		private final IntValue r, g, b, a;

		private ColourValue(ModConfigSpec.Builder builder, String name, int defaultValue, String comment)
		{
			builder.comment(comment).translation(key(name)).push(name);
			r = builder.translation(key("red")).defineInRange("red", defaultValue >> 16 & 0xFF, 0, 255);
			g = builder.translation(key("green")).defineInRange("green", defaultValue >> 8 & 0xFF, 0, 255);
			b = builder.translation(key("blue")).defineInRange("blue", defaultValue & 0xFF, 0, 255);
			a = builder.translation(key("alpha")).defineInRange("alpha", defaultValue >> 24 & 0xFF, 0, 255);
			builder.pop();
		}

		/**
		 * Converts the value to an integer value in the format (in hex): 0xAARRGGBB
		 */
		public int toInteger()
		{
			return (a.get() & 0xFF) << 24 | (r.get() & 0xFF) << 16 | (g.get() & 0xFF) << 8 | (b.get() & 0xFF);
		}
	}

	static
	{
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

		DISABLE_HUD = builder.comment("Set to true to disable the Baubles HUD Element (which essentially disables the entire mod)")
				.translation(key("disable_hud")).define("disable_hud", false);
		SKIN_TYPE = builder.comment("Skin variant to display behind the Baubles in the HUD Element")
				.translation(key("skin_type")).defineEnum("skin_type", SkinType.HOTBAR_STYLE);
		ALIGNMENT = builder.comment("Alignment of the Baubles HUD Element")
				.translation(key("alignment")).defineEnum("alignment", Alignment.BOTTOM_RIGHT);
		OFFSET_X = builder.comment("X offset of the Baubles HUD Element. Can be used to adjust the X position")
				.translation(key("offset_x")).defineInRange("offset_x", 0, -1000, 1000);
		OFFSET_Y = builder.comment("Y offset of the Baubles HUD Element. Can be used to adjust the Y position")
				.translation(key("offset_y")).defineInRange("offset_y", 0, -1000, 1000);
		ROTATE_VERTICAL = builder.comment("Set to true to rotate the Baubles HUD Element vertically or false for horizontally")
				.translation(key("rotate_vertical")).define("rotate_vertical", false);
		SHOW_BAUBLES_ICON = builder.comment("Enables display of the Curios icon next to the Baubles HUD Element. This won't show if the skin type is set to 'NONE'")
				.translation(key("show_baubles_icon")).define("show_baubles_icon", true);
		SHOW_EMPTY_SLOTS = builder.comment("Also show the empty Curio slots (with the slot's icon), not only the equipped Curios")
				.translation(key("show_empty_slots")).define("show_empty_slots", false);
		MAX_SLOTS_PER_LINE = builder.comment("How many slots fit in one row (or column, if rotated vertically) before the HUD Element starts a new one. 0 = as many as fit on the screen")
				.translation(key("max_slots_per_line")).defineInRange("max_slots_per_line", 0, 0, 64);
		TOOLTIP_BACKGROUND_COLOUR = new ColourValue(builder, "tooltip_background_colour", Defaults.TOOLTIP_BACKGROUND_COLOUR, "Background colour for the tooltip skin variant");
		TOOLTIP_FRAME_TOP_COLOUR = new ColourValue(builder, "tooltip_frame_top_colour", Defaults.TOOLTIP_FRAME_TOP_COLOUR, "Frame top colour for the tooltip skin variant");
		TOOLTIP_FRAME_BOTTOM_COLOUR = new ColourValue(builder, "tooltip_frame_bottom_colour", Defaults.TOOLTIP_FRAME_BOTTOM_COLOUR, "Frame bottom colour for the tooltip skin variant");
		MINIMAL_TOP_COLOUR = new ColourValue(builder, "minimal_top_colour", Defaults.MINIMAL_TOP_COLOUR, "Top colour for the minimal skin variant");
		MINIMAL_BOTTOM_COLOUR = new ColourValue(builder, "minimal_bottom_colour", Defaults.MINIMAL_BOTTOM_COLOUR, "Bottom colour for the minimal skin variant");

		SPEC = builder.build();
	}

	private static String key(String name)
	{
		return ModSpartanHUDBaubles.ID + ".configuration." + name;
	}
}
