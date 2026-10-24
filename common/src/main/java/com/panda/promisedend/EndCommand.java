package com.panda.promisedend;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /promisedend codex reset <targets>}: forgets every goal of ours in the Promise Codex (our advancements;
 * vanilla's, such as killing the dragon, stay). Game-master permission.
 */
public final class EndCommand {
    private EndCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(PromisedEnd.MODID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("codex")
                        .then(Commands.literal("reset")
                                .then(Commands.argument("targets", EntityArgument.players()).executes(EndCommand::resetCodex)))));
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
