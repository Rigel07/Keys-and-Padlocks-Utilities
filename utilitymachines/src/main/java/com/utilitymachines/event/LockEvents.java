package com.utilitymachines.event;

import com.utilitymachines.UtilityMachines;
import com.utilitymachines.item.KeyItem;
import com.utilitymachines.item.MasterKeyItem;
import com.utilitymachines.item.PadlockItem;
import com.utilitymachines.lock.LockInfo;
import com.utilitymachines.lock.LockManager;
import com.utilitymachines.lock.LockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Reglas de llaves y candados.
 *  - Candado en la mano + clic derecho sobre un bloque cerrable: lo cierra.
 *  - Con la llave del mismo código en cualquier mano: se usa el bloque con normalidad.
 *    Agachado + clic con la llave: se retira el candado (y vuelve a tu inventario).
 *  - Sin la llave: no se puede usar y cuesta mucho romperlo (según el material).
 */
@Mod.EventBusSubscriber(modid = UtilityMachines.MODID)
public class LockEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        ItemStack held = event.getItemStack();
        LockInfo info = LockUtil.getLock(level, pos);

        // ---- Bloque sin candado: ¿se le está poniendo uno?
        if (info == null) {
            if (held.getItem() instanceof PadlockItem padlock && LockUtil.isLockable(level, pos)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
                if (level instanceof ServerLevel serverLevel) {
                    install(serverLevel, player, held, padlock, pos);
                }
            }
            return;
        }

        // ---- Bloque con candado
        if (LockUtil.holdsKey(player, info) || LockUtil.isPicked(level, pos)) {
            if (player.isShiftKeyDown() && (held.getItem() instanceof KeyItem || held.getItem() instanceof MasterKeyItem)
                    && LockUtil.opens(held, info)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
                if (level instanceof ServerLevel serverLevel) {
                    LockManager.removeLock(serverLevel, LockUtil.group(serverLevel, pos));
                    ItemStack padlock = LockUtil.makePadlock(info);
                    if (!player.getInventory().add(padlock)) player.drop(padlock, false);
                    serverLevel.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.4F);
                    player.displayClientMessage(Component.translatable("message.utilitymachines.lock_removed"), true);
                }
            }
            return; // con la llave, uso normal
        }

        event.setUseBlock(Event.Result.DENY);
        if (!level.isClientSide && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.utilitymachines.locked"), true);
        }
    }

    private static void install(ServerLevel level, Player player, ItemStack held, PadlockItem padlock, BlockPos pos) {
        String code = LockUtil.getCode(held);
        if (code == null) {
            player.displayClientMessage(Component.translatable("message.utilitymachines.lock_blank"), true);
            return;
        }
        LockManager.addLock(level, LockUtil.group(level, pos), new LockInfo(code, padlock.getTier(), player.getUUID(), player.getGameProfile().getName()));
        if (!player.getAbilities().instabuild) held.shrink(1);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 1.4F);
        player.displayClientMessage(Component.translatable("message.utilitymachines.lock_installed"), true);
    }

    /** Sin llave, picar el bloque tarda "divisor" veces más (en servidor y cliente). */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Optional<BlockPos> position = event.getPosition();
        if (position.isEmpty()) return;
        Player player = event.getEntity();
        LockInfo info = LockUtil.getLock(player.level(), position.get());
        if (info == null || LockUtil.holdsKey(player, info) || LockUtil.isPicked(player.level(), position.get())) return;
        event.setNewSpeed(event.getNewSpeed() / info.tier().breakDivisor);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        LockInfo info = LockUtil.getLock(level, pos);
        if (info == null) return;

        Player player = event.getPlayer();
        boolean hasKey = LockUtil.holdsKey(player, info);
        if (!hasKey && info.tier().unbreakable() && !player.getAbilities().instabuild) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.utilitymachines.lock_unbreakable"), true);
            return;
        }
        LockManager.removeLock(level, LockUtil.group(level, pos));
        if (hasKey) {
            Block.popResource(level, pos, LockUtil.makePadlock(info)); // el dueño recupera el candado
        }
    }

    /** Diamante y netherita resisten las explosiones; el resto pierde el candado al destruirse el bloque. */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        event.getAffectedBlocks().removeIf(pos -> {
            LockInfo info = LockUtil.getLock(level, pos);
            if (info == null) return false;
            if (info.tier().explosionProof) return true;
            LockManager.removeLock(level, LockUtil.group(level, pos));
            return false;
        });
    }

    /** Diamante y netherita también resisten a mobs destructivos (wither, dragón). */
    @SubscribeEvent
    public static void onMobGrief(LivingDestroyBlockEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        LockInfo info = LockUtil.getLock(level, event.getPos());
        if (info != null && info.tier().explosionProof) event.setCanceled(true);
    }

    // ---- sincronización de candados con los clientes
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LockManager.syncFull(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LockManager.syncFull(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LockManager.syncFull(player);
    }
}
