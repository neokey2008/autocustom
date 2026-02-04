package com.autoenchant;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Pantalla de configuración del mod.
 *
 * Layout (panel centrado 440×270)
 * ┌─────────────────────────────────────────────┐
 * │           AUTO ENCHANT BUY                  │  ← título
 * │  ● [Activado]      [XP ████░░  32 / 40]    │  ← fila estado
 * │                                             │
 * │  ┌────────┐ ┌────────┐ ┌────────┐ ...      │  ← cards de tier
 * │  │  🗡️   │ │  🗡️   │ │  🗡️   │          │     icono + nombre + costo
 * │  │ Simple │ │ Único  │ │ Elite  │          │     borde brillante = seleccionado
 * │  │ 20 lvl │ │ 25 lvl │ │ 30 lvl │          │
 * │  └────────┘ └────────┘ └────────┘          │
 * │                                             │
 * │              [ Cerrar ]                     │  ← botón cerrar
 * └─────────────────────────────────────────────┘
 *
 * Paleta colores (tema oscuro, compatible con Minecraft)
 *   panel bg        #1a1a2e     card seleccionada  #0f3460
 *   card bg         #16213e     borde glow         #e94560
 *   texto primario  #eaeaea     texto secundario   #8892a4
 *   barra XP llena  #f5a623     barra XP vacía     #2c3e50
 *   toggle on       #2ecc71     toggle off         #e74c3c
 *
 * Usa ctx.drawBorder() nativo de DrawContext (disponible desde 1.21.x)
 * en lugar de una implementación manual.
 */
public class ConfigScreen extends Screen {

    // ── colores (ARGB) ────────────────────────────────────────────────
    private static final int C_BG_PANEL      = 0xE0_1A1A2E;
    private static final int C_BG_CARD       = 0xD0_16213E;
    private static final int C_CARD_SELECTED = 0xD0_0F3460;
    private static final int C_GLOW          = 0xFF_E94560;
    private static final int C_TEXT_PRI      = 0xFF_EAEAEA;
    private static final int C_TEXT_SEC      = 0xFF_8892A4;
    private static final int C_XP_FULL       = 0xFF_F5A623;
    private static final int C_XP_EMPTY      = 0xFF_2C3E50;
    private static final int C_TOGGLE_ON     = 0xFF_2ECC71;
    private static final int C_TOGGLE_OFF    = 0xFF_E74C3C;
    private static final int C_BORDER        = 0x60_E94560;

    // ── layout ────────────────────────────────────────────────────────
    private static final int PANEL_W  = 440;
    private static final int PANEL_H  = 270;
    private static final int CARD_W   = 76;
    private static final int CARD_H   = 68;
    private static final int CARD_GAP = 8;

    // ── campos ────────────────────────────────────────────────────────
    private final Screen parent;
    private final Config  config;
    private int px, py;   // esquina superior-izquierda del panel

    public ConfigScreen(Screen parent, Config config) {
        super(Text.literal("Auto Enchant Buy"));
        this.parent = parent;
        this.config = config;
    }

    // ── init ──────────────────────────────────────────────────────────
    @Override
    protected void init() {
        super.init();

        px = this.width  / 2 - PANEL_W / 2;
        py = this.height / 2 - PANEL_H / 2;

        // Toggle activado / desactivado
        this.addDrawableChild(ButtonWidget.builder(
                        getToggleText(),
                        btn -> {
                            config.setEnabled(!config.isEnabled());
                            btn.setMessage(getToggleText());
                        })
                .dimensions(px + 16, py + 52, 140, 22)
                .build());

        // Cards de cada tier (botones invisibles sobre las tarjetas)
        EnchantmentType[] types = EnchantmentType.values();
        int totalCardsW = types.length * CARD_W + (types.length - 1) * CARD_GAP;
        int cardsStartX = px + (PANEL_W - totalCardsW) / 2;
        int cardsY      = py + 120;

        for (int i = 0; i < types.length; i++) {
            final EnchantmentType t = types[i];
            int cx = cardsStartX + i * (CARD_W + CARD_GAP);

            ButtonWidget cardBtn = ButtonWidget.builder(
                            Text.empty(),
                            btn -> config.setSelectedType(t))
                    .dimensions(cx, cardsY, CARD_W, CARD_H)
                    .build();
            cardBtn.setAlpha(0f);   // invisible — nosotros dibujamos la card
            this.addDrawableChild(cardBtn);
        }

        // Botón cerrar
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Cerrar"),
                        btn -> this.close())
                .dimensions(this.width / 2 - 60, py + PANEL_H - 44, 120, 24)
                .build());
    }

    // ── render ────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // fondo oscurado
        ctx.fill(0, 0, this.width, this.height, 0x80_000000);

        // panel
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, C_BG_PANEL);
        ctx.drawBorder(px, py, PANEL_W, PANEL_H, C_BORDER);

        // título
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("AUTO ENCHANT BUY"), this.width / 2, py + 16, C_TEXT_PRI);

        // punto de estado (verde/rojo junto al toggle)
        int dotColor = config.isEnabled() ? C_TOGGLE_ON : C_TOGGLE_OFF;
        ctx.fill(px + 18, py + 57, px + 26, py + 65, dotColor);

        // barra XP
        drawXpBar(ctx);

        // cards de tier
        drawTierCards(ctx, mouseX, mouseY);

        // widgets (botones)
        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── barra XP ──────────────────────────────────────────────────────
    private void drawXpBar(DrawContext ctx) {
        if (this.client == null || this.client.player == null) return;

        int playerXp   = this.client.player.experienceLevel;
        int requiredXp = config.getSelectedType().getXpCost();

        int barX = px + 180;
        int barY = py + 55;
        int barW = 220;
        int barH = 12;

        // fondo vacío
        ctx.fill(barX, barY, barX + barW, barY + barH, C_XP_EMPTY);

        // porción llena (clampeada a 100 %)
        float ratio = Math.min((float) playerXp / requiredXp, 1.0f);
        int   fillW = (int) (barW * ratio);
        if (fillW > 0) {
            ctx.fill(barX, barY, barX + fillW, barY + barH, C_XP_FULL);
        }

        // borde de la barra
        ctx.drawBorder(barX, barY, barW, barH, 0xFF_F5A623);

        // etiqueta "XP  32 / 40"
        String label  = "XP  " + playerXp + " / " + requiredXp;
        int    labelX = barX + barW + 8;
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(label), labelX, barY + 1,
                playerXp >= requiredXp ? C_TOGGLE_ON : C_TEXT_SEC);
    }

    // ── cards de tier ─────────────────────────────────────────────────
    private void drawTierCards(DrawContext ctx, int mouseX, int mouseY) {
        EnchantmentType[] types       = EnchantmentType.values();
        EnchantmentType   selected    = config.getSelectedType();
        int totalCardsW = types.length * CARD_W + (types.length - 1) * CARD_GAP;
        int cardsStartX = px + (PANEL_W - totalCardsW) / 2;
        int cardsY      = py + 120;

        for (int i = 0; i < types.length; i++) {
            EnchantmentType t  = types[i];
            int             cx = cardsStartX + i * (CARD_W + CARD_GAP);
            boolean         sel = (t == selected);
            boolean         hov = mouseX >= cx && mouseX < cx + CARD_W
                               && mouseY >= cardsY && mouseY < cardsY + CARD_H;

            // fondo de la card
            int bgCol = sel ? C_CARD_SELECTED : (hov ? 0xD0_1a2a4a : C_BG_CARD);
            ctx.fill(cx, cardsY, cx + CARD_W, cardsY + CARD_H, bgCol);

            // borde
            if (sel) {
                ctx.drawBorder(cx, cardsY, CARD_W, CARD_H, C_GLOW);
                // glow exterior (1 px fuera)
                ctx.drawBorder(cx - 1, cardsY - 1, CARD_W + 2, CARD_H + 2, 0x40_E94560);
            } else {
                ctx.drawBorder(cx, cardsY, CARD_W, CARD_H, 0x40_FFFFFF);
            }

            // ícono del item representativo del tier
            ItemStack icon = new ItemStack(t.getIconItem());
            ctx.drawItem(icon, cx + (CARD_W - 16) / 2, cardsY + 6);

            // nombre del tier
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(t.getDisplayName()),
                    cx + CARD_W / 2,
                    cardsY + 30,
                    sel ? 0xFF_FFFFFF : C_TEXT_PRI);

            // costo en XP
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§e" + t.getXpCost() + " lvl"),
                    cx + CARD_W / 2,
                    cardsY + 44,
                    C_TEXT_SEC);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────
    private Text getToggleText() {
        return config.isEnabled()
                ? Text.literal("  Activado")
                : Text.literal("  Desactivado");
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }
}