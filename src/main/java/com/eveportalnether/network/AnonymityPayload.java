package com.eveportalnether.network;

import com.eveportalnether.EvePortalNether;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AnonymityPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<AnonymityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EvePortalNether.MODID, "anonymity"));
    public static final StreamCodec<ByteBuf, AnonymityPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(AnonymityPayload::new, AnonymityPayload::enabled);

    /**
     * Last value received from the server. Read by the client name tag hook.
     */
    public static boolean clientEnabled = false;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
