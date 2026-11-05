package com.utilitymachines.event;

import com.utilitymachines.ModRegistry;
import com.utilitymachines.UtilityMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ascensor: de pie sobre un bloque ascensor,
 *   SALTAR  -> te lleva al ascensor más cercano por ENCIMA (misma columna)
 *   AGACHARSE -> te lleva al ascensor más cercano por DEBAJO
 */
@Mod.EventBusSubscriber(modid = UtilityMachines.MODID)
public class ElevatorEvents {
    private static final int MAX_RANGE = 96;
    private static final int COOLDOWN_TICKS = 8;

    private static final Set<UUID> SNEAKING = new HashSet<>();
    private static final Map<UUID, Long> LAST_USE = new HashMap<>();

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tryTeleport(player, 1);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.isShiftKeyDown()) {
            if (SNEAKING.add(player.getUUID())) {
                tryTeleport(player, -1);
            }
        } else {
            SNEAKING.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SNEAKING.remove(event.getEntity().getUUID());
        LAST_USE.remove(event.getEntity().getUUID());
    }

    private static void tryTeleport(ServerPlayer player, int direction) {
        Level level = player.level();
        BlockPos standingOn = player.blockPosition().below();
        if (!level.getBlockState(standingOn).is(ModRegistry.ELEVATOR.get())) return;

        long now = level.getGameTime();
        Long last = LAST_USE.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) return;

        BlockPos target = findTarget(level, standingOn, direction);
        if (target == null) return;
        LAST_USE.put(player.getUUID(), now);

        level.playSound(null, standingOn, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.5F, 1.2F);
        player.connection.teleport(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.resetFallDistance();
        level.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.5F, 1.2F);
    }

    private static BlockPos findTarget(Level level, BlockPos from, int direction) {
        for (int i = 1; i <= MAX_RANGE; i++) {
            BlockPos candidate = from.offset(0, direction * i, 0);
            if (level.isOutsideBuildHeight(candidate)) return null;
            if (!level.getBlockState(candidate).is(ModRegistry.ELEVATOR.get())) continue;
            BlockPos feet = candidate.above();
            BlockPos head = candidate.above(2);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(head).getCollisionShape(level, head).isEmpty()) {
                return candidate;
            }
        }
        return null;
    }
}
