package com.eveportalnether.item;

import com.eveportalnether.Config;
import com.eveportalnether.block.WormholeBlock;
import com.eveportalnether.registry.ModItems;
import com.eveportalnether.world.data.PortalManager;
import com.eveportalnether.world.data.PortalPair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class PortalDetectorItem extends Item {
    public PortalDetectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.eveportalnether.portal_detector.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.PORTAL_SHARD.get());
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!Config.DETECTOR_ENABLED.get()) {
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(Component.translatable("eveportalnether.detector.disabled").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof WormholeBlock)) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            if (DetectorDecryption.isDecrypting(serverPlayer)) {
                return InteractionResult.CONSUME;
            }
            PortalPair pair = PortalManager.get(serverLevel).findPairAt(serverLevel.dimension(), pos);
            if (pair == null) {
                player.displayClientMessage(Component.translatable("eveportalnether.detector.no_signal").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            DetectorDecryption.start(serverPlayer, serverLevel, pos, pair);
            player.getCooldowns().addCooldown(this, Config.DETECTOR_DECRYPT_TICKS.get());
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
