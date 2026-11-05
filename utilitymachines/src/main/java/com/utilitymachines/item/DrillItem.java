package com.utilitymachines.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** El taladrador en sí no hace nada solo: se instala en una vagoneta (ver DrillCartHandler). */
public class DrillItem extends Item {
    public DrillItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.utilitymachines.drill.1"));
        tooltip.add(Component.translatable("tooltip.utilitymachines.drill.2"));
        tooltip.add(Component.translatable("tooltip.utilitymachines.drill.3"));
    }
}
