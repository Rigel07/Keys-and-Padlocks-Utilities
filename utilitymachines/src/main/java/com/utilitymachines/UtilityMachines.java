package com.utilitymachines;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(UtilityMachines.MODID)
public class UtilityMachines {
    public static final String MODID = "utilitymachines";

    public UtilityMachines() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.BLOCKS.register(bus);
        ModRegistry.ITEMS.register(bus);
        ModRegistry.BLOCK_ENTITIES.register(bus);
        ModRegistry.MENUS.register(bus);
        ModRegistry.TABS.register(bus);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(com.utilitymachines.lock.ModNetwork::register);
    }
}
