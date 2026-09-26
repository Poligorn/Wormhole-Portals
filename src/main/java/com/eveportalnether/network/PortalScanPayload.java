package com.eveportalnether.network;

import com.eveportalnether.EvePortalNether;
import com.eveportalnether.world.PortalColor;
import com.eveportalnether.world.PortalTier;
import com.eveportalnether.world.data.PortalPair;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent when a detector decryption starts. The client scan panel reveals the fields over {@code decryptTicks}.
 *
 * @param stage decay stage 0..3, or -1 while the portal is still forming
 */
public record PortalScanPayload(String signature, PortalTier tier, PortalColor color, int width, int height, int stage,
                                float stability, int ticksRemaining, int usesRemaining, int decryptTicks) implements CustomPacketPayload {
    public static final Type<PortalScanPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EvePortalNether.MODID, "portal_scan"));
    public static final StreamCodec<ByteBuf, PortalScanPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                FriendlyByteBuf out = new FriendlyByteBuf(buf);
                out.writeUtf(payload.signature());
                out.writeEnum(payload.tier());
                out.writeEnum(payload.color());
                out.writeVarInt(payload.width());
                out.writeVarInt(payload.height());
                out.writeVarInt(payload.stage() + 1);
                out.writeFloat(payload.stability());
                out.writeVarInt(payload.ticksRemaining());
                out.writeVarInt(payload.usesRemaining());
                out.writeVarInt(payload.decryptTicks());
            },
            buf -> {
                FriendlyByteBuf in = new FriendlyByteBuf(buf);
                return new PortalScanPayload(in.readUtf(), in.readEnum(PortalTier.class), in.readEnum(PortalColor.class),
                        in.readVarInt(), in.readVarInt(), in.readVarInt() - 1, in.readFloat(),
                        in.readVarInt(), in.readVarInt(), in.readVarInt());
            });

    public static PortalScanPayload of(PortalPair pair, int decryptTicks) {
        float stability = Math.min(100.0F, Math.max(0.0F, pair.getTicksRemaining() / (float) pair.getInitialTicks() * 100.0F));
        return new PortalScanPayload(pair.getSignature(), pair.getTier(), pair.getColor(), pair.getWidth(), pair.getHeight(),
                pair.isForming() ? -1 : pair.getDecay(), stability,
                Math.max(0, pair.getTicksRemaining()), Math.max(0, pair.getJumpsRemaining()), decryptTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
