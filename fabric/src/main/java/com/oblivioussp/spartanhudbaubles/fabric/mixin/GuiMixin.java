package com.oblivioussp.spartanhudbaubles.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.oblivioussp.spartanhudbaubles.client.gui.HudElementBaubles;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The HUD element right above the experience bar, where the original drew it (NeoForge: a GUI layer above
 * VanillaGuiLayers.EXPERIENCE_LEVEL; Fabric API 1.21.1 has no GUI layers)
 */
@Mixin(Gui.class)
public abstract class GuiMixin
{
	@Inject(method = "renderExperienceLevel", at = @At("RETURN"))
	private void spartanhudbaubles$hud(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci)
	{
		HudElementBaubles.onHUDOverlay(graphics, delta);
	}
}
