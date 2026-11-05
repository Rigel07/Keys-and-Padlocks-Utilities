package com.utilitymachines.lock;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Material de la llave/candado. Cuanto mejor el material, más cuesta romper un bloque cerrado sin la llave.
 */
public enum LockTier {
    //     id          divisor de velocidad de picado, a prueba de explosiones
    WOOD("wooden", 3f, false, 0.85f),
    COPPER("copper", 6f, false, 0.60f),
    IRON("iron", 15f, false, 0.30f),
    DIAMOND("diamond", 50f, true, 0.08f),
    NETHERITE("netherite", 1000f, true, 0.0f);

    public final String id;
    public final float breakDivisor;
    public final boolean explosionProof;
    /** Probabilidad de forzarlo con una ganzúa (0 = imposible). */
    public final float pickChance;

    LockTier(String id, float breakDivisor, boolean explosionProof, float pickChance) {
        this.id = id;
        this.breakDivisor = breakDivisor;
        this.explosionProof = explosionProof;
        this.pickChance = pickChance;
    }

    /** La llave maestra abre todo salvo la netherita. */
    public boolean masterKeyWorks() {
        return this != NETHERITE;
    }

    /** Netherita: imposible de romper sin la llave (salvo en creativo). */
    public boolean unbreakable() {
        return this == NETHERITE;
    }

    public Component displayName() {
        return Component.translatable("tier.utilitymachines." + id);
    }

    /** Material que se pone en la mesa de cerradura para fabricar de este tipo. */
    @Nullable
    public static LockTier fromMaterial(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(ItemTags.PLANKS)) return WOOD;
        if (stack.is(Items.COPPER_INGOT)) return COPPER;
        if (stack.is(Items.IRON_INGOT)) return IRON;
        if (stack.is(Items.DIAMOND)) return DIAMOND;
        if (stack.is(Items.NETHERITE_INGOT)) return NETHERITE;
        return null;
    }
}
