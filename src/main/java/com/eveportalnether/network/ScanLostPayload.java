package com.eveportalnether.network;

import com.eveportalnether.EvePortalNether;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Decryption was interrupted: the client panel shows "signal lost" and fades out.
 */
public record ScanLostPayload() implements CustomPacketPayload {
    public static final ScanLostPayload INSTANCE = new ScanLostPayload();
    public static final Type<ScanLostPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EvePortalNether.MODID, "scan_lost"));
    public static final StreamCodec<ByteBuf, ScanLostPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
