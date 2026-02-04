package com.autoenchant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson  GSON        = new GsonBuilder().setPrettyPrinting().create();
    private static final Path  CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("autoenchantbuy.json");

    // ── persisted fields ──────────────────────────────────────────────
    private boolean         enabled      = false;
    private String          selectedType = EnchantmentType.SIMPLE.name();   // stored as String for safe deserialisation

    // ── runtime (never written to disk) ───────────────────────────────
    private transient EnchantmentType resolvedType = null;

    // ── load ──────────────────────────────────────────────────────────
    public static Config load() {
        if (Files.exists(CONFIG_FILE)) {
            try {
                String json = Files.readString(CONFIG_FILE);
                Config loaded = GSON.fromJson(json, Config.class);
                if (loaded != null) {
                    loaded.resolveType();   // validate enum value
                    return loaded;
                }
            } catch (Exception e) {                        // catch ALL (not just IOException)
                AutoEnchantBuy.LOGGER.error("Error al cargar configuración – se usará la predeterminada", e);
            }
        }
        Config def = new Config();
        def.resolveType();
        return def;
    }

    /** Converts the stored String into the enum; falls back to SIMPLE if invalid. */
    private void resolveType() {
        try {
            resolvedType = EnchantmentType.valueOf(selectedType);
        } catch (Exception e) {
            AutoEnchantBuy.LOGGER.warn("Tipo de encantamiento inválido en config: '{}' – se reemplaza por SIMPLE", selectedType);
            resolvedType   = EnchantmentType.SIMPLE;
            selectedType   = resolvedType.name();
        }
    }

    // ── save ──────────────────────────────────────────────────────────
    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Files.writeString(CONFIG_FILE, GSON.toJson(this));
        } catch (IOException e) {
            AutoEnchantBuy.LOGGER.error("Error al guardar configuración", e);
        }
    }

    // ── getters / setters ─────────────────────────────────────────────
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public EnchantmentType getSelectedType() {
        if (resolvedType == null) resolveType();   // safety net
        return resolvedType;
    }

    public void setSelectedType(EnchantmentType type) {
        this.resolvedType  = type;
        this.selectedType  = type.name();
        save();
    }
}