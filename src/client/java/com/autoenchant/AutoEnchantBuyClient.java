package com.autoenchant;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AutoEnchantBuyClient implements ClientModInitializer {
    private static Config     config;
    private static KeyBinding configKeyBinding;

    @Override
    public void onInitializeClient() {
        config = Config.load();
        AutoEnchantBuy.LOGGER.info("[AEB] Cliente inicializado  (MC {})", VersionHelper.versionString());

        // ── keybinding ────────────────────────────────────────────────
        configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoenchantbuy.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.autoenchantbuy"
        ));

        // ── tick loop ─────────────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // open config screen
            while (configKeyBinding.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ConfigScreen(null, config));
                }
            }

            // run the buy FSM
            EnchantmentBuyHandler.tick(client);
        });
    }

    public static Config getConfig() { return config; }
}