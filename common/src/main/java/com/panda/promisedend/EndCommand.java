package com.panda.promisedend;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.panda.promisedend.logic.DragonWrath;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /promisedend codex reset <targets>}: forgets every goal of ours in the Promise Codex (our advancements;
 * vanilla's, such as killing the dragon, stay). {@code /promisedend wrath get|set <0-5>}: the world's Dragon Wrath (set
 * is held under {@code dragon.max_wrath}). Game-master permission.
 */
public final class EndCommand {
    private EndCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(PromisedEnd.MODID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("codex")
                        .then(Commands.literal("reset")
                                .then(Commands.argument("targets", EntityArgument.players()).executes(EndCommand::resetCodex))))
                .then(Commands.literal("wrath")
                        .then(Commands.literal("get").executes(EndCommand::getWrath))
                        .then(Commands.literal("set")
                                .then(Commands.argument("wrath", IntegerArgumentType.integer(0, DragonWrath.MAX)).executes(EndCommand::setWrath)))));
    }

    /** Returns the Wrath the next fight is at. */
    private static int getWrath(CommandContext<CommandSourceStack> c) {
        if (!EndConfig.dragonWrath()) {
            c.getSource().sendSuccess(() -> Component.translatable("commands.promisedend.wrath.off"), false);
            return 0;
        }
        int wrath = DragonWrath.effective(c.getSource().getServer());
        c.getSource().sendSuccess(() -> Component.translatable("commands.promisedend.wrath.get", wrath, EndConfig.maxWrath()), false);
        return wrath;
    }

    private static int setWrath(CommandContext<CommandSourceStack> c) {
        int wrath = Math.min(IntegerArgumentType.getInteger(c, "wrath"), EndConfig.maxWrath());
        DragonWrath.setStored(c.getSource().getServer(), wrath);
        c.getSource().sendSuccess(() -> Component.translatable("commands.promisedend.wrath.set", wrath), true);
        if (!EndConfig.dragonWrath()) c.getSource().sendSuccess(() -> Component.translatable("commands.promisedend.wrath.off"), false);
        return wrath;
    }

    private static int resetCodex(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        players.forEach(EndCommand::resetCodex);
        c.getSource().sendSuccess(() -> Component.translatable("commands.promisedend.codex.reset", players.size()), true);
        return players.size();
    }

    /** Revokes every criterion of every advancement in our namespace. */
    public static void resetCodex(ServerPlayer player) {
        player.level().getServer().getAdvancements().getAllAdvancements().forEach(holder -> {
            if (!holder.id().getNamespace().equals(PromisedEnd.MODID)) return;
            var progress = player.getAdvancements().getOrStartProgress(holder);
            var done = new java.util.ArrayList<String>();
            progress.getCompletedCriteria().forEach(done::add);
            done.forEach(criterion -> player.getAdvancements().revoke(holder, criterion));
        });
    }
}
