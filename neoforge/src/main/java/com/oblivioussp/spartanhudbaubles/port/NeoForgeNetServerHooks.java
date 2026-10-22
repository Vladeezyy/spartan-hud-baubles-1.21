package com.oblivioussp.spartanhudbaubles.port;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.oblivioussp.spartanhudbaubles.ModSpartanHUDBaubles;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * The dev server of the multiplayer check (dev runs only, not in the jar): {@code /shbtest set <slot> <index> <item>
 * [count]} puts an item into a Curios slot of the executing player. Curios 14's own {@code /curios replace} only knows
 * the slot types present when the server started, not those of the world's data pack ("Unknown curio slot type ring").
 */
@Mod(value = ModSpartanHUDBaubles.ID, dist = Dist.DEDICATED_SERVER)
public final class NeoForgeNetServerHooks
{
	public NeoForgeNetServerHooks()
	{
		if(!"netserver".equals(System.getProperty("spartanhudbaubles.scene")))
			return;
		NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent ev) -> ev.getDispatcher().register(Commands.literal("shbtest").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("set").then(Commands.argument("slot", StringArgumentType.string())
						.then(Commands.argument("index", IntegerArgumentType.integer(0)).then(Commands.argument("item", ItemArgument.item(ev.getBuildContext()))
								.executes(context -> set(context, 1))
								.then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(context -> set(context, IntegerArgumentType.getInteger(context, "count"))))))))));
	}

	private static int set(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException
	{
		ServerPlayer player = context.getSource().getPlayerOrException();
		ItemStack stack = ItemArgument.getItem(context, "item").createItemStack(count, false);
		CuriosApi.getCuriosInventory(player).flatMap(inventory -> inventory.getStacksHandler(StringArgumentType.getString(context, "slot"))).orElseThrow()
				.getStacks().setStackInSlot(IntegerArgumentType.getInteger(context, "index"), stack);
		return 1;
	}
}
