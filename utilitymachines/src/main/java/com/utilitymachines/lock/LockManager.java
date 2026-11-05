package com.utilitymachines.lock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Operaciones del servidor sobre los candados + sincronización con los clientes. */
public final class LockManager {
    private LockManager() {}

    public static void addLock(ServerLevel level, List<BlockPos> positions, LockInfo info) {
        LockData data = LockData.get(level);
        List<LockSyncPacket.Entry> entries = new ArrayList<>();
        for (BlockPos pos : positions) {
            data.put(pos, info);
            entries.add(new LockSyncPacket.Entry(pos.asLong(), info.tier().ordinal(), info.code()));
        }
        broadcast(level, new LockSyncPacket(false, entries));
    }

    public static void removeLock(ServerLevel level, List<BlockPos> positions) {
        LockData data = LockData.get(level);
        List<LockSyncPacket.Entry> entries = new ArrayList<>();
        for (BlockPos pos : positions) {
            data.remove(pos);
            entries.add(new LockSyncPacket.Entry(pos.asLong(), -1, ""));
        }
        broadcast(level, new LockSyncPacket(false, entries));
    }

    /** Envía a un jugador todos los candados de su dimensión actual. */
    public static void syncFull(ServerPlayer player) {
        List<LockSyncPacket.Entry> entries = new ArrayList<>();
        LockData.get(player.serverLevel()).all().forEach((pos, info) ->
                entries.add(new LockSyncPacket.Entry(pos, info.tier().ordinal(), info.code())));
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new LockSyncPacket(true, entries));
    }

    private static void broadcast(ServerLevel level, LockSyncPacket packet) {
        ModNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
    }
}
