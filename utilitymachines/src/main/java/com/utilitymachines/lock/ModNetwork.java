package com.utilitymachines.lock;

import com.utilitymachines.UtilityMachines;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UtilityMachines.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, LockSyncPacket.class, LockSyncPacket::encode,
                LockSyncPacket::decode, LockSyncPacket::handle);
    }
}
