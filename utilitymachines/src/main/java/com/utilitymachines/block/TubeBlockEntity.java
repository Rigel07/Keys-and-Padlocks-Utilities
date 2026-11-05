package com.utilitymachines.block;

import com.utilitymachines.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tubo: absorbe objetos sueltos (en su propio bloque o sobre una placa de presión adyacente)
 * y los lleva por la red de tubos hasta el inventario más cercano que los acepte.
 * También acepta objetos que le inserten tolvas u otras máquinas.
 */
public class TubeBlockEntity extends BlockEntity {
    private static final int MOVE_DELAY = 4;   // ticks entre saltos de tubo a tubo
    private static final int MAX_NODES = 256;  // tamaño máximo de red que se explora

    private ItemStack buffer = ItemStack.EMPTY;
    private int moveTimer = 0;
    private final LazyOptional<IItemHandler> handler = LazyOptional.of(this::createHandler);

    public TubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.TUBE_BE.get(), pos, state);
    }

    public ItemStack getBuffer() {
        return buffer;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TubeBlockEntity be) {
        if (be.buffer.isEmpty()) {
            be.absorb(level, pos);
        }
        if (be.buffer.isEmpty()) {
            be.moveTimer = 0;
            return;
        }
        if (++be.moveTimer >= MOVE_DELAY) {
            be.moveTimer = 0;
            be.route(level, pos);
        }
    }

    private void absorb(Level level, BlockPos pos) {
        List<AABB> zones = new ArrayList<>();
        zones.add(new AABB(pos));
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (level.getBlockState(neighbor).getBlock() instanceof BasePressurePlateBlock) {
                zones.add(new AABB(neighbor));
            }
        }
        for (AABB zone : zones) {
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, zone, Entity::isAlive)) {
                ItemStack stack = entity.getItem();
                if (stack.isEmpty()) continue;
                buffer = stack.copy();
                entity.discard();
                setChanged();
                return;
            }
        }
    }

    /** Búsqueda en anchura: encuentra el inventario más cercano y avanza un paso hacia él. */
    private void route(Level level, BlockPos start) {
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        parent.put(start, null);
        queue.add(start);

        while (!queue.isEmpty() && parent.size() <= MAX_NODES) {
            BlockPos node = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = node.relative(dir);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                if (neighbor instanceof TubeBlockEntity) {
                    if (!parent.containsKey(neighborPos)) {
                        parent.put(neighborPos, node);
                        queue.add(neighborPos);
                    }
                    continue;
                }
                if (neighbor == null) continue;
                IItemHandler target = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite())
                        .resolve().orElse(null);
                if (target == null) continue;
                ItemStack leftover = ItemHandlerHelper.insertItem(target, buffer, true);
                if (leftover.getCount() >= buffer.getCount()) continue; // no acepta nada

                if (node.equals(start)) {
                    buffer = ItemHandlerHelper.insertItem(target, buffer, false);
                    setChanged();
                    return;
                }
                BlockPos step = node;
                while (!start.equals(parent.get(step))) {
                    step = parent.get(step);
                }
                if (level.getBlockEntity(step) instanceof TubeBlockEntity next && next.buffer.isEmpty()) {
                    next.buffer = buffer;
                    next.moveTimer = 0;
                    next.setChanged();
                    buffer = ItemStack.EMPTY;
                    setChanged();
                }
                return;
            }
        }
    }

    private IItemHandler createHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @NotNull
            @Override
            public ItemStack getStackInSlot(int slot) {
                return buffer;
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (!buffer.isEmpty() || stack.isEmpty()) return stack;
                if (!simulate) {
                    buffer = stack.copy();
                    setChanged();
                }
                return ItemStack.EMPTY;
            }

            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return true;
            }
        };
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!buffer.isEmpty()) {
            tag.put("Buffer", buffer.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        buffer = tag.contains("Buffer") ? ItemStack.of(tag.getCompound("Buffer")) : ItemStack.EMPTY;
    }
}
