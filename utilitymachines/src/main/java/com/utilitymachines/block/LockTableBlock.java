package com.utilitymachines.block;

import com.utilitymachines.menu.LockTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Mesa de cerradura: fabrica juegos llave + candado y copias de llaves. */
public class LockTableBlock extends Block {
    public LockTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new LockTableMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
                Component.translatable("container.utilitymachines.lock_table")));
        return InteractionResult.CONSUME;
    }
}
