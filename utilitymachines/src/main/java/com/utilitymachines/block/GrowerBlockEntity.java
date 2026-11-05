package com.utilitymachines.block;

import com.utilitymachines.ModRegistry;
import com.utilitymachines.menu.GrowerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Autocultivadora. Se coloca enterrada: encima va la tierra de cultivo (1 bloque) y encima de ésta el cultivo.
 *
 *   y+2  cultivo
 *   y+1  tierra de cultivo (si es tierra/césped normal, la máquina la ara sola)
 *   y    AUTOCULTIVADORA
 *
 * Cada ciclo (CYCLE_TICKS) en que falte el cultivo (porque lo cosechaste) lo vuelve a plantar ya crecido.
 * Coste: 1 "uso" de cultivo y 1 "uso" de polvo de hueso por ciclo.
 * Cada cultivo vale 3 usos y cada polvo de hueso vale 4 usos.
 */
public class GrowerBlockEntity extends RandomizableContainerBlockEntity {
    public static final int BONE_SLOT = 0;
    public static final int CROP_SLOT = 1;
    public static final int CYCLE_TICKS = 40;          // 2 segundos entre ciclos
    public static final int CROP_USES_PER_ITEM = 3;
    public static final int BONE_USES_PER_ITEM = 4;

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int timer = 0;
    private int cropUses = 0;
    private int boneUses = 0;

    public GrowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.GROWER_BE.get(), pos, state);
    }

    /** Devuelve el estado de cultivo que corresponde a ese objeto (semillas, zanahoria, patata...) o null. */
    @Nullable
    public static BlockState plantFor(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof CropBlock) {
            return blockItem.getBlock().defaultBlockState();
        }
        return null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GrowerBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (++be.timer < CYCLE_TICKS) return;
        be.timer = 0;
        be.cycle(serverLevel);
    }

    private void cycle(ServerLevel level) {
        BlockPos soilPos = worldPosition.above();
        BlockPos cropPos = worldPosition.above(2);

        ItemStack bone = items.get(BONE_SLOT);
        ItemStack seed = items.get(CROP_SLOT);
        boolean hasBone = bone.is(Items.BONE_MEAL);
        BlockState plant = plantFor(seed);

        BlockState soil = level.getBlockState(soilPos);
        // Ara tierra/césped automáticamente, pero solo si hay material y el hueco está libre
        if (plant != null && hasBone && level.getBlockState(cropPos).isAir()
                && (soil.is(Blocks.DIRT) || soil.is(Blocks.GRASS_BLOCK))) {
            level.setBlock(soilPos, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 3);
            soil = level.getBlockState(soilPos);
        }
        if (!soil.is(Blocks.FARMLAND)) return;
        if (soil.getValue(FarmBlock.MOISTURE) < 7) {
            level.setBlock(soilPos, soil.setValue(FarmBlock.MOISTURE, 7), 2);
        }

        BlockState cropState = level.getBlockState(cropPos);
        if (cropState.isAir()) {
            if (plant == null || !hasBone) return;
            if (!plant.canSurvive(level, cropPos)) return; // p. ej. sin luz suficiente
            CropBlock crop = (CropBlock) plant.getBlock();
            level.setBlock(cropPos, crop.getStateForAge(crop.getMaxAge()), 3);
            consumeCropUse();
            consumeBoneUse();
            effects(level, cropPos);
        } else if (cropState.getBlock() instanceof CropBlock crop && !crop.isMaxAge(cropState) && hasBone) {
            level.setBlock(cropPos, crop.getStateForAge(crop.getMaxAge()), 3);
            consumeBoneUse();
            effects(level, cropPos);
        }
    }

    private void consumeCropUse() {
        if (++cropUses >= CROP_USES_PER_ITEM) {
            cropUses = 0;
            ItemStack stack = items.get(CROP_SLOT);
            stack.shrink(1);
            if (stack.isEmpty()) items.set(CROP_SLOT, ItemStack.EMPTY);
        }
        setChanged();
    }

    private void consumeBoneUse() {
        if (++boneUses >= BONE_USES_PER_ITEM) {
            boneUses = 0;
            ItemStack stack = items.get(BONE_SLOT);
            stack.shrink(1);
            if (stack.isEmpty()) items.set(BONE_SLOT, ItemStack.EMPTY);
        }
        setChanged();
    }

    private void effects(ServerLevel level, BlockPos cropPos) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                cropPos.getX() + 0.5, cropPos.getY() + 0.5, cropPos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, cropPos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 0.4F, 1.0F);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == BONE_SLOT ? stack.is(Items.BONE_MEAL) : plantFor(stack) != null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.utilitymachines.auto_grower");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new GrowerMenu(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return 2;
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
        tag.putInt("CropUses", cropUses);
        tag.putInt("BoneUses", boneUses);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, items);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cropUses = tag.getInt("CropUses");
        boneUses = tag.getInt("BoneUses");
        items = NonNullList.withSize(2, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, items);
        }
    }
}
