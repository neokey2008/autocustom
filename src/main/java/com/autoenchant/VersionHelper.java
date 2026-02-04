package com.autoenchant;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Versión de Minecraft detectada en runtime sin usar ninguna API interna
 * de MC (GameVersion / MinecraftVersion cambian entre snapshots y no son
 * estables en yarn 1.21.8).
 *
 * Estrategia: leer la versión desde el ModContainer "minecraft"
 * usando la API pública de FabricLoader (ModMetadata.getVersion()).
 * Como fallback usa la versión hardcodeada en compilación.
 */
public final class VersionHelper {

    // ── versión fallback (se usa si FabricLoader no puede resolverla) ──
    // Este valor viene de gradle.properties → minecraft_version
    private static final String FALLBACK_VERSION = "1.21.8";

    // ── cache lazy ────────────────────────────────────────────────────
    private static int[] MC_VERSION;

    /**
     * Resuelve y cachea [major, minor, patch].
     * Usa FabricLoader para buscar el mod "minecraft" y leer su versión.
     * Si por cualquier razón falla, cae al FALLBACK.
     */
    private static int[] version() {
        if (MC_VERSION != null) return MC_VERSION;

        String raw = FALLBACK_VERSION;   // default

        try {
            // getModContainer() retorna Optional<ModContainer>.
            // ModContainer.getMetadata().getVersion().getFriendlyString()
            // es la API estable y pública de Fabric Loader 0.16.x.
            var container = FabricLoader.getInstance().getModContainer("minecraft");
            if (container.isPresent()) {
                raw = container.get().getMetadata().getVersion().getFriendlyString();
            }
        } catch (Exception e) {
            AutoEnchantBuy.LOGGER.warn("[AEB] No se pudo detectar versión de MC, usando fallback: {}", FALLBACK_VERSION);
        }

        String[] parts = raw.split("[._-]");
        MC_VERSION = new int[3];
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            try {
                MC_VERSION[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ignored) { /* deja en 0 */ }
        }
        return MC_VERSION;
    }

    private VersionHelper() {}

    // ── queries ───────────────────────────────────────────────────────

    /** True cuando se ejecuta en 1.21.2 o posterior (dentro de la familia 1.21). */
    public static boolean is1_21_2OrLater() {
        int[] v = version();
        return v[0] == 1 && v[1] == 21 && v[2] >= 2;
    }

    /** True cuando se ejecuta en exactamente 1.21.0 o 1.21.1. */
    public static boolean is1_21_0Or1() {
        int[] v = version();
        return v[0] == 1 && v[1] == 21 && v[2] <= 1;
    }

    /** Retorna "major.minor.patch" para logging. */
    public static String versionString() {
        int[] v = version();
        return v[0] + "." + v[1] + "." + v[2];
    }

    /**
     * Comando de chat para abrir el menú de encantamientos.
     * Si en el futuro difiere entre versiones, hacer el branch aquí.
     */
    public static String enchantmentMenuCommand() {
        return "/encantamientos";
    }
}