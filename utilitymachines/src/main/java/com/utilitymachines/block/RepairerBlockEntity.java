package com.utilitymachines.block;

import com.utilitymachines.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reparadora: 27 huecos. Cada objeto dañado se repara solo.
 * Un objeto completamente roto tarda REPAIR_TICKS (24000 ticks = 20 minutos) en quedar entero.
 */
public class RepairerBlockEntity extends RandomizableContainerBlockEntity {
    public static final int SIZE = 27;
    public static final int REPAIR_TICKS = 20 * 60 * 20; // 20 minutos

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final float[] progress = new float[SIZE];

    public RepairerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.REPAIRER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RepairerBlockEntity be) {
        boolean changed = false;
        for (int i = 0; i < be.items.size(); i++) {
            ItemStack stack = be.items.get(i);
            if (stack.isEmpty() || !stack.isDamaged()) {
                be.progress[i] = 0;
                continue;
            }
            be.progress[i] += stack.getMaxDamage() / (float) REPAIR_TICKS;
            int whole = (int) be.progress[i];
            if (whole > 0) {
                be.progress[i] -= whole;
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - whole));
                changed = true;
            }
        }
        if (changed) be.setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.utilitymachines.repairer");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, items);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, items);
        }
    }
}
