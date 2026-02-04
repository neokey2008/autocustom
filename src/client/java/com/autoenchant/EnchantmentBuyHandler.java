package com.autoenchant;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

/**
 * Finite-state machine that drives the auto-buy loop.
 *
 * States
 * ------
 *   IDLE            – doing nothing; watches XP and triggers a buy when ready.
 *   WAITING_FOR_MENU– command has been sent; waiting for the
 *                      GenericContainerScreenHandler to appear.
 *   CLICKING        – menu is open; performs the slot-click exactly once.
 *   CLOSING         – click done; waits one tick for the screen to close,
 *                      then returns to IDLE.
 *
 * Why this fixes the double-click
 * --------------------------------
 * The old code used a trio of booleans (isProcessing / waitingForMenu /
 * hasPurchased) that could be cleared by the "menu-closed" detector between
 * the time the command was sent and the menu actually appeared.  During that
 * window startPurchase() could fire a second time.
 *
 * The FSM never leaves WAITING_FOR_MENU until the menu actually appears, so
 * a second command is never sent.  A hard cooldown of COOLDOWN_MS is also
 * enforced after every successful purchase as an extra safety net.
 */
public class EnchantmentBuyHandler {

    // ── tunables ──────────────────────────────────────────────────────
    /** ms to wait after sending the command before we time-out. */
    private static final long MENU_TIMEOUT_MS  = 3_000;
    /** ms to wait after the menu opens before clicking (lets the server
     *  populate the slots; 1 tick ≈ 50 ms is usually enough). */
    private static final long CLICK_DELAY_MS   =    60;
    /** Hard cooldown between purchases – prevents any possibility of
     *  back-to-back buys even if the player is swimming in XP.         */
    private static final long COOLDOWN_MS      = 1_200;

    // ── state ─────────────────────────────────────────────────────────
    private enum State { IDLE, WAITING_FOR_MENU, CLICKING, CLOSING }

    private static State   state        = State.IDLE;
    private static long    stateEnteredAt = 0;         // System.currentTimeMillis() when we entered the current state
    private static long    lastPurchaseAt = 0;         // timestamp of the last successful purchase

    // ── public entry ──────────────────────────────────────────────────
    public static void tick(MinecraftClient client) {
        if (client.player == null || !AutoEnchantBuyClient.getConfig().isEnabled()) {
            state = State.IDLE;
            return;
        }

        switch (state) {
            case IDLE            -> tickIdle(client);
            case WAITING_FOR_MENU-> tickWaiting(client);
            case CLICKING        -> tickClicking(client);
            case CLOSING         -> tickClosing(client);
        }
    }

    // ── IDLE ──────────────────────────────────────────────────────────
    private static void tickIdle(MinecraftClient client) {
        long now = System.currentTimeMillis();

        // respect cooldown
        if (now - lastPurchaseAt < COOLDOWN_MS) return;

        EnchantmentType type = AutoEnchantBuyClient.getConfig().getSelectedType();
        ClientPlayerEntity player = client.player;

        if (player.experienceLevel >= type.getXpCost()) {
            // ── transition → WAITING_FOR_MENU ──
            state          = State.WAITING_FOR_MENU;
            stateEnteredAt = now;

            player.networkHandler.sendChatMessage(VersionHelper.enchantmentMenuCommand());
            AutoEnchantBuy.LOGGER.info("[AEB] Comando enviado para comprar: {}", type.getDisplayName());
        }
    }

    // ── WAITING_FOR_MENU ──────────────────────────────────────────────
    private static void tickWaiting(MinecraftClient client) {
        ScreenHandler handler = client.player.currentScreenHandler;

        if (handler instanceof GenericContainerScreenHandler) {
            // menu appeared – move to CLICKING and start the click-delay timer
            state          = State.CLICKING;
            stateEnteredAt = System.currentTimeMillis();
            return;
        }

        // timeout guard
        if (System.currentTimeMillis() - stateEnteredAt > MENU_TIMEOUT_MS) {
            AutoEnchantBuy.LOGGER.warn("[AEB] Timeout esperando el menú de encantamientos.");
            client.player.sendMessage(
                    Text.literal("§7[§6AutoEnchant§7] §cError: no se pudo abrir el menú."), false);
            state = State.IDLE;
        }
    }

    // ── CLICKING ──────────────────────────────────────────────────────
    private static void tickClicking(MinecraftClient client) {
        // wait for the short click-delay
        if (System.currentTimeMillis() - stateEnteredAt < CLICK_DELAY_MS) return;

        ScreenHandler handler = client.player.currentScreenHandler;
        if (!(handler instanceof GenericContainerScreenHandler)) {
            // menu vanished before we could click – bail
            AutoEnchantBuy.LOGGER.warn("[AEB] El menú desapareció antes del click.");
            state = State.IDLE;
            return;
        }

        EnchantmentType type = AutoEnchantBuyClient.getConfig().getSelectedType();

        // ── perform the single click ──
        if (client.interactionManager != null) {
            client.interactionManager.clickSlot(
                    handler.syncId,
                    type.getSlotIndex(),
                    0,
                    SlotActionType.PICKUP,
                    client.player
            );
        }

        lastPurchaseAt = System.currentTimeMillis();
        AutoEnchantBuy.LOGGER.info("[AEB] Click en slot {}  ({})", type.getSlotIndex(), type.getDisplayName());

        // confirmation message
        client.player.sendMessage(
                Text.literal("§7[§6AutoEnchant§7] Comprado: ").append(Text.literal(type.getColoredName())), false);

        // close screen on next tick
        state          = State.CLOSING;
        stateEnteredAt = System.currentTimeMillis();
    }

    // ── CLOSING ───────────────────────────────────────────────────────
    private static void tickClosing(MinecraftClient client) {
        // give the server one tick to process the click, then close
        if (System.currentTimeMillis() - stateEnteredAt < 50) return;

        if (client.player != null) {
            client.player.closeHandledScreen();
        }
        state = State.IDLE;
    }

    // ── util ──────────────────────────────────────────────────────────
    /** Hard-reset (call on disconnect / mod disable). */
    public static void reset() {
        state          = State.IDLE;
        stateEnteredAt = 0;
    }
}