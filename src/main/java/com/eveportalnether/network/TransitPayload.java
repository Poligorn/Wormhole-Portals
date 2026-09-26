package com.eveportalnether.network;

import com.eveportalnether.EvePortalNether;
import com.eveportalnether.world.PortalColor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent right before a wormhole jump so the client can paint the dimension loading screen in the portal's colour.
 */
public record TransitPayload(PortalColor color, String signature) implements CustomPacketPayload {
    public static final Type<TransitPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EvePortalNether.MODID, "transit"));
    public static final StreamCodec<ByteBuf, TransitPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                FriendlyByteBuf out = new FriendlyByteBuf(buf);
                out.writeEnum(payload.color());
                out.writeUtf(payload.signature());
            },
            buf -> {
                FriendlyByteBuf in = new FriendlyByteBuf(buf);
                return new TransitPayload(in.readEnum(PortalColor.class), in.readUtf());
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
