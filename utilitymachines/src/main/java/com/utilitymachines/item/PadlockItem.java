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

/** Candado: clic derecho (mano principal) sobre puerta, cofre, etc. para cerrarlo. Se abre con la llave del mismo código. */
public class PadlockItem extends Item {
    private final LockTier tier;

    public PadlockItem(LockTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public LockTier getTier() {
        return tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        String code = LockUtil.getCode(stack);
        return Component.translatable("item.utilitymachines.padlock", tier.displayName(), code == null ? "?" : code);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String code = LockUtil.getCode(stack);
        tooltip.add(Component.translatable("tooltip.utilitymachines.code", code == null ? "?" : code)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.utilitymachines.padlock_hint"));
        tooltip.add(Component.translatable("tooltip.utilitymachines.padlock_strength." + tier.id)
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
