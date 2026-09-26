package com.eveportalnether.network;

import com.eveportalnether.EvePortalNether;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ShakePayload(float strength, int duration) implements CustomPacketPayload {
    public static final Type<ShakePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EvePortalNether.MODID, "shake"));
    public static final StreamCodec<ByteBuf, ShakePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ShakePayload::strength,
            ByteBufCodecs.VAR_INT, ShakePayload::duration,
            ShakePayload::new);

    /**
     * Client-side shake state, driven by the client tick and camera hooks.
     */
    public static volatile float clientStrength;
    public static volatile int clientRemaining;
    public static volatile int clientDuration;

    public static void applyOnClient(ShakePayload payload) {
        float current = clientDuration > 0 ? clientStrength * clientRemaining / (float) clientDuration : 0.0F;
        if (payload.strength() >= current) {
            clientStrength = payload.strength();
            clientDuration = Math.max(1, payload.duration());
            clientRemaining = clientDuration;
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
