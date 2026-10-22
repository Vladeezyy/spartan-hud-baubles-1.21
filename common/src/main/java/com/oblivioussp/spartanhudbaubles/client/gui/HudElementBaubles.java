package com.oblivioussp.spartanhudbaubles.client.gui;

import java.util.List;

import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;
import com.oblivioussp.spartanhudbaubles.client.gui.AlignmentHelper.Alignment;
import com.oblivioussp.spartanhudbaubles.client.gui.AlignmentHelper.HorizontalAlignment;
import com.oblivioussp.spartanhudbaubles.client.gui.AlignmentHelper.VerticalAlignment;
import com.oblivioussp.spartanhudbaubles.util.Config;
import com.oblivioussp.spartanhudbaubles.platform.Platform;
import com.oblivioussp.spartanhudbaubles.util.SkinType;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HudElementBaubles extends HudElement
{
	public static final Identifier LAYER = Identifier.fromNamespaceAndPath(ModSpartanHUDBaubles.ID, "baubles");
	private static HudElementBaubles ACTIVE = null;

	private static final Identifier HUD = Identifier.fromNamespaceAndPath(ModSpartanHUDBaubles.ID, "textures/gui/hud.png");
	// Port: the accessory mod's icon (Platform#drawModIcon) replaces the Baubles icon, and its generic slot icon the
	// empty Bauble slot of the preview (both were drawn from Baubles' expanded_inventory.png)
	private static final int DEFAULT_WIDTH = 130;
	private static final int DEFAULT_HEIGHT = 22;
	private static final int PREVIEW_SLOTS = 7;

	/**
	 * One slot shown in the HUD: an equipped accessory, or an empty slot with its icon. The slot identifier is the
	 * accessory mod's: a Curios slot type ({@code ring}) or a Trinkets slot ({@code hand/ring}).
	 */
	public record Bauble(String slotIdentifier, int slotIndex, ItemStack stack, SlotIcon icon) {}

	private boolean isPreview = false;
	// The position and contents of the last render (read by the test scenes)
	private int lastX, lastY;
	private List<Bauble> lastRendered = List.of();

	public HudElementBaubles()
	{
		super(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	public HudElementBaubles(boolean enablePreview)
	{
		this();
		isPreview = enablePreview;
	}

	/**
	 * The accessories to show, in the slot order of the accessory mod (as in its inventory). Hidden slot types and
	 * disabled slots are left out; empty slots only with {@code showEmpty}.
	 */
	public static List<Bauble> collectBaubles(Player player, boolean showEmpty)
	{
		return Platform.INSTANCE.collectBaubles(player, showEmpty);
	}

	// The list of the last game tick: collecting and sorting the Curios slots every frame (a slot type lookup per
	// comparison) cost up to a sixth of the frame with many Curios; the slots only change with the game ticks
	private static List<Bauble> cached = List.of();
	private static long cachedTick = Long.MIN_VALUE;
	private static Player cachedPlayer;
	private static boolean cachedShowEmpty;

	private static List<Bauble> cachedBaubles(Player player, boolean showEmpty)
	{
		long tick = player.level().getGameTime();
		if(tick != cachedTick || player != cachedPlayer || showEmpty != cachedShowEmpty)
		{
			cached = collectBaubles(player, showEmpty);
			cachedTick = tick;
			cachedPlayer = player;
			cachedShowEmpty = showEmpty;
		}
		return cached;
	}

	/**
	 * How many slots go in one row (or column, if vertical) before a new one starts
	 */
	public static int slotsPerLine(int screenLength)
	{
		int fit = Math.max(1, (screenLength - 4) / 18);
		int max = Config.MAX_SLOTS_PER_LINE.get();
		return max > 0 ? Math.min(max, fit) : fit;
	}

	@Override
	public void render(GuiGraphics graphics)
	{
		Minecraft mc = Minecraft.getInstance();
		List<Bauble> baublesToRender = List.of();

		// Fetch the item stacks to render if preview mode is not enabled (once per game tick, see cachedBaubles)
		if(!isPreview)
			baublesToRender = cachedBaubles(mc.player, Config.SHOW_EMPTY_SLOTS.get());
		lastRendered = baublesToRender;

		// Default to 7 slots for preview mode
		int slotsToRender = isPreview ? PREVIEW_SLOTS : baublesToRender.size();

		// Don't render if there is no items to render or if the player is a spectator unless in preview mode
		if(!isPreview && (baublesToRender.isEmpty() || mc.player.isSpectator()))	return;

		boolean rotateVertical = Config.ROTATE_VERTICAL.get();
		Alignment hudAlignment = Config.ALIGNMENT.get();
		SkinType skinType = Config.SKIN_TYPE.get();

		HorizontalAlignment horizontal = hudAlignment.getHorizontal();
		VerticalAlignment vertical = hudAlignment.getVertical();

		int startX = 3, startY = 3;
		int spacing = 2;
		// Port: Curios has no fixed number of slots, so the slots continue in a new row (or column) when one is full
		int perLine = slotsPerLine(rotateVertical ? graphics.guiHeight() : graphics.guiWidth());
		int lines = (slotsToRender + perLine - 1) / perLine;
		int alongLine = Math.min(slotsToRender, perLine);
		int length = (rotateVertical ? startY : startX) * 2 + 16 * alongLine + spacing * (alongLine - 1);
		int across = DEFAULT_HEIGHT + (16 + spacing) * (lines - 1);
		width = rotateVertical ? across : length;
		height = rotateVertical ? length : across;

		int x = this.getAlignedX(hudAlignment, Config.OFFSET_X.get(), graphics.guiWidth());
		int y = this.getAlignedY(hudAlignment, Config.OFFSET_Y.get(), graphics.guiHeight());
		lastX = x;
		lastY = y;


		// Draw the Baubles icon if enabled unless set to no skin type
		if(Config.SHOW_BAUBLES_ICON.get() && skinType != SkinType.NONE)
		{
			int iconX = rotateVertical ? x + 6 : horizontal == HorizontalAlignment.LEFT ? x + width + 1 : x - 11;
			int iconY = rotateVertical ? vertical == VerticalAlignment.BOTTOM ? y - 11 : y + height + 1 : y + 6;
			Platform.INSTANCE.drawModIcon(graphics, iconX, iconY);
		}

		// Draw the appropriate skin according to the skin type
		switch(skinType)
		{
			case HOTBAR_STYLE:
				drawHotbarSkin(graphics, x, y, rotateVertical ? lines : alongLine, rotateVertical ? alongLine : lines);
				break;
			case TOOLTIP_STYLE:
				int tooltipBackgroundColour = Config.TOOLTIP_BACKGROUND_COLOUR.toInteger();
				int tooltipFrameTopColour = Config.TOOLTIP_FRAME_TOP_COLOUR.toInteger();
				int tooltipFrameBottomColour = Config.TOOLTIP_FRAME_BOTTOM_COLOUR.toInteger();
				graphics.fillGradient(x + 1, y, x + width - 1, y + 1, tooltipBackgroundColour, tooltipBackgroundColour);
				graphics.fillGradient(x + 1, y + height - 1, x + width - 1, y + height, tooltipBackgroundColour, tooltipBackgroundColour);
				graphics.fillGradient(x, y + 1, x + 1, y + height - 1, tooltipBackgroundColour, tooltipBackgroundColour);
				graphics.fillGradient(x + width - 1, y + 1, x + width, y + height - 1, tooltipBackgroundColour, tooltipBackgroundColour);
				graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1, tooltipBackgroundColour, tooltipBackgroundColour);

				graphics.fillGradient(x + 1, y + 1, x + width - 1, y + 2, tooltipFrameTopColour, tooltipFrameTopColour);
				graphics.fillGradient(x + 1, y + height - 2, x + width - 1, y + height - 1, tooltipFrameBottomColour, tooltipFrameBottomColour);
				graphics.fillGradient(x + 1, y + 2, x + 2, y + height - 2, tooltipFrameTopColour, tooltipFrameBottomColour);
				graphics.fillGradient(x + width - 2, y + 2, x + width - 1, y + height - 2, tooltipFrameTopColour, tooltipFrameBottomColour);
				break;
			case MINIMAL:
				graphics.fillGradient(x, y, x + width, y + height, Config.MINIMAL_TOP_COLOUR.toInteger(), Config.MINIMAL_BOTTOM_COLOUR.toInteger());
				break;
			default:
				break;
		}

		for(int i = 0; i < slotsToRender; i++)
		{
			int alongOffset = (16 + spacing) * (i % perLine);
			int acrossOffset = (16 + spacing) * (i / perLine);
			int offsetX = x + startX + (rotateVertical ? acrossOffset : alongOffset);
			int offsetY = y + startY + (rotateVertical ? alongOffset : acrossOffset);
			if(isPreview)
			{
				// Draw "Dummy" baubles in the HUD if in preview mode
				Platform.INSTANCE.dummySlotIcon().draw(graphics, offsetX, offsetY);
				continue;
			}
			Bauble bauble = baublesToRender.get(i);
			ItemStack stack = bauble.stack();
			if(stack.isEmpty())
			{
				// An empty slot (show_empty_slots): its icon, as in the accessory mod's inventory
				bauble.icon().draw(graphics, offsetX, offsetY);
				continue;
			}
			// Draw the equipped Bauble items
			Platform.INSTANCE.onRenderBauble(false, stack, bauble.slotIdentifier(), bauble.slotIndex(), offsetX, offsetY, hudAlignment, rotateVertical, graphics, mc.font);
			graphics.renderItem(stack, offsetX, offsetY);
			graphics.renderItemDecorations(mc.font, stack, offsetX, offsetY);
			Platform.INSTANCE.onRenderBauble(true, stack, bauble.slotIdentifier(), bauble.slotIndex(), offsetX, offsetY, hudAlignment, rotateVertical, graphics, mc.font);
		}
	}

	/**
	 * Draws the hotbar skin for any number of slots. hud.png holds a single slot (0, 0), a row and a column of 7 slots
	 * and a 7 by 7 grid; each one is cut into its first slot, a middle slot and its last slot, and the middle slot is
	 * repeated. For 1 to 7 slots in one row or column this gives the same pixels as the original, which only had those
	 * (more than 7 slots showed no skin at all).
	 */
	private static void drawHotbarSkin(GuiGraphics graphics, int x, int y, int columns, int rows)
	{
		int[][] uPieces = skinPieces(columns);
		int[][] vPieces = skinPieces(rows);
		int drawX = x;
		for(int[] u : uPieces)
		{
			int drawY = y;
			for(int[] v : vPieces)
			{
				graphics.blit(RenderPipelines.GUI_TEXTURED, HUD, drawX, drawY, u[0], v[0], u[1], v[1], 256, 256);
				drawY += v[1];
			}
			drawX += u[1];
		}
	}

	/**
	 * The texture pieces (offset, size) along one direction of the hotbar skin for the given number of slots
	 */
	static int[][] skinPieces(int slots)
	{
		if(slots <= 1)
			return new int[][] { { 0, 22 } };
		int[][] pieces = new int[slots][];
		pieces[0] = new int[] { 24, 21 };
		for(int i = 1; i < slots - 1; i++)
			pieces[i] = new int[] { 45, 18 };
		pieces[slots - 1] = new int[] { 135, 19 };
		return pieces;
	}

	public static HudElementBaubles getActive()
	{
		return ACTIVE;
	}

	public int getLastX()
	{
		return lastX;
	}

	public int getLastY()
	{
		return lastY;
	}

	public List<Bauble> getLastRendered()
	{
		return lastRendered;
	}

	/**
	 * Draws the HUD element: the loader puts it right above the experience bar, where the original drew it (NeoForge:
	 * a GUI layer above VanillaGuiLayers.EXPERIENCE_LEVEL, Fabric: a mixin at the end of Gui.renderExperienceLevel)
	 */
	public static void onHUDOverlay(GuiGraphics graphics, DeltaTracker deltaTracker)
	{
		Minecraft mc = Minecraft.getInstance();
		if(Config.DISABLE_HUD.get() || mc.options.hideGui || mc.player == null)
			return;
		// Don't render the HUD if the Preview HUD GUI is being displayed
		if(mc.screen instanceof GuiPreviewHUD)
			return;
		if(ACTIVE == null)
			ACTIVE = new HudElementBaubles();

		ACTIVE.render(graphics);
	}
}
