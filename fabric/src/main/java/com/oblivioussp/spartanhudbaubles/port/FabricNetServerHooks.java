package com.oblivioussp.spartanhudbaubles.port;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The dev server of the multiplayer check (dev runs only): {@code /shbtest set <group> <slot> <index> <item> [count]}
 * puts an item into a Trinkets slot of the executing player. Trinkets' own {@code /trinkets set ... <count>} fails
 * (3.10.0 reads the count argument by the name "amount", registered as "count").
 */
public final class FabricNetServerHooks
{
	public static void init()
	{
		CommandRegistrationCallback.EVENT.register((dispatcher, registries, selection) -> dispatcher.register(Commands.literal("shbtest").requires(source -> source.hasPermission(2))
				.then(Commands.literal("set").then(Commands.argument("group", StringArgumentType.string()).then(Commands.argument("slot", StringArgumentType.string())
						.then(Commands.argument("index", IntegerArgumentType.integer(0)).then(Commands.argument("item", ItemArgument.item(registries))
								.executes(context -> set(context, 1))
								.then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(context -> set(context, IntegerArgumentType.getInteger(context, "count")))))))))));
	}

	private static int set(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException
	{
		ServerPlayer player = context.getSource().getPlayerOrException();
		ItemStack stack = ItemArgument.getItem(context, "item").createItemStack(count, false);
		TrinketInventory inventory = TrinketsApi.getTrinketComponent(player).map(c -> c.getInventory().get(StringArgumentType.getString(context, "group")))
				.map(g -> g.get(StringArgumentType.getString(context, "slot"))).orElseThrow();
		inventory.setItem(IntegerArgumentType.getInteger(context, "index"), stack);
		return 1;
	}
}
