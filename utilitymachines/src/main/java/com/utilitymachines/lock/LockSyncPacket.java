package com.utilitymachines.lock;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class LockSyncPacket {
    /** tier = -1 significa "candado retirado". */
    public record Entry(long pos, int tier, String code) {}

    private final boolean clear;
    private final List<Entry> entries;

    public LockSyncPacket(boolean clear, List<Entry> entries) {
        this.clear = clear;
        this.entries = entries;
    }

    public static void encode(LockSyncPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.clear);
        buf.writeVarInt(p.entries.size());
        for (Entry e : p.entries) {
            buf.writeLong(e.pos());
            buf.writeByte(e.tier());
            buf.writeUtf(e.code());
        }
    }

    public static LockSyncPacket decode(FriendlyByteBuf buf) {
        boolean clear = buf.readBoolean();
        int size = buf.readVarInt();
        List<Entry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new Entry(buf.readLong(), buf.readByte(), buf.readUtf()));
        }
        return new LockSyncPacket(clear, list);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientLockCache.apply(clear, entries));
        ctx.get().setPacketHandled(true);
    }
}
