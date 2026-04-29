package com.example.ftbwar;

public class ChunkCapture {
    public String attackerTeam;
    public double progress; // 0-100
    public long lastUpdateTime;
    public long captureStartTime;

    public ChunkCapture(String attacker) {
        this.attackerTeam = attacker;
        this.progress = 0;
        this.lastUpdateTime = System.currentTimeMillis();
        this.captureStartTime = System.currentTimeMillis();
    }

    public int getRemainingSeconds() {
        long elapsed = System.currentTimeMillis() - captureStartTime;
        long totalMs = 5 * 60 * 1000; // 5 минут
        long remaining = totalMs - elapsed;
        return Math.max(0, (int) (remaining / 1000));
    }
}
