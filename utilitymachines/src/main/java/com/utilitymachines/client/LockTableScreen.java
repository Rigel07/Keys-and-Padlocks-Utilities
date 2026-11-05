package com.utilitymachines.client;

import com.utilitymachines.menu.LockTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Pantalla de la mesa de cerradura (dibujada por código, sin textura propia). */
public class LockTableScreen extends AbstractContainerScreen<LockTableMenu> {
    public LockTableScreen(LockTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.utilitymachines.new_set"),
                b -> press(LockTableMenu.BTN_NEW)).bounds(leftPos + 58, topPos + 14, 68, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.utilitymachines.copy_key"),
                b -> press(LockTableMenu.BTN_COPY)).bounds(leftPos + 58, topPos + 38, 68, 20).build());
    }

    private void press(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF000000);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
            g.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        centered(g, "gui.utilitymachines.material", 38, 35);
        centered(g, "gui.utilitymachines.key", 38, 59);
        centered(g, "gui.utilitymachines.key", 142, 35);
        centered(g, "gui.utilitymachines.padlock", 142, 59);
    }

    private void centered(GuiGraphics g, String key, int centerX, int y) {
        Component text = Component.translatable(key);
        g.drawString(font, text, centerX - font.width(text) / 2, y, 0x404040, false);
    }
}
