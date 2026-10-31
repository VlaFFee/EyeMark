package com.example.eyemark.server;

import com.example.eyemark.EyeMarkData;
import com.example.eyemark.Marker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/**
 * /eyemark add &lt;игроки&gt; &lt;x y z&gt; [id] [run &lt;команда&gt;]
 * /eyemark remove &lt;игроки&gt; &lt;id&gt;
 * /eyemark clear &lt;игроки&gt;
 * /eyemark list &lt;игроки&gt;
 */
public final class EyeMarkCommand {
    private static final int MAX_ID_LENGTH = 32;

    private EyeMarkCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eyemark")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(ctx -> add(ctx, null, null))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> add(ctx, StringArgumentType.getString(ctx, "id"), null))
                                                .then(Commands.literal("run")
                                                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                                                .executes(ctx -> add(ctx,
                                                                        StringArgumentType.getString(ctx, "id"),
                                                                        StringArgumentType.getString(ctx, "command")))))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(EyeMarkCommand::remove))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(EyeMarkCommand::clear)))
                .then(Commands.literal("list")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(EyeMarkCommand::list))));
    }

    private static int add(CommandContext<CommandSourceStack> ctx, String idArg, String command) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        EyeMarkData data = EyeMarkData.get(src.getServer());

        String id = idArg != null ? idArg : "eye" + data.nextId();
        if (id.length() > MAX_ID_LENGTH) {
            src.sendFailure(Component.translatable("commands.eyemark.bad_id", MAX_ID_LENGTH));
            return 0;
        }
        String dim = src.getLevel().dimension().location().toString();

        int added = 0;
        for (ServerPlayer p : targets) {
            if (data.put(p.getUUID(), new Marker(id, dim, pos.x, pos.y, pos.z, command))) {
                added++;
                EyeMarkServer.sync(p);
            } else {
                src.sendFailure(Component.translatable("commands.eyemark.too_many",
                        p.getDisplayName(), EyeMarkData.MAX_PER_PLAYER));
            }
        }
        final int count = added;
        final String markerId = id;
        src.sendSuccess(() -> Component.translatable("commands.eyemark.add", markerId, count), true);
        return added;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        String id = StringArgumentType.getString(ctx, "id");
        EyeMarkData data = EyeMarkData.get(src.getServer());

        int removed = 0;
        for (ServerPlayer p : targets) {
            if (data.remove(p.getUUID(), id) != null) {
                removed++;
                EyeMarkServer.sync(p);
            }
        }
        if (removed == 0) {
            src.sendFailure(Component.translatable("commands.eyemark.remove.none", id));
            return 0;
        }
        final int count = removed;
        src.sendSuccess(() -> Component.translatable("commands.eyemark.remove", id, count), true);
        return removed;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        EyeMarkData data = EyeMarkData.get(src.getServer());

        int total = 0;
        for (ServerPlayer p : targets) {
            total += data.clear(p.getUUID());
            EyeMarkServer.sync(p);
        }
        final int count = total;
        src.sendSuccess(() -> Component.translatable("commands.eyemark.clear", count, targets.size()), true);
        return total;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        EyeMarkData data = EyeMarkData.get(src.getServer());

        int total = 0;
        for (ServerPlayer p : targets) {
            var markers = data.of(p.getUUID());
            if (markers.isEmpty()) {
                src.sendSuccess(() -> Component.translatable("commands.eyemark.list.empty", p.getDisplayName()), false);
                continue;
            }
            src.sendSuccess(() -> Component.translatable("commands.eyemark.list.header",
                    p.getDisplayName(), markers.size()), false);
            for (Marker m : markers.values()) {
                total++;
                src.sendSuccess(() -> Component.translatable("commands.eyemark.list.entry",
                        m.id, String.format("%.1f", m.x), String.format("%.1f", m.y), String.format("%.1f", m.z), m.dim), false);
            }
        }
        return total;
    }
}
