package com.utilitymachines.client;

import com.utilitymachines.ModRegistry;
import com.utilitymachines.UtilityMachines;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = UtilityMachines.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModRegistry.GROWER_MENU.get(), GrowerScreen::new);
            MenuScreens.register(ModRegistry.LOCK_TABLE_MENU.get(), LockTableScreen::new);
        });
    }
}
