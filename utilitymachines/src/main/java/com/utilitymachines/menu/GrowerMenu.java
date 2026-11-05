package com.utilitymachines.menu;

import com.utilitymachines.ModRegistry;
import com.utilitymachines.block.GrowerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GrowerMenu extends AbstractContainerMenu {
    private final Container container;

    public GrowerMenu(int id, Inventory playerInventory, Container container) {
        super(ModRegistry.GROWER_MENU.get(), id);
        checkContainerSize(container, 2);
        this.container = container;
        container.startOpen(playerInventory.player);

        addSlot(new Slot(container, GrowerBlockEntity.BONE_SLOT, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BONE_MEAL);
            }
        });
        addSlot(new Slot(container, GrowerBlockEntity.CROP_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GrowerBlockEntity.plantFor(stack) != null;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (index < 2) {
                if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 2, false)) {
                return ItemStack.EMPTY;
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
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
