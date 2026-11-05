package com.utilitymachines.lock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Candados colocados en cada dimensión (se guarda con el mundo). */
public class LockData extends SavedData {
    private static final String NAME = "utilitymachines_locks";

    private final Map<Long, LockInfo> locks = new HashMap<>();

    public static LockData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(LockData::load, LockData::new, NAME);
    }

    public static LockData load(CompoundTag tag) {
        LockData data = new LockData();
        ListTag list = tag.getList("Locks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            int tier = Math.max(0, Math.min(LockTier.values().length - 1, t.getInt("Tier")));
            UUID owner = t.hasUUID("Owner") ? t.getUUID("Owner") : null;
            data.locks.put(t.getLong("Pos"), new LockInfo(t.getString("Code"), LockTier.values()[tier],
                    owner, t.getString("OwnerName")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        locks.forEach((pos, info) -> {
            CompoundTag t = new CompoundTag();
            t.putLong("Pos", pos);
            t.putString("Code", info.code());
            t.putInt("Tier", info.tier().ordinal());
            if (info.owner() != null) t.putUUID("Owner", info.owner());
            t.putString("OwnerName", info.ownerName());
            list.add(t);
        });
        tag.put("Locks", list);
        return tag;
    }

    @Nullable
    public LockInfo get(BlockPos pos) {
        return locks.get(pos.asLong());
    }

    public void put(BlockPos pos, LockInfo info) {
        locks.put(pos.asLong(), info);
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (locks.remove(pos.asLong()) != null) setDirty();
    }

    public Map<Long, LockInfo> all() {
        return locks;
    }
}
