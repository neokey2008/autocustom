package com.autoenchant.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

// Mixin vacío - mantiene la estructura pero sin inyecciones problemáticas
// El reset se maneja ahora dentro del EnchantmentBuyHandler automáticamente
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    // Este mixin está vacío intencionalmente
    // La funcionalidad de reset se maneja automáticamente en EnchantmentBuyHandler
    // al detectar cuando el menú se cierra
}