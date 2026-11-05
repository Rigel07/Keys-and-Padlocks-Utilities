package com.utilitymachines.item;

import com.utilitymachines.lock.LockTier;
import com.utilitymachines.lock.LockUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Llave con código único. El nombre por defecto es "Llave <material> <código>", pero se puede renombrar en un yunque. */
public class KeyItem extends Item {
    private final LockTier tier;

    public KeyItem(LockTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public LockTier getTier() {
        return tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        String code = LockUtil.getCode(stack);
        return Component.translatable("item.utilitymachines.key", tier.displayName(), code == null ? "?" : code);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String code = LockUtil.getCode(stack);
        tooltip.add(Component.translatable("tooltip.utilitymachines.code", code == null ? "?" : code)
                .withStyle(ChatFormatting.GRAY));
        int gen = Math.min(stack.hasTag() ? stack.getTag().getInt("Gen") : 0, 2);
        tooltip.add(Component.translatable("tooltip.utilitymachines.key_gen" + gen)
                .withStyle(gen == 0 ? ChatFormatting.GREEN : gen == 1 ? ChatFormatting.YELLOW : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.utilitymachines.key_hint"));
        tooltip.add(Component.translatable("tooltip.utilitymachines.key_hint2"));
    }
}
