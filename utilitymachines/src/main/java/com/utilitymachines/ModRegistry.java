package com.utilitymachines;

import com.utilitymachines.block.GrowerBlock;
import com.utilitymachines.block.LockTableBlock;
import com.utilitymachines.item.KeyItem;
import com.utilitymachines.item.PadlockItem;
import com.utilitymachines.lock.LockTier;
import com.utilitymachines.lock.LockUtil;
import com.utilitymachines.menu.LockTableMenu;
import net.minecraft.util.RandomSource;
import net.minecraft.world.flag.FeatureFlags;
import java.util.EnumMap;
import java.util.Map;
import com.utilitymachines.block.GrowerBlockEntity;
import com.utilitymachines.block.RepairerBlock;
import com.utilitymachines.block.RepairerBlockEntity;
import com.utilitymachines.block.TubeBlock;
import com.utilitymachines.block.TubeBlockEntity;
import com.utilitymachines.item.DrillItem;
import com.utilitymachines.menu.GrowerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, UtilityMachines.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, UtilityMachines.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, UtilityMachines.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, UtilityMachines.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UtilityMachines.MODID);

    // ---- Bloques ----
    public static final RegistryObject<Block> REPAIRER = BLOCKS.register("repairer",
            () -> new RepairerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.5f).sound(SoundType.METAL)));

    public static final RegistryObject<Block> TUBE = BLOCKS.register("tube",
            () -> new TubeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.0f).sound(SoundType.COPPER).noOcclusion()));

    public static final RegistryObject<Block> AUTO_GROWER = BLOCKS.register("auto_grower",
            () -> new GrowerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
                    .strength(3.0f).sound(SoundType.METAL)));

    public static final RegistryObject<Block> ELEVATOR = BLOCKS.register("elevator",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.5f).sound(SoundType.METAL)));

    // ---- Items ----
    public static final RegistryObject<Item> REPAIRER_ITEM = ITEMS.register("repairer",
            () -> new BlockItem(REPAIRER.get(), new Item.Properties()));
    public static final RegistryObject<Item> TUBE_ITEM = ITEMS.register("tube",
            () -> new BlockItem(TUBE.get(), new Item.Properties()));
    public static final RegistryObject<Item> AUTO_GROWER_ITEM = ITEMS.register("auto_grower",
            () -> new BlockItem(AUTO_GROWER.get(), new Item.Properties()));
    public static final RegistryObject<Item> ELEVATOR_ITEM = ITEMS.register("elevator",
            () -> new BlockItem(ELEVATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> DRILL = ITEMS.register("drill",
            () -> new DrillItem(new Item.Properties().stacksTo(16)));

    // ---- Mesa de cerradura, llaves y candados ----
    public static final RegistryObject<Block> LOCK_TABLE = BLOCKS.register("lock_table",
            () -> new LockTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.5f).sound(SoundType.WOOD)));
    public static final RegistryObject<Item> LOCK_TABLE_ITEM = ITEMS.register("lock_table",
            () -> new BlockItem(LOCK_TABLE.get(), new Item.Properties()));

    public static final Map<LockTier, RegistryObject<Item>> KEYS = new EnumMap<>(LockTier.class);
    public static final Map<LockTier, RegistryObject<Item>> PADLOCKS = new EnumMap<>(LockTier.class);

    static {
        for (LockTier tier : LockTier.values()) {
            KEYS.put(tier, ITEMS.register(tier.id + "_key",
                    () -> new KeyItem(tier, new Item.Properties().stacksTo(1))));
            PADLOCKS.put(tier, ITEMS.register(tier.id + "_padlock",
                    () -> new PadlockItem(tier, new Item.Properties().stacksTo(1))));
        }
    }

    // ---- Ganzúa, llavero y llave maestra ----
    public static final RegistryObject<Item> LOCKPICK = ITEMS.register("lockpick",
            () -> new com.utilitymachines.item.LockpickItem(new Item.Properties().durability(25)));
    public static final RegistryObject<Item> KEYRING = ITEMS.register("keyring",
            () -> new com.utilitymachines.item.KeyringItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MASTER_KEY = ITEMS.register("master_key",
            () -> new com.utilitymachines.item.MasterKeyItem(new Item.Properties().stacksTo(1)
                    .rarity(net.minecraft.world.item.Rarity.EPIC)));

    // ---- Block entities ----
    public static final RegistryObject<BlockEntityType<RepairerBlockEntity>> REPAIRER_BE =
            BLOCK_ENTITIES.register("repairer", () ->
                    BlockEntityType.Builder.of(RepairerBlockEntity::new, REPAIRER.get()).build(null));
    public static final RegistryObject<BlockEntityType<TubeBlockEntity>> TUBE_BE =
            BLOCK_ENTITIES.register("tube", () ->
                    BlockEntityType.Builder.of(TubeBlockEntity::new, TUBE.get()).build(null));
    public static final RegistryObject<BlockEntityType<GrowerBlockEntity>> GROWER_BE =
            BLOCK_ENTITIES.register("auto_grower", () ->
                    BlockEntityType.Builder.of(GrowerBlockEntity::new, AUTO_GROWER.get()).build(null));

    // ---- Menús ----
    public static final RegistryObject<MenuType<GrowerMenu>> GROWER_MENU =
            MENUS.register("auto_grower", () ->
                    IForgeMenuType.create((id, inv, buf) -> new GrowerMenu(id, inv, new SimpleContainer(2))));

    public static final RegistryObject<MenuType<LockTableMenu>> LOCK_TABLE_MENU =
            MENUS.register("lock_table", () -> new MenuType<>(LockTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    // ---- Pestaña creativa ----
    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.utilitymachines"))
                    .icon(() -> new ItemStack(REPAIRER_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(REPAIRER_ITEM.get());
                        output.accept(TUBE_ITEM.get());
                        output.accept(AUTO_GROWER_ITEM.get());
                        output.accept(ELEVATOR_ITEM.get());
                        output.accept(DRILL.get());
                        output.accept(LOCK_TABLE_ITEM.get());
                        output.accept(LOCKPICK.get());
                        output.accept(KEYRING.get());
                        output.accept(MASTER_KEY.get());
                        RandomSource random = RandomSource.create();
                        for (LockTier tier : LockTier.values()) {
                            String code = LockUtil.generateCode(random);
                            ItemStack key = new ItemStack(KEYS.get(tier).get());
                            LockUtil.setCode(key, code);
                            output.accept(key);
                            ItemStack padlock = new ItemStack(PADLOCKS.get(tier).get());
                            LockUtil.setCode(padlock, code);
                            output.accept(padlock);
                        }
                    })
                    .build());
}
