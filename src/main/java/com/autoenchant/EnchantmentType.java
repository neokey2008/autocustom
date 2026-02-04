package com.autoenchant;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

public enum EnchantmentType {
    // Definición de Tiers con su respectivo Item de ícono para la GUI
    SIMPLE(20, 11, "Simple", "§7Simple", 0xFF808080, Items.WHITE_STAINED_GLASS_PANE),
    UNICO(25, 12, "Único", "§aÚnico", 0xFF00FF00, Items.LIME_STAINED_GLASS_PANE),
    ELITE(30, 13, "Elite", "§bElite", 0xFF00FFFF, Items.LIGHT_BLUE_STAINED_GLASS_PANE),
    ULTIMATE(35, 14, "Ultimate", "§eUltimate", 0xFFFFFF00, Items.YELLOW_STAINED_GLASS_PANE),
    LEGENDARIO(40, 15, "Legendario", "§6Legendario", 0xFFFF8800, Items.ORANGE_STAINED_GLASS_PANE);

    private final int xpCost;
    private final int slotIndex;
    private final String displayName;
    private final String coloredName;
    private final int color;
    private final Item iconItem; // Campo faltante que pedía tu ConfigScreen

    EnchantmentType(int xpCost, int slotIndex, String displayName, String coloredName, int color, Item iconItem) {
        this.xpCost = xpCost;
        this.slotIndex = slotIndex;
        this.displayName = displayName;
        this.coloredName = coloredName;
        this.color = color;
        this.iconItem = iconItem;
    }

    // --- Getters ---
    public int getXpCost() { return xpCost; }
    public int getSlotIndex() { return slotIndex; }
    public String getDisplayName() { return displayName; }
    public String getColoredName() { return coloredName; }
    public int getColor() { return color; }
    
    /**
     * Devuelve el ítem que se dibujará en la card de la interfaz.
     */
    public Item getIconItem() {
        return iconItem;
    }

    // --- Navegación ---
    public EnchantmentType next() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public EnchantmentType previous() {
        return values()[(this.ordinal() - 1 + values().length) % values().length];
    }
}