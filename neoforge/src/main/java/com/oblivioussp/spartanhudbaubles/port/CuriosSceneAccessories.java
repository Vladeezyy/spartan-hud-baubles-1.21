package com.oblivioussp.spartanhudbaubles.port;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/** The test scenes' slots as Curios slot types (dev runs only: port/ is not in the jar) */
final class CuriosSceneAccessories implements SceneAccessories
{
	@Override
	public String id(String name)
	{
		return name;
	}

	@Override
	public void writeSlotsDataPack(Path pack, Map<String, List<ItemStack>> items, int curioSlots) throws IOException
	{
		Files.createDirectories(pack.resolve("data/shb_test/curios/entities"));
		Files.createDirectories(pack.resolve("data/shb_test/curios/slots"));
		Files.createDirectories(pack.resolve("data/curios/tags/item"));
		Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":48,\"description\":\"Spartan HUD: Baubles test slots\"}}");
		Files.writeString(pack.resolve("data/shb_test/curios/entities/player.json"),
				"{\"entities\":[\"minecraft:player\"],\"slots\":[" + String.join(",", items.keySet().stream().map(name -> "\"" + name + "\"").toList()) + "]}");
		for(Map.Entry<String, List<ItemStack>> entry : items.entrySet())
		{
			int size = entry.getKey().equals("curio") ? curioSlots : entry.getValue().size();
			if(size > 1)
				Files.writeString(pack.resolve("data/shb_test/curios/slots/" + entry.getKey() + ".json"), "{\"size\":" + size + "}");
			List<String> ids = entry.getValue().stream().map(s -> "\"" + BuiltInRegistries.ITEM.getKey(s.getItem()) + "\"").distinct().toList();
			Files.writeString(pack.resolve("data/curios/tags/item/" + entry.getKey() + ".json"), "{\"values\":[" + String.join(",", ids) + "]}");
		}
	}

	@Override
	public List<Slot> slots(LivingEntity entity)
	{
		List<Slot> slots = new ArrayList<>();
		CuriosApi.getCuriosInventory(entity).ifPresent(inventory -> {
			for(ICurioStacksHandler handler : inventory.getCurios().values())
			{
				slots.add(new Slot()
				{
					@Override
					public String id()
					{
						return handler.getIdentifier();
					}

					@Override
					public int size()
					{
						return handler.getSlots();
					}

					@Override
					public ItemStack get(int index)
					{
						return handler.getStacks().getStackInSlot(index);
					}

					@Override
					public void set(int index, ItemStack stack)
					{
						handler.getStacks().setStackInSlot(index, stack);
					}
				});
			}
		});
		return slots;
	}

	@Override
	public String describe(ServerPlayer player)
	{
		return "player slots " + CuriosApi.getPlayerSlots(player).keySet() + ", inventory " + CuriosApi.getCuriosInventory(player).map(i -> i.getCurios().keySet()).orElse(null);
	}

	@Override
	public String setCommand(String name, int index, String item)
	{
		// NeoForgeNetServerHooks: /curios replace does not know the slot types of the data pack (Curios 14)
		return "shbtest set " + name + " " + index + " " + item;
	}

	@Override
	public Screen configScreen(Minecraft mc, Screen parent)
	{
		var container = ModList.get().getModContainerById(ModSpartanHUDBaubles.ID).orElseThrow();
		return IConfigScreenFactory.getForMod(container.getModInfo()).orElseThrow().createScreen(container, parent);
	}

	@Override
	public Path configDir()
	{
		return FMLPaths.CONFIGDIR.get();
	}
}
