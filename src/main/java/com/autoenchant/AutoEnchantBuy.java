package com.autoenchant;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoEnchantBuy implements ModInitializer {
    public static final String MOD_ID = "autoenchantbuy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Inicializando Auto Enchant Buy");
    }
}