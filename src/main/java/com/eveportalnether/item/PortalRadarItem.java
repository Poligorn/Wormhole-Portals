package com.eveportalnether.item;

import com.eveportalnether.Config;
import com.eveportalnether.registry.ModItems;
import com.eveportalnether.registry.ModSounds;
import net.minecraft.world.entity.LivingEntity;
import com.eveportalnether.world.data.PortalManager;
import com.eveportalnether.world.data.PortalPair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PortalRadarItem extends Item {
    public PortalRadarItem(Properties properties) {
        super(properties);
    }

    private int getRadarTicks(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return customData.copyTag().getInt("RadarTicks");
    }

    private void setRadarTicks(ItemStack stack, int ticks) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putInt("RadarTicks", ticks);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.eveportalnether.portal_radar.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.eveportalnether.portal_radar.tooltip.controls").withStyle(ChatFormatting.DARK_GRAY));
        int ticks = getRadarTicks(stack);
        if (ticks > 0) {
            tooltip.add(Component.translatable("eveportalnether.radar.fuel_left", ticks / 20).withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        if (!Config.RADAR_ENABLED.get()) {
            player.displayClientMessage(Component.translatable("eveportalnether.radar.disabled").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!player.isShiftKeyDown() && getRadarTicks(stack) > 0 && level instanceof ServerLevel serverLevel) {
            fullScan(serverLevel, player);
            player.getCooldowns().addCooldown(this, Config.RADAR_COOLDOWN_TICKS.get());
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            return InteractionResultHolder.success(stack);
        }

        int cost = Config.RADAR_BLAZE_POWDER_COST.get();
        boolean hasPowder = cost <= 0 || player.getInventory().countItem(Items.BLAZE_POWDER) >= cost;
        if (hasPowder && cost > 0) {
            int remaining = cost;
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.is(Items.BLAZE_POWDER)) {
                    int take = Math.min(remaining, invStack.getCount());
                    invStack.shrink(take);
                    remaining -= take;
                }
            }
        }

        if (hasPowder) {
            setRadarTicks(stack, getRadarTicks(stack) + Config.RADAR_DURATION_TICKS.get());
            player.displayClientMessage(Component.translatable("eveportalnether.radar.recharged").withStyle(ChatFormatting.GREEN), true);
            level.playSound(null, player.blockPosition(), ModSounds.RADAR_CHARGE.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
            return InteractionResultHolder.success(stack);
        }

        int currentTicks = getRadarTicks(stack);
        if (currentTicks > 0) {
            player.displayClientMessage(Component.translatable("eveportalnether.radar.fuel_left", currentTicks / 20).withStyle(ChatFormatting.YELLOW), true);
        } else {
            player.displayClientMessage(Component.translatable("eveportalnether.radar.no_fuel").withStyle(ChatFormatting.RED), true);
            level.playSound(null, player.blockPosition(), ModSounds.RADAR_EMPTY.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static void fullScan(ServerLevel level, Player player) {
        PortalManager manager = PortalManager.get(level);
        List<PortalPair> visible = new ArrayList<>();
        for (PortalPair pair : manager.getPairs()) {
            if (manager.isVisibleToRadar(level, pair)) {
                visible.add(pair);
            }
        }
        BlockPos origin = player.blockPosition();
        visible.sort(Comparator.comparingDouble(pair -> pair.getStandPos(level.dimension()).distSqr(origin)));
        int shown = Math.min(visible.size(), Config.RADAR_SCAN_COUNT.get());

        level.playSound(null, origin, ModSounds.RADAR_PING.get(), SoundSource.PLAYERS, 0.8F, 0.9F);
        if (shown == 0) {
            player.displayClientMessage(Component.translatable("eveportalnether.radar.none").withStyle(ChatFormatting.GRAY), false);
            return;
        }

        MutableComponent message = Component.translatable("eveportalnether.radar.scan_header", visible.size())
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        for (int i = 0; i < shown; i++) {
            BlockPos target = visible.get(i).getStandPos(level.dimension());
            double distance = Math.sqrt(target.distSqr(origin));
            message.append(Component.literal("\n"))
                    .append(Component.translatable("eveportalnether.radar.scan_entry",
                            i + 1,
                            Component.translatable(getDirectionKey(player, target)),
                            String.format("%.0f", distance)).withStyle(ChatFormatting.DARK_AQUA));
        }
        player.displayClientMessage(message, false);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player) || !Config.RADAR_ENABLED.get()) {
            return;
        }

        int ticks = getRadarTicks(stack);
        if (ticks <= 0 || level.getGameTime() % 20 != 0) {
            return;
        }

        setRadarTicks(stack, Math.max(0, ticks - 20));
        if (!isSelected || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        PortalManager manager = PortalManager.get(serverLevel);
        PortalPair closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (PortalPair record : manager.getPairs()) {
            if (!manager.isVisibleToRadar(serverLevel, record)) {
                continue;
            }
            BlockPos target = record.getStandPos(serverLevel.dimension());
            double distSq = target.distSqr(player.blockPosition());
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = record;
            }
        }

        if (closest == null) {
            player.displayClientMessage(Component.translatable("eveportalnether.radar.none").withStyle(ChatFormatting.GRAY), true);
            return;
        }

        BlockPos target = closest.getStandPos(serverLevel.dimension());
        player.displayClientMessage(Component.translatable(
                "eveportalnether.radar.nearest",
                Component.translatable(getDirectionKey(player, target)),
                String.format("%.0f", Math.sqrt(closestDistSq))
        ).withStyle(ChatFormatting.AQUA), true);

        level.playSound(null, player.blockPosition(), ModSounds.RADAR_PING.get(), SoundSource.PLAYERS, 0.35F, 1.4F);

        Vec3 dir = new Vec3(target.getX() - player.getX(), target.getY() - player.getY(), target.getZ() - player.getZ()).normalize();
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                player.getX() + dir.x * 2,
                player.getY() + 1.5 + dir.y * 2,
                player.getZ() + dir.z * 2,
                3, 0.1, 0.1, 0.1, 0.01);
    }

    private static String getDirectionKey(Player player, BlockPos target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        double playerYaw = player.getYRot() % 360;
        if (playerYaw < 0) {
            playerYaw += 360;
        }

        double targetYaw = (angle - 90) % 360;
        if (targetYaw < 0) {
            targetYaw += 360;
        }

        double diff = (targetYaw - playerYaw + 360) % 360;
        if (diff < 22.5 || diff > 337.5) return "eveportalnether.radar.dir.ahead";
        if (diff < 67.5) return "eveportalnether.radar.dir.slight_right";
        if (diff < 112.5) return "eveportalnether.radar.dir.right";
        if (diff < 157.5) return "eveportalnether.radar.dir.behind_right";
        if (diff < 202.5) return "eveportalnether.radar.dir.behind";
        if (diff < 247.5) return "eveportalnether.radar.dir.behind_left";
        if (diff < 292.5) return "eveportalnether.radar.dir.left";
        return "eveportalnether.radar.dir.slight_left";
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
    public boolean isFoil(ItemStack stack) {
        return getRadarTicks(stack) > 0;
    }
}
