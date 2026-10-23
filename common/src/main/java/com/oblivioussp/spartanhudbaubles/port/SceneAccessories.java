package com.oblivioussp.spartanhudbaubles.port;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The accessory mod of the test scenes (NeoForge: Curios, Fabric: Trinkets), set by the loader's scene hooks. The
 * scenes name their test slots as Curios slot types ({@code ring}, {@code curio}, ...); {@link #id} is the slot
 * identifier the HUD reports for one of them.
 */
public interface SceneAccessories
{
	/** One accessory slot type of an entity with its slots */
	interface Slot
	{
		String id();

		int size();

		ItemStack get(int index);

		void set(int index, ItemStack stack);
	}

	/** The HUD's slot identifier of a scene slot name (Curios: the same, Trinkets: group/slot) */
	String id(String name);

	/**
	 * A data pack that gives players the scene's slot types, in the order of {@code items} (whose items it makes valid
	 * there), {@code curioSlots} of them for "curio" and as many as listed for the others
	 */
	void writeSlotsDataPack(Path pack, Map<String, List<net.minecraft.world.item.Item>> items, int curioSlots) throws IOException;

	/** Every slot type of the entity */
	List<Slot> slots(LivingEntity entity);

	/** The slot types the server knows for players (the log of the scene) */
	String describe(ServerPlayer player);

	/** The command that puts an item (with an optional count after it) into a slot of the executing player */
	String setCommand(String name, int index, String item);

	/** The mod's config GUI, as the mods list opens it */
	Screen configScreen(Minecraft mc, Screen parent);

	Path configDir();

	default Slot slot(LivingEntity entity, String name)
	{
		String id = id(name);
		return slots(entity).stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow(() -> new IllegalStateException("no slot " + id));
	}
}
