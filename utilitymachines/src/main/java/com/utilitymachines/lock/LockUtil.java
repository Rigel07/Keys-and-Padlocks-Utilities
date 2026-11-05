package com.utilitymachines.lock;

import com.utilitymachines.ModRegistry;
import com.utilitymachines.item.KeyItem;
import com.utilitymachines.item.KeyringItem;
import com.utilitymachines.item.MasterKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class LockUtil {
    private static final String CODE_TAG = "LockCode";

    private LockUtil() {}

    // ------------------------------------------------------------ códigos

    /** Código tipo "AA6667896": 2 letras + 7 dígitos. */
    public static String generateCode(RandomSource random) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2; i++) sb.append((char) ('A' + random.nextInt(26)));
        for (int i = 0; i < 7; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }

    @Nullable
    public static String getCode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(CODE_TAG) ? tag.getString(CODE_TAG) : null;
    }

    public static void setCode(ItemStack stack, String code) {
        stack.getOrCreateTag().putString(CODE_TAG, code);
    }

    /** Llave suelta, llave dentro de un llavero, o llave maestra (salvo netherita), en cualquier mano. */
    public static boolean holdsKey(Player player, LockInfo info) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (opens(stack, info)) return true;
        }
        return false;
    }

    public static boolean opens(ItemStack stack, LockInfo info) {
        if (stack.getItem() instanceof KeyItem) return info.code().equals(getCode(stack));
        if (stack.getItem() instanceof MasterKeyItem) return info.tier().masterKeyWorks();
        if (stack.getItem() instanceof KeyringItem) return KeyringItem.contains(stack, info.code());
        return false;
    }

    // ---- cerraduras forzadas con ganzúa: quedan abiertas unos segundos (solo servidor)
    private static final java.util.Map<String, Long> PICKED = new java.util.HashMap<>();
    public static final int PICK_OPEN_TICKS = 200; // 10 s

    private static String pickKey(Level level, BlockPos pos) {
        return level.dimension().location() + "@" + canonical(level, pos).asLong();
    }

    public static void markPicked(Level level, BlockPos pos) {
        PICKED.put(pickKey(level, pos), level.getGameTime() + PICK_OPEN_TICKS);
    }

    public static boolean isPicked(Level level, BlockPos pos) {
        String k = pickKey(level, pos);
        Long until = PICKED.get(k);
        if (until == null) return false;
        if (level.getGameTime() > until) {
            PICKED.remove(k);
            return false;
        }
        return true;
    }

    public static ItemStack makePadlock(LockInfo info) {
        ItemStack stack = new ItemStack(ModRegistry.PADLOCKS.get(info.tier()).get());
        setCode(stack, info.code());
        return stack;
    }

    // ------------------------------------------------------------ bloques

    /** Puertas, trampillas, portones y cualquier bloque con inventario/menú (cofres, barriles, hornos...). */
    public static boolean isLockable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof FenceGateBlock) {
            return true;
        }
        return level.getBlockEntity(pos) instanceof MenuProvider;
    }

    /** Las puertas se guardan siempre por su mitad inferior. */
    public static BlockPos canonical(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    /** Posiciones que comparten candado: el cofre doble cuenta como un solo bloque. */
    public static List<BlockPos> group(Level level, BlockPos pos) {
        BlockPos base = canonical(level, pos);
        List<BlockPos> out = new ArrayList<>(2);
        out.add(base);
        BlockState state = level.getBlockState(base);
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            out.add(base.relative(ChestBlock.getConnectedDirection(state)));
        }
        return out;
    }

    /** Candado que protege esa posición, o null. Funciona en servidor y en cliente. */
    @Nullable
    public static LockInfo getLock(Level level, BlockPos pos) {
        for (BlockPos p : group(level, pos)) {
            if (level instanceof ServerLevel serverLevel) {
                LockInfo info = LockData.get(serverLevel).get(p);
                if (info == null) continue;
                if (!isLockable(level, p)) { // el bloque ya no existe: se limpia el candado huérfano
                    LockManager.removeLock(serverLevel, List.of(p));
                    continue;
                }
                return info;
            } else {
                LockInfo info = ClientLockCache.get(p);
                if (info != null) return info;
            }
        }
        return null;
    }
}
