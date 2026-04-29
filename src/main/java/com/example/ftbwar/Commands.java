package com.example.ftbwar;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class Commands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerWar(dispatcher);
        registerWarAccept(dispatcher);
        registerPeace(dispatcher);
        registerInfoWar(dispatcher);
        registerNonAggressionPact(dispatcher);
    }

    // ================= /war <team> =================

    static void registerWar(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("war")
                .then(net.minecraft.commands.Commands.argument("team", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayer();
                        if (player == null) return 0;

                        String selfTeam = WarManager.getTeamId(player);
                        String targetTeamName = StringArgumentType.getString(context, "team");

                        Team targetTeam = WarManager.getTeamByName(targetTeamName);

                        if (targetTeam == null) {
                            player.sendSystemMessage(Component.literal("§c❌ Команда не найдена"));
                            return 0;
                        }

                        if (!WarManager.hasEnoughOnline(player, targetTeam)) {
                            player.sendSystemMessage(Component.literal("§c❌ У врага менее 39% в онлайне. Войну начать нельзя"));
                            return 0;
                        }

                        if (WarManager.isAtWar(selfTeam, targetTeamName)) {
                            player.sendSystemMessage(Component.literal("§c❌ Вы уже в войне с этой командой"));
                            return 0;
                        }

                        if (WarManager.hasPact(selfTeam, targetTeamName)) {
                            player.sendSystemMessage(Component.literal("§c❌ У вас пакт невооруженности с этой командой"));
                            return 0;
                        }

                        // Запрашиваем войну
                        WarManager.requestWar(selfTeam, targetTeamName);

                        player.server.getPlayerList().broadcastSystemMessage(
                            Component.literal("§6⚔️ " + selfTeam + " объявил войну " + targetTeamName + "!"),
                            false
                        );

                        player.sendSystemMessage(Component.literal("§a✓ Запрос отправлен"));

                        return 1;
                    })
                )
        );
    }

    // ================= /waraccept =================

    static void registerWarAccept(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("waraccept")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) return 0;

                    String selfTeam = WarManager.getTeamId(player);
                    String attacker = WarManager.getWarRequest(selfTeam);

                    if (attacker == null) {
                        player.sendSystemMessage(Component.literal("§c❌ Нет запроса на войну"));
                        return 0;
                    }

                    WarManager.removeWarRequest(selfTeam);
                    WarManager.startWar(attacker, selfTeam);

                    player.server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§6⚔️ ВОЙНА НАЧАЛАСЬ: " + attacker + " VS " + selfTeam),
                        false
                    );

                    return 1;
                })
        );
    }

    // ================= /peace <team> =================

    static void registerPeace(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("peace")
                .then(net.minecraft.commands.Commands.argument("team", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayer();
                        if (player == null) return 0;

                        String selfTeam = WarManager.getTeamId(player);
                        String targetTeam = StringArgumentType.getString(context, "team");

                        if (!WarManager.isAtWar(selfTeam, targetTeam)) {
                            player.sendSystemMessage(Component.literal("§c❌ Вы не в вой��е с этой командой"));
                            return 0;
                        }

                        WarManager.endWar(selfTeam, targetTeam);

                        player.server.getPlayerList().broadcastSystemMessage(
                            Component.literal("§a☮️ " + selfTeam + " и " + targetTeam + " заключили мир!"),
                            false
                        );

                        return 1;
                    })
                )
        );
    }

    // ================= /infowar =================

    static void registerInfoWar(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("infowar")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) return 0;

                    String selfTeam = WarManager.getTeamId(player);
                    var enemies = WarManager.getEnemies(selfTeam);

                    if (enemies.isEmpty()) {
                        player.sendSystemMessage(Component.literal("§a✓ Вы не в войне"));
                        return 1;
                    }

                    player.sendSystemMessage(Component.literal("§6=== ВРАГИ ==="));
                    for (String enemy : enemies) {
                        player.sendSystemMessage(Component.literal("§c⚔️ " + enemy));
                    }

                    return 1;
                })
        );
    }

    // ================= /nonaggression pact <team> =================

    static void registerNonAggressionPact(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("nonaggression")
                .then(net.minecraft.commands.Commands.literal("pact")
                    .then(net.minecraft.commands.Commands.argument("team", StringArgumentType.word())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) return 0;

                            String selfTeam = WarManager.getTeamId(player);
                            String targetTeam = StringArgumentType.getString(context, "team");

                            Team target = WarManager.getTeamByName(targetTeam);
                            if (target == null) {
                                player.sendSystemMessage(Component.literal("§c❌ Команда не найдена"));
                                return 0;
                            }

                            if (WarManager.hasPact(selfTeam, targetTeam)) {
                                player.sendSystemMessage(Component.literal("§c❌ Пакт уже активен"));
                                return 0;
                            }

                            // Если в войне, завершить её и создать пакт
                            if (WarManager.isAtWar(selfTeam, targetTeam)) {
                                WarManager.endWar(selfTeam, targetTeam);
                            }

                            player.server.getPlayerList().broadcastSystemMessage(
                                Component.literal("§b📜 " + selfTeam + " и " + targetTeam + " заключили пакт невооруженности на 5 дней"),
                                false
                            );

                            return 1;
                        })
                    )
                )
        );
    }
}
