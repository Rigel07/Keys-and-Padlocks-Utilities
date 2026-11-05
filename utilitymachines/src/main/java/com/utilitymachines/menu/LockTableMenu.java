package com.utilitymachines.menu;

import com.utilitymachines.ModRegistry;
import com.utilitymachines.item.KeyItem;
import com.utilitymachines.lock.LockTier;
import com.utilitymachines.lock.LockUtil;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Mesa de cerradura.
 *   Hueco 0: material (tablones, lingote de cobre/hierro, diamante, lingote de netherita)
 *   Hueco 1: llave a copiar
 *   Hueco 2: llave resultante      Hueco 3: candado resultante
 * Botón "Nuevo juego": gasta COST_NEW materiales y da una llave + un candado con un código nuevo.
 * Botón "Copiar llave": gasta COST_COPY material (del mismo tipo que la llave) y da una copia con el mismo código.
 */
public class LockTableMenu extends AbstractContainerMenu {
    public static final int BTN_NEW = 0;
    public static final int BTN_COPY = 1;
    public static final int COST_NEW = 3;
    public static final int COST_COPY = 1;

    private static final int MATERIAL = 0;
    private static final int KEY_IN = 1;
    private static final int KEY_OUT = 2;
    private static final int PADLOCK_OUT = 3;

    private final SimpleContainer container = new SimpleContainer(4);
    private final ContainerLevelAccess access;

    public LockTableMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public LockTableMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModRegistry.LOCK_TABLE_MENU.get(), id);
        this.access = access;

        addSlot(new Slot(container, MATERIAL, 30, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LockTier.fromMaterial(stack) != null;
            }
        });
        addSlot(new Slot(container, KEY_IN, 30, 41) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof KeyItem;
            }
        });
        addSlot(new OutputSlot(container, KEY_OUT, 134, 17));
        addSlot(new OutputSlot(container, PADLOCK_OUT, 134, 41));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return switch (id) {
            case BTN_NEW -> craftNewSet(player);
            case BTN_COPY -> copyKey();
            default -> false;
        };
    }

    private boolean craftNewSet(Player player) {
        ItemStack material = container.getItem(MATERIAL);
        LockTier tier = LockTier.fromMaterial(material);
        if (tier == null || material.getCount() < COST_NEW) return false;
        if (!container.getItem(KEY_OUT).isEmpty() || !container.getItem(PADLOCK_OUT).isEmpty()) return false;

        String code = LockUtil.generateCode(player.getRandom());
        ItemStack key = new ItemStack(ModRegistry.KEYS.get(tier).get());
        LockUtil.setCode(key, code);
        ItemStack padlock = new ItemStack(ModRegistry.PADLOCKS.get(tier).get());
        LockUtil.setCode(padlock, code);

        consumeMaterial(material, COST_NEW);
        container.setItem(KEY_OUT, key);
        container.setItem(PADLOCK_OUT, padlock);
        broadcastChanges();
        return true;
    }

    private boolean copyKey() {
        ItemStack original = container.getItem(KEY_IN);
        if (!(original.getItem() instanceof KeyItem keyItem) || LockUtil.getCode(original) == null) return false;
        ItemStack material = container.getItem(MATERIAL);
        if (LockTier.fromMaterial(material) != keyItem.getTier() || material.getCount() < COST_COPY) return false;
        if (!container.getItem(KEY_OUT).isEmpty()) return false;

        int gen = original.hasTag() ? original.getTag().getInt("Gen") : 0;
        if (gen >= 2) return false; // copia de copia de copia: demasiado desgastada
        ItemStack copy = original.copy(); // conserva código y nombre personalizado
        copy.setCount(1);
        copy.getOrCreateTag().putInt("Gen", gen + 1);
        consumeMaterial(material, COST_COPY);
        container.setItem(KEY_OUT, copy);
        broadcastChanges();
        return true;
    }

    private void consumeMaterial(ItemStack material, int amount) {
        material.shrink(amount);
        if (material.isEmpty()) container.setItem(MATERIAL, ItemStack.EMPTY);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (index < 4) {
                if (!moveItemStackTo(stack, 4, 40, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 2, false)) {
                if (index < 31) {
                    if (!moveItemStackTo(stack, 31, 40, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, 4, 31, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModRegistry.LOCK_TABLE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, container));
    }
}
