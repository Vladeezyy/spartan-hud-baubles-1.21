package com.oblivioussp.spartanhudbaubles.port;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;
import com.oblivioussp.spartanhudbaubles.client.gui.GuiConfigSHB;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * The test scenes' slots as Trinkets slots (dev runs only: port/ is not in the jar): the scene's slot names with
 * {@code test_} in the "chest" group, in the scene's order ({@code chest/test_ring}, ...). The prefix keeps them apart
 * from Trinkets' own chest/back and chest/necklace, whose order would win over the data pack's.
 */
final class TrinketsSceneAccessories implements SceneAccessories
{
	private static final String GROUP = "chest";
	private static final String PREFIX = "test_";
	private static final Map<String, String> ICONS = Map.of("curio", "charm", "head", "hat", "necklace", "necklace", "back", "back", "body", "cape",
			"bracelet", "glove", "hands", "glove_mirrored", "ring", "ring", "belt", "belt", "charm", "charm");

	@Override
	public String id(String name)
	{
		return GROUP + "/" + PREFIX + name;
	}

	@Override
	public void writeSlotsDataPack(Path pack, Map<String, List<ItemStack>> items, int curioSlots) throws IOException
	{
		Files.createDirectories(pack.resolve("data/trinkets/slots/" + GROUP));
		Files.createDirectories(pack.resolve("data/trinkets/entities"));
		Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":48,\"description\":\"Spartan HUD: Baubles test slots\"}}");
		List<String> ids = new ArrayList<>();
		int order = 0;
		for(Map.Entry<String, List<ItemStack>> entry : items.entrySet())
		{
			String name = entry.getKey();
			int amount = name.equals("curio") ? curioSlots : entry.getValue().size();
			Files.writeString(pack.resolve("data/trinkets/slots/" + GROUP + "/" + PREFIX + name + ".json"), "{\"order\":" + ++order + ",\"amount\":" + amount
					+ ",\"icon\":\"trinkets:gui/slots/" + ICONS.getOrDefault(name, "empty") + "\",\"validator_predicates\":[\"trinkets:all\"]}");
			ids.add("\"" + id(name) + "\"");
		}
		Files.writeString(pack.resolve("data/trinkets/entities/shb_test.json"), "{\"entities\":[\"minecraft:player\"],\"slots\":[" + String.join(",", ids) + "]}");
	}

	@Override
	public List<Slot> slots(LivingEntity entity)
	{
		List<Slot> slots = new ArrayList<>();
		TrinketsApi.getTrinketComponent(entity).ifPresent(component -> {
			List<SlotGroup> groups = new ArrayList<>(component.getGroups().values());
			groups.sort(Comparator.comparingInt(SlotGroup::getOrder).thenComparing(SlotGroup::getName));
			for(SlotGroup group : groups)
			{
				Map<String, TrinketInventory> inventories = component.getInventory().get(group.getName());
				if(inventories == null)
					continue;
				List<SlotType> types = new ArrayList<>(group.getSlots().values());
				types.sort(Comparator.comparingInt(SlotType::getOrder).thenComparing(SlotType::getName));
				for(SlotType type : types)
				{
					TrinketInventory inventory = inventories.get(type.getName());
					if(inventory != null)
						slots.add(new Slot()
						{
							@Override
							public String id()
							{
								return type.getId();
							}

							@Override
							public int size()
							{
								return inventory.getContainerSize();
							}

							@Override
							public ItemStack get(int index)
							{
								return inventory.getItem(index);
							}

							@Override
							public void set(int index, ItemStack stack)
							{
								inventory.setItem(index, stack);
							}
						});
				}
			}
		});
		return slots;
	}

	@Override
	public String describe(ServerPlayer player)
	{
		List<String> slots = new ArrayList<>();
		TrinketsApi.getPlayerSlots(player).forEach((name, group) -> group.getSlots().keySet().forEach(slot -> slots.add(name + "/" + slot)));
		return "player slots " + slots;
	}

	@Override
	public String setCommand(String name, int index, String item)
	{
		// The dev server's command (port/FabricNetServerHooks): /trinkets set fails with a count in Trinkets 3.10.0
		return "shbtest set " + GROUP + " " + PREFIX + name + " " + index + " " + item;
	}

	@Override
	public Screen configScreen(Minecraft mc, Screen parent)
	{
		return new ConfigurationScreen(ModSpartanHUDBaubles.ID, parent, GuiConfigSHB::new);
	}

	@Override
	public Path configDir()
	{
		return FabricLoader.getInstance().getConfigDir();
	}
}
