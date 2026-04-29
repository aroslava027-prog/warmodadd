package com.example.ftbwar;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "ftbwar", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {

    private static final Map<ChunkPos, ChunkCapture> CHUNK_CAPTURES = new HashMap<>();
    private static final double CAPTURE_SPEED = 100.0 / (5 * 60); // 5 минут = 300 сек, 0.33% за сек

    // ================= PVP =================

    @SubscribeEvent
    static void onPlayerAttack(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        String attackerTeam = WarManager.getTeamId(attacker);
        String targetTeam = WarManager.getTeamId(target);

        // Разрешить PvP только если в войне
        if (!WarManager.isAtWar(attackerTeam, targetTeam)) {
            event.setCanceled(true);
        }
    }

    // ================= BLOCK BREAK =================

    @SubscribeEvent
    static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        var chunkManager = FTBChunksAPI.api().getManager();
        var claim = chunkManager.getChunk(player.serverLevel(), new ChunkPos(player.blockPosition()));

        if (claim == null) {
            // Некlaimed чанк — можно ломать
            return;
        }

        String playerTeam = WarManager.getTeamId(player);
        String ownerTeam = claim.getTeam().getId().toString();

        // Разрешить ломать блоки только если в войне с владельцем
        if (!WarManager.isAtWar(playerTeam, ownerTeam)) {
            event.setCanceled(true);
        }
    }

    // ================= CHUNK CAPTURE =================

    @SubscribeEvent
    static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            updateChunkCaptures(level);
        }
    }

    static void updateChunkCaptures(ServerLevel level) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<ChunkPos, ChunkCapture>> iterator = CHUNK_CAPTURES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ChunkPos, ChunkCapture> entry = iterator.next();
            ChunkPos chunkPos = entry.getKey();
            ChunkCapture capture = entry.getValue();

            // Проверяем, есть ли враги в чанке
            boolean attackersPresent = false;
            boolean defendersPresent = false;

            for (ServerPlayer player : level.players()) {
                if (new ChunkPos(player.blockPosition()).equals(chunkPos)) {
                    String playerTeam = WarManager.getTeamId(player);

                    if (playerTeam.equals(capture.attackerTeam)) {
                        attackersPresent = true;
                    } else {
                        defendersPresent = true;
                    }
                }
            }

            // Проверяем, есть ли 39% онлайна у защищающей команды
            Team defenderTeam = null;
            var chunkManager = FTBChunksAPI.api().getManager();
            var claimData = chunkManager.getChunk(level, chunkPos);

            if (claimData != null) {
                defenderTeam = claimData.getTeam();
            }

            boolean defenderHasEnoughOnline = defenderTeam != null && WarManager.hasEnoughOnline(
                level.players().get(0),
                defenderTeam
            );

            // Логика прогресса захвата
            if (attackersPresent && !defendersPresent && defenderHasEnoughOnline) {
                // Враги есть, защитников нет, у защитников 39%+ онлайна → захват идёт
                double dt = (now - capture.lastUpdateTime) / 1000.0;
                capture.progress += CAPTURE_SPEED * dt;
            } else if (!attackersPresent) {
                // Нет врагов → прогресс сбрасывается
                capture.progress = Math.max(0, capture.progress - CAPTURE_SPEED * 2);
            }

            capture.lastUpdateTime = now;

            // Завершение захвата
            if (capture.progress >= 100) {
                claimChunk(level, chunkPos, capture.attackerTeam);
                iterator.remove();
            }
            // Удаляем захват, если прогресс = 0
            else if (capture.progress <= 0) {
                iterator.remove();
            }
        }

        // Запуск новых захватов
        for (ServerPlayer player : level.players()) {
            ChunkPos playerChunk = new ChunkPos(player.blockPosition());

            // Только если нет активного захвата в этом чанке
            if (!CHUNK_CAPTURES.containsKey(playerChunk)) {
                var chunkManager = FTBChunksAPI.api().getManager();
                var claim = chunkManager.getChunk(level, playerChunk);

                if (claim != null) {
                    String playerTeam = WarManager.getTeamId(player);
                    String ownerTeam = claim.getTeam().getId().toString();

                    // Запускаем захват, только если в войне и у врага 39%+ онлайна
                    if (WarManager.isAtWar(playerTeam, ownerTeam)) {
                        Team ownerTeamObj = WarManager.getTeamByName(ownerTeam);
                        if (ownerTeamObj != null && WarManager.hasEnoughOnline(player, ownerTeamObj)) {
                            CHUNK_CAPTURES.put(playerChunk, new ChunkCapture(playerTeam));
                        }
                    }
                }
            }
        }
    }

    static void claimChunk(ServerLevel level, ChunkPos pos, String teamId) {
        try {
            var chunksManager = FTBChunksAPI.api().getManager();
            Team team = WarManager.getTeamByName(teamId);

            if (team == null) return;

            var teamData = chunksManager.getOrCreateData(team);
            chunksManager.forceClaim(level, team, pos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
