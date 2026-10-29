package com.panda.promisedend.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.logic.Offerings;
import com.panda.promisedend.network.ShrinePayloads;
import com.panda.promisedend.registry.ModEffects;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * A Shrine Altar's offer: the three boons it holds out to this player, each with its icon, its name and what it does.
 * A click (or 1, 2, 3) takes one; the server checks it was offered. Closing the screen loses nothing: the altar keeps
 * the offer, and an empty-handed use opens it again.
 */
public class ShrineScreen extends Screen {
    static final int W = 260, H = 150, ROW_Y = 30, ROW_H = 32, ROW_X = 10;
    // the Voidstone and purpur ramps: a dark shrine stone with lilac ink
    static final int STONE = 0xF0161220, EDGE = 0xFF6E4A6E, ROW = 0x40A97CA9, ROW_HOVER = 0x80A97CA9, TEXT = 0xFFE8DDF2, TEXT_MUTED = 0xFFA898B8;

    private final BlockPos altar;
    private final List<Integer> boons;
    private int x0, y0;

    public ShrineScreen(BlockPos altar, List<Integer> boons) {
        super(Component.translatable("screen.promisedend.shrine.title"));
        this.altar = altar;
        this.boons = List.copyOf(boons);
    }

    /** The payload's way in: opens on the offer, or closes this altar's screen when the offer is spent. */
    public static void open(ShrinePayloads.Offer offer) {
        Minecraft mc = Minecraft.getInstance();
        if (offer.boons().isEmpty()) {
            if (mc.gui.screen() instanceof ShrineScreen) mc.gui.setScreen(null);
            return;
        }
        mc.gui.setScreen(new ShrineScreen(offer.altar(), offer.boons()));
    }

    /** An offering's tooltip line: what it is worth at a Shrine Altar (read from the favour tags). */
    public static void offeringLine(net.minecraft.world.item.ItemStack stack, List<Component> lines) {
        int worth = com.panda.promisedend.block.ShrineAltarBlockEntity.worth(stack);
        if (worth > 0) lines.add(Component.translatable("tooltip.promisedend.offering", worth).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
    }

    public List<Integer> boons() {
        return boons;
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractRenderState(g, mx, my, partial);
        g.fill(x0, y0, x0 + W, y0 + H, STONE);
        g.fill(x0, y0, x0 + W, y0 + 1, EDGE);
        g.fill(x0, y0 + H - 1, x0 + W, y0 + H, EDGE);
        g.fill(x0, y0, x0 + 1, y0 + H, EDGE);
        g.fill(x0 + W - 1, y0, x0 + W, y0 + H, EDGE);
        g.text(font, title, x0 + (W - font.width(title)) / 2, y0 + 10, TEXT, false);
        for (int i = 0; i < boons.size(); i++) {
            int rx = x0 + ROW_X, ry = y0 + ROW_Y + i * (ROW_H + 4);
            boolean hover = row(mx, my) == i;
            g.fill(rx, ry, rx + W - 2 * ROW_X, ry + ROW_H, hover ? ROW_HOVER : ROW);
            var effect = ModEffects.boon(boons.get(i));
            g.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect), rx + 7, ry + 7, 18, 18);
            String name = Offerings.BOONS[boons.get(i)];
            g.text(font, Component.literal((i + 1) + "  ").append(effect.value().getDisplayName()), rx + 32, ry + 6, TEXT, false);
            int ticks = name.equals("void_sight") ? com.panda.promisedend.logic.ShrineBoons.VOID_SIGHT_DURATION : com.panda.promisedend.logic.ShrineBoons.DURATION;
            Component time = Component.translatable("screen.promisedend.shrine.minutes", ticks / 1200);
            g.text(font, time, rx + W - 2 * ROW_X - 6 - font.width(time), ry + 6, TEXT_MUTED, false);
            Component what = Component.translatable("effect." + PromisedEnd.MODID + "." + name + ".description");
            // a pack's longer line is cut to the row rather than run past it
            int room = W - 2 * ROW_X - 32 - 6;
            if (font.width(what) > room) what = Component.literal(font.plainSubstrByWidth(what.getString(), room - font.width("...")) + "...");
            g.text(font, what, rx + 32, ry + 18, TEXT_MUTED, false);
        }
        Component foot = Component.translatable("screen.promisedend.shrine.footer");
        g.text(font, foot, x0 + (W - font.width(foot)) / 2, y0 + H - 14, TEXT_MUTED, false);
    }

    private int row(double mx, double my) {
        for (int i = 0; i < boons.size(); i++) {
            int rx = x0 + ROW_X, ry = y0 + ROW_Y + i * (ROW_H + 4);
            if (mx >= rx && mx < x0 + W - ROW_X && my >= ry && my < ry + ROW_H) return i;
        }
        return -1;
    }

    /** Takes the boon in row {@code i}. Public for the harness, which picks as a click would. */
    public void choose(int i) {
        if (i < 0 || i >= boons.size()) return;
        PromisedEnd.toServer.accept(new ShrinePayloads.Choose(altar, boons.get(i)));
        onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int i = row(event.x(), event.y());
            if (i >= 0) {
                choose(i);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key >= InputConstants.KEY_1 && key < InputConstants.KEY_1 + boons.size()) {
            choose(key - InputConstants.KEY_1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
