package com.utilitymachines.item;

import com.utilitymachines.lock.LockInfo;
import com.utilitymachines.lock.LockUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ganzúa: fuerza un candado sin llave. Si sale bien, el bloque queda abierto 10 segundos (el candado sigue puesto).
 * Si falla, se gasta más, suena la campana y se avisa al dueño del candado. La netherita no se puede forzar.
 */
public class LockpickItem extends Item {
    public LockpickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        LockInfo info = LockUtil.getLock(level, pos);
        if (info == null || LockUtil.holdsKey(player, info)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (LockUtil.isPicked(level, pos)) return InteractionResult.PASS;
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.CONSUME;

        ServerLevel sl = (ServerLevel) level;
        Vec3 c = Vec3.atCenterOf(pos);

        if (info.tier().pickChance <= 0.0F) {
            player.displayClientMessage(Component.translatable("message.utilitymachines.pick_impossible"), true);
            sl.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.8F, 0.6F);
            return InteractionResult.CONSUME;
        }

        player.getCooldowns().addCooldown(this, 25);

        if (sl.random.nextFloat() < info.tier().pickChance) {
            LockUtil.markPicked(level, pos);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(ctx.getHand()));
            player.displayClientMessage(Component.translatable("message.utilitymachines.pick_success"), true);
            sl.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.5F);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y + 0.4, c.z, 10, 0.25, 0.25, 0.25, 0.02);
        } else {
            stack.hurtAndBreak(3, player, p -> p.broadcastBreakEvent(ctx.getHand()));
            player.displayClientMessage(Component.translatable("message.utilitymachines.pick_fail"), true);
            sl.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.2F, 1.6F);
            sl.sendParticles(ParticleTypes.NOTE, c.x, c.y + 0.6, c.z, 6, 0.25, 0.25, 0.25, 0.02);
            if (info.owner() != null) {
                ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(info.owner());
                if (owner != null && owner != player) {
                    owner.displayClientMessage(Component.translatable("message.utilitymachines.alarm",
                            pos.getX(), pos.getY(), pos.getZ()), false);
                }
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.utilitymachines.lockpick").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.utilitymachines.lockpick2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
