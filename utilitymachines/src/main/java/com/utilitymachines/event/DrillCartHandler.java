package com.utilitymachines.event;

import com.utilitymachines.ModRegistry;
import com.utilitymachines.UtilityMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Taladrador sobre vagoneta.
 *
 *  - Clic derecho a una vagoneta con el taladrador en la mano: lo instala.
 *  - Agachado + clic derecho con la mano vacía: activa / desactiva. Avanza por el carril
 *    hacia donde mires (en el eje del carril).
 *  - Pica un frente de 3 de ancho x 2 de alto (3 bloques delante + los 3 de encima),
 *    coloca un carril delante y avanza.
 *  - Si la vagoneta tiene inventario (con cofre o con tolva), los carriles se toman de ahí
 *    y lo que se pica se guarda ahí. Con una vagoneta normal solo avanza por carriles ya puestos
 *    y los objetos picados caen al suelo.
 */
@Mod.EventBusSubscriber(modid = UtilityMachines.MODID)
public class DrillCartHandler {
    private static final String TAG_INSTALLED = "UMDrillInstalled";
    private static final String TAG_ACTIVE = "UMDrillActive";
    private static final String TAG_DIR = "UMDrillDir";
    private static final String TAG_PROGRESS = "UMDrillProgress";

    private static final int MINE_TICKS = 20;     // ticks para picar cada tramo
    private static final double SPEED = 0.12D;    // velocidad de avance (bloques/tick)
    /** false = el taladrador pone los carriles solo y gratis. true = los toma del inventario de la vagoneta. */
    private static final boolean REQUIRE_RAILS = false;

    private static final Set<AbstractMinecart> ACTIVE = new HashSet<>();

    // ------------------------------------------------------------ interacción

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractMinecart cart)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack held = event.getItemStack();
        CompoundTag data = cart.getPersistentData();
        boolean installed = data.getBoolean(TAG_INSTALLED);

        if (held.is(ModRegistry.DRILL.get())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            if (level.isClientSide) return;
            if (installed) {
                player.displayClientMessage(Component.translatable("message.utilitymachines.drill_already"), true);
                return;
            }
            data.putBoolean(TAG_INSTALLED, true);
            if (!player.getAbilities().instabuild) held.shrink(1);
            player.displayClientMessage(Component.translatable("message.utilitymachines.drill_installed"), true);
            return;
        }

        if (installed && player.isShiftKeyDown() && held.isEmpty()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            if (!level.isClientSide) toggle(cart, player);
        }
    }

    private static void toggle(AbstractMinecart cart, Player player) {
        CompoundTag data = cart.getPersistentData();
        if (data.getBoolean(TAG_ACTIVE)) {
            data.putBoolean(TAG_ACTIVE, false);
            ACTIVE.remove(cart);
            cart.setDeltaMovement(Vec3.ZERO);
            player.displayClientMessage(Component.translatable("message.utilitymachines.drill_off"), true);
            return;
        }

        Level level = cart.level();
        BlockPos pos = cart.blockPosition();
        BlockState railState = level.getBlockState(pos);
        if (!(railState.getBlock() instanceof BaseRailBlock rail)) {
            player.displayClientMessage(Component.translatable("message.utilitymachines.drill_need_rail"), true);
            return;
        }
        RailShape shape = rail.getRailDirection(railState, level, pos, cart);
        Direction[] axis;
        switch (shape) {
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> axis = new Direction[]{Direction.NORTH, Direction.SOUTH};
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> axis = new Direction[]{Direction.EAST, Direction.WEST};
            default -> {
                player.displayClientMessage(Component.translatable("message.utilitymachines.drill_need_straight"), true);
                return;
            }
        }
        Vec3 look = player.getLookAngle();
        double a = look.x * axis[0].getStepX() + look.z * axis[0].getStepZ();
        double b = look.x * axis[1].getStepX() + look.z * axis[1].getStepZ();
        Direction dir = b > a ? axis[1] : axis[0];

        data.putInt(TAG_DIR, dir.get3DDataValue());
        data.putInt(TAG_PROGRESS, 0);
        data.putBoolean(TAG_ACTIVE, true);
        ACTIVE.add(cart);
        player.displayClientMessage(Component.translatable("message.utilitymachines.drill_on"), true);
    }

    // ------------------------------------------------------------ ciclo de vida

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof AbstractMinecart cart
                && cart.getPersistentData().getBoolean(TAG_ACTIVE)) {
            ACTIVE.add(cart);
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof AbstractMinecart cart)) return;
        ACTIVE.remove(cart);
        // Si la vagoneta se destruye, se recupera el taladrador
        if (cart.getRemovalReason() == Entity.RemovalReason.KILLED
                && cart.getPersistentData().getBoolean(TAG_INSTALLED)
                && event.getLevel() instanceof ServerLevel serverLevel) {
            Vec3 p = cart.position();
            serverLevel.getServer().execute(() -> serverLevel.addFreshEntity(
                    new ItemEntity(serverLevel, p.x, p.y, p.z, new ItemStack(ModRegistry.DRILL.get()))));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        Iterator<AbstractMinecart> it = ACTIVE.iterator();
        while (it.hasNext()) {
            AbstractMinecart cart = it.next();
            if (cart.isRemoved()) {
                it.remove();
                continue;
            }
            if (cart.level() != level) continue;
            if (!tickCart(level, cart)) {
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------ lógica de excavación

    /** @return false si el taladrador se ha desactivado y debe salir de la lista. */
    private static boolean tickCart(ServerLevel level, AbstractMinecart cart) {
        CompoundTag data = cart.getPersistentData();
        if (!data.getBoolean(TAG_ACTIVE)) return false;

        Direction dir = Direction.from3DDataValue(data.getInt(TAG_DIR));
        BlockPos current = cart.blockPosition();
        if (!(level.getBlockState(current).getBlock() instanceof BaseRailBlock)) {
            cart.setDeltaMovement(Vec3.ZERO);
            return true; // descarrilada: espera
        }

        BlockPos front = current.relative(dir);
        Direction side = dir.getClockWise();
        BlockPos[] slice = {
                front, front.relative(side), front.relative(side.getOpposite()),
                front.above(), front.above().relative(side), front.above().relative(side.getOpposite())
        };

        // 1) ¿hay algo que picar delante?
        boolean needsMining = false;
        for (BlockPos p : slice) {
            BlockState s = level.getBlockState(p);
            if (!needsMining(s)) continue;
            if (!canMine(level, p, s)) {
                return fail(level, cart, "message.utilitymachines.drill_obstacle");
            }
            needsMining = true;
        }
        if (needsMining) {
            cart.setDeltaMovement(Vec3.ZERO);
            if (cart instanceof Container inv && !hasFreeSlot(inv)) {
                return fail(level, cart, "message.utilitymachines.drill_full");
            }
            int progress = data.getInt(TAG_PROGRESS) + 1;
            if (progress < MINE_TICKS) {
                data.putInt(TAG_PROGRESS, progress);
                return true;
            }
            data.putInt(TAG_PROGRESS, 0);
            if (mineSlice(level, cart, slice)) {
                return fail(level, cart, "message.utilitymachines.drill_full");
            }
            return true;
        }

        // 2) asegurar el carril delante
        if (!(level.getBlockState(front).getBlock() instanceof BaseRailBlock)) {
            BlockState rail = Blocks.RAIL.defaultBlockState();
            if (!rail.canSurvive(level, front)) {
                return fail(level, cart, "message.utilitymachines.drill_no_floor");
            }
            if (REQUIRE_RAILS && !consumeRail(cart)) {
                cart.setDeltaMovement(Vec3.ZERO);
                if (level.getGameTime() % 100 == 0) {
                    notifyNearby(level, cart, "message.utilitymachines.drill_no_rails");
                }
                return true;
            }
            level.setBlock(front, rail, 3);
        }

        // 3) avanzar
        cart.setDeltaMovement(dir.getStepX() * SPEED, cart.getDeltaMovement().y, dir.getStepZ() * SPEED);
        return true;
    }

    private static boolean needsMining(BlockState state) {
        return !state.isAir() && !(state.getBlock() instanceof BaseRailBlock);
    }

    private static boolean canMine(ServerLevel level, BlockPos pos, BlockState state) {
        return state.getDestroySpeed(level, pos) >= 0 && state.getFluidState().isEmpty();
    }

    /** @return true si algún objeto no cupo en el inventario (está lleno). */
    private static boolean mineSlice(ServerLevel level, AbstractMinecart cart, BlockPos[] slice) {
        boolean overflow = false;
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        for (BlockPos pos : slice) {
            BlockState state = level.getBlockState(pos);
            if (!needsMining(state) || !canMine(level, pos, state)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            List<ItemStack> drops = Block.getDrops(state, level, pos, be, cart, tool);
            state.spawnAfterBreak(level, pos, tool, true); // experiencia de menas
            level.destroyBlock(pos, false, cart);
            for (ItemStack drop : drops) {
                if (!store(level, cart, pos, drop)) overflow = true;
            }
        }
        return overflow;
    }

    /** @return false si no cupo todo en el inventario (lo que sobra cae al suelo). */
    private static boolean store(ServerLevel level, AbstractMinecart cart, BlockPos pos, ItemStack stack) {
        ItemStack remainder = stack;
        if (cart instanceof Container container) {
            remainder = HopperBlockEntity.addItem(null, container, stack, null);
        }
        if (!remainder.isEmpty()) {
            Block.popResource(level, pos, remainder);
            return !(cart instanceof Container);
        }
        return true;
    }

    private static boolean hasFreeSlot(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) return true;
        }
        return false;
    }

    private static boolean consumeRail(AbstractMinecart cart) {
        if (!(cart instanceof Container container)) return false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(Items.RAIL)) {
                stack.shrink(1);
                container.setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean fail(ServerLevel level, AbstractMinecart cart, String messageKey) {
        CompoundTag data = cart.getPersistentData();
        data.putBoolean(TAG_ACTIVE, false);
        cart.setDeltaMovement(Vec3.ZERO);
        notifyNearby(level, cart, messageKey);
        return false;
    }

    private static void notifyNearby(ServerLevel level, AbstractMinecart cart, String key) {
        for (Player player : level.players()) {
            if (player.distanceToSqr(cart) < 24 * 24) {
                player.displayClientMessage(Component.translatable(key), true);
            }
        }
    }
}
