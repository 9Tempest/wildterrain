package dev.lukez.wildterrain.client.screen;

import dev.lukez.wildterrain.core.ModItems;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class MossquillGuideScreen extends Screen {
    private static final Component TITLE = Component.translatable("guide.wildterrain.mossquill.title");
    private static final Component SUBTITLE = Component.translatable("guide.wildterrain.mossquill.subtitle");
    private static final Component HABITAT = Component.translatable("guide.wildterrain.mossquill.habitat");
    private static final Component BEHAVIOR = Component.translatable("guide.wildterrain.mossquill.behavior");
    private static final Component ECOLOGY = Component.translatable("guide.wildterrain.mossquill.ecology");
    private static final Component ANIMATION = Component.translatable("guide.wildterrain.mossquill.animation");

    public MossquillGuideScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(x + panelWidth - 76, y + panelHeight - 26, 62, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        renderPanel(guiGraphics, x, y, panelWidth, panelHeight, partialTick);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderPanel(GuiGraphics guiGraphics, int x, int y, int panelWidth, int panelHeight, float partialTick) {
        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, 0xF21C2319);
        guiGraphics.fill(x + 2, y + 2, x + panelWidth - 2, y + panelHeight - 2, 0xF22B3825);
        guiGraphics.fill(x + 6, y + 6, x + panelWidth - 6, y + 34, 0xFF496B3B);
        guiGraphics.fill(x + 6, y + 34, x + panelWidth - 6, y + 36, 0xFFD5E889);

        drawMossPulse(guiGraphics, x + panelWidth - 54, y + 11, partialTick);
        guiGraphics.renderItem(new ItemStack(ModItems.MOSSQUILL_FIELD_GUIDE.get()), x + 14, y + 12);
        guiGraphics.renderItem(new ItemStack(ModItems.MOSSQUILL_SPAWN_EGG.get()), x + panelWidth - 28, y + 12);

        guiGraphics.drawCenteredString(font, TITLE, x + panelWidth / 2, y + 12, 0xFFEAF7D2);
        guiGraphics.drawCenteredString(font, SUBTITLE, x + panelWidth / 2, y + 23, 0xFFBFD998);

        int contentX = x + 16;
        int contentWidth = panelWidth - 32;
        int lineY = y + 48;
        lineY = drawSection(guiGraphics, Component.translatable("guide.wildterrain.label.habitat"),
                HABITAT, contentX, lineY, contentWidth, 0xFF8FB66F);
        lineY = drawSection(guiGraphics, Component.translatable("guide.wildterrain.label.behavior"),
                BEHAVIOR, contentX, lineY + 4, contentWidth, 0xFFD5E889);
        lineY = drawSection(guiGraphics, Component.translatable("guide.wildterrain.label.ecology"),
                ECOLOGY, contentX, lineY + 4, contentWidth, 0xFF83BFC3);
        drawSection(guiGraphics, Component.translatable("guide.wildterrain.label.animation"),
                ANIMATION, contentX, lineY + 4, contentWidth - 70, 0xFFE9C46A);
    }

    private int drawSection(GuiGraphics guiGraphics, Component label, Component body,
                            int x, int y, int width, int accentColor) {
        guiGraphics.fill(x, y, x + 4, y + 8, accentColor);
        guiGraphics.drawString(font, label, x + 8, y, accentColor, false);

        int textY = y + 12;
        List<FormattedCharSequence> lines = font.split(body, width);
        int lineCount = Math.min(lines.size(), 3);
        for (int i = 0; i < lineCount; i++) {
            guiGraphics.drawString(font, lines.get(i), x + 8, textY, 0xFFE7E2C2, false);
            textY += 10;
        }
        return textY;
    }

    private void drawMossPulse(GuiGraphics guiGraphics, int x, int y, float partialTick) {
        float time = (minecraft == null || minecraft.level == null ? 0.0F : minecraft.level.getGameTime()) + partialTick;
        for (int i = 0; i < 4; i++) {
            float wave = (Mth.sin(time * 0.18F + i * 1.7F) + 1.0F) * 0.5F;
            int height = 3 + Mth.floor(wave * 8.0F);
            int color = i % 2 == 0 ? 0xFFD5E889 : 0xFF83B66F;
            guiGraphics.fill(x + i * 7, y + 12 - height, x + i * 7 + 4, y + 12, color);
        }
    }

    private int panelWidth() {
        return Mth.clamp(width - 28, 280, 360);
    }

    private int panelHeight() {
        return Mth.clamp(height - 28, 206, 242);
    }
}
