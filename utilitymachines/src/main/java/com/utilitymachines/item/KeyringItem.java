package com.utilitymachines.item;

import com.utilitymachines.lock.LockUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Llavero: guarda hasta 12 llaves. Llave en la otra mano + clic derecho (al aire o sobre un bloque sin candado): la guarda.
 * Agachado + clic derecho: saca la última. Si lo llevas en la mano, abre todo lo que abran sus llaves.
 */
public class KeyringItem extends Item {
    public static final int MAX_KEYS = 12;

    public KeyringItem(Properties properties) {
        super(properties);
    }

    private static ListTag list(ItemStack ring) {
        return ring.hasTag() ? ring.getTag().getList("Keys", Tag.TAG_COMPOUND) : new ListTag();
    }

    public static List<String> codes(ItemStack ring) {
        List<String> out = new ArrayList<>();
        ListTag l = list(ring);
        for (int i = 0; i < l.size(); i++) {
            String code = l.getCompound(i).getCompound("tag").getString("LockCode");
            if (!code.isEmpty()) out.add(code);
        }
        return out;
    }

    public static boolean contains(ItemStack ring, String code) {
        return codes(ring).contains(code);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack ring = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(ring, true);

        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        ListTag l = list(ring);

        if (other.getItem() instanceof KeyItem && LockUtil.getCode(other) != null) {
            String code = LockUtil.getCode(other);
            if (l.size() >= MAX_KEYS) {
                player.displayClientMessage(Component.translatable("message.utilitymachines.ring_full"), true);
            } else if (contains(ring, code)) {
                player.displayClientMessage(Component.translatable("message.utilitymachines.ring_dup"), true);
            } else {
                ItemStack one = other.copy();
                one.setCount(1);
                l.add(one.save(new CompoundTag()));
                ring.getOrCreateTag().put("Keys", l);
                other.shrink(1);
                player.displayClientMessage(Component.translatable("message.utilitymachines.ring_added"), true);
                level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        } else if (player.isShiftKeyDown()) {
            if (l.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.utilitymachines.ring_empty"), true);
            } else {
                ItemStack key = ItemStack.of(l.getCompound(l.size() - 1));
                l.remove(l.size() - 1);
                ring.getOrCreateTag().put("Keys", l);
                if (!player.getInventory().add(key)) player.drop(key, false);
                player.displayClientMessage(Component.translatable("message.utilitymachines.ring_took"), true);
                level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        } else {
            player.displayClientMessage(Component.translatable("message.utilitymachines.ring_count",
                    l.size(), MAX_KEYS), true);
        }
        return InteractionResultHolder.sidedSuccess(ring, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        List<String> codes = codes(stack);
        tooltip.add(Component.translatable("tooltip.utilitymachines.ring_count", codes.size(), MAX_KEYS)
                .withStyle(ChatFormatting.GRAY));
        for (String c : codes) {
            tooltip.add(Component.literal("· " + c).withStyle(ChatFormatting.DARK_AQUA));
        }
        tooltip.add(Component.translatable("tooltip.utilitymachines.ring_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
