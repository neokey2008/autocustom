package com.autoenchant.mixin;

import com.autoenchant.AutoEnchantBuyClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin extends Screen {
    
    protected GenericContainerScreenMixin(Text title) {
        super(title);
    }
    
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Si el mod está activo y es el menú de encantamientos, no renderizar
        if (AutoEnchantBuyClient.getConfig().isEnabled()) {
            Text title = this.getTitle();
            
            // Verificar si el título contiene "Encantamientos" o "enc" (case insensitive)
            if (title != null) {
                String titleStr = title.getString().toLowerCase();
                if (titleStr.contains("enc") || titleStr.contains("enchant")) {
                    // Solo renderizar el fondo oscuro, sin el menú
                    this.renderBackground(context, mouseX, mouseY, delta);
                    ci.cancel();
                }
            }
        }
    }
}