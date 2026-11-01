package com.cerbon.cerbons_api.neoforge.network;

import com.cerbon.cerbons_api.api.network.data.PacketContainer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * Registers client-side payload handlers on the client network registry
 * ({@link RegisterClientPayloadHandlersEvent}), which is mandatory for
 * clientbound payloads in NeoForge 26.1.2. This class must only be
 * instantiated on the client (it references client-only NeoForge classes).
 */
public class NeoForgeClientNetworkHandler {
    private final NeoForgeNetworkHandler handler;

    public NeoForgeClientNetworkHandler(NeoForgeNetworkHandler handler) {
        this.handler = handler;
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void register(RegisterClientPayloadHandlersEvent event) {
        if (!handler.getPacketMap().isEmpty()) {
            handler.getPacketMap().forEach((type, container) -> {
                PacketContainer pc = (PacketContainer) container;
                event.register((CustomPacketPayload.Type) pc.getType(), NeoForgeNetworkHandler.buildHandler(pc.handler()));
            });
        }
    }
}
