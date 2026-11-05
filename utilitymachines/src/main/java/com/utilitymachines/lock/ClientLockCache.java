package com.utilitymachines.lock;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Copia en el cliente de los candados de su dimensión, para que el picado y las puertas no "parpadeen". */
public final class ClientLockCache {
    private static final Map<Long, LockInfo> LOCKS = new HashMap<>();

    private ClientLockCache() {}

    @Nullable
    public static LockInfo get(net.minecraft.core.BlockPos pos) {
        return LOCKS.get(pos.asLong());
    }

    public static void apply(boolean clear, List<LockSyncPacket.Entry> entries) {
        if (clear) LOCKS.clear();
        for (LockSyncPacket.Entry e : entries) {
            if (e.tier() < 0) {
                LOCKS.remove(e.pos());
            } else {
                LOCKS.put(e.pos(), new LockInfo(e.code(), LockTier.values()[e.tier()]));
            }
        }
    }
}
