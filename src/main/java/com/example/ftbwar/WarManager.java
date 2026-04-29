package com.example.ftbwar;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class WarManager {

    static Map<String, Set<String>> wars = new HashMap<>();
    static Map<String, Map<String, Long>> pacts = new HashMap<>();
    static Map<String, String> warRequests = new HashMap<>();

    // ================= TEAM HELPERS =================

    public static Team getTeamObj(ServerPlayer p) {
        return FTBTeamsAPI.api().getManager().getTeamForPlayer(p);
    }

    public static String getTeamId(ServerPlayer p) {
        Team t = getTeamObj(p);
        return t == null ? "none" : t.getId().toString();
    }

    public static Team getTeamByName(String name) {
        return FTBTeamsAPI.api().getManager().getTeam(name);
    }

    // ================= ONLINE 39% CHECK =================

    public static boolean hasEnoughOnline(ServerPlayer player, Team enemy) {
        if (enemy == null || enemy.getMembers().isEmpty()) return false;

        int total = enemy.getMembers().size();
        int online = 0;

        for (var member : enemy.getMembers()) {
            if (player.server.getPlayerList().getPlayer(member.getId()) != null) {
                online++;
            }
        }

        return ((double) online / total) >= 0.39;
    }

    // ================= WAR LOGIC =================

    public static boolean isAtWar(String teamA, String teamB) {
        return wars.getOrDefault(teamA, Set.of()).contains(teamB);
    }

    public static boolean hasPact(String teamA, String teamB) {
        Map<String, Long> teamPacts = pacts.getOrDefault(teamA, new HashMap<>());
        Long expireTime = teamPacts.get(teamB);
        return expireTime != null && expireTime > System.currentTimeMillis();
    }

    public static void startWar(String teamA, String teamB) {
        if (hasPact(teamA, teamB)) {
            return;
        }

        wars.computeIfAbsent(teamA, k -> new HashSet<>()).add(teamB);
        wars.computeIfAbsent(teamB, k -> new HashSet<>()).add(teamA);
    }

    public static void endWar(String teamA, String teamB) {
        wars.getOrDefault(teamA, new HashSet<>()).remove(teamB);
        wars.getOrDefault(teamB, new HashSet<>()).remove(teamA);

        // 5 дней = 432000000 мс
        long pactExpireTime = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000);

        pacts.computeIfAbsent(teamA, k -> new HashMap<>()).put(teamB, pactExpireTime);
        pacts.computeIfAbsent(teamB, k -> new HashMap<>()).put(teamA, pactExpireTime);
    }

    // ================= REQUEST LOGIC =================

    public static void requestWar(String attacker, String defender) {
        warRequests.put(defender, attacker);
    }

    public static String getWarRequest(String team) {
        return warRequests.get(team);
    }

    public static void removeWarRequest(String team) {
        warRequests.remove(team);
    }

    public static Set<String> getEnemies(String team) {
        return new HashSet<>(wars.getOrDefault(team, new HashSet<>()));
    }
}
