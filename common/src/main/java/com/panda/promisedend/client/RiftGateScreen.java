package com.panda.promisedend.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.network.RiftPayloads;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A Rift Gate's screen: the gates of the player's network in this level, nearest first, each with its land's mark,
 * its name, the land and how far. A click on one goes there; the server checks the journey. The gate the player
 * stands at is listed first and can't be picked.
 */
public class RiftGateScreen extends Screen {
    static final int W = 260, ROW_H = 22, ROWS = 7, TOP = 28;
    // the shrine screen's stone and ink
    static final int STONE = 0xF0161220, EDGE = 0xFF6E4A6E, ROW = 0x40A97CA9, ROW_HOVER = 0x80A97CA9, ROW_HERE = 0x20FFFFFF,
            TEXT = 0xFFE8DDF2, TEXT_MUTED = 0xFFA898B8;

    private final BlockPos here;
    private final List<RiftPayloads.Shown> gates;
    private int x0, y0, h, scroll;

    public RiftGateScreen(BlockPos here, List<RiftPayloads.Shown> gates) {
        super(Component.translatable("screen.promisedend.rift_gate.title"));
        this.here = here;
        List<RiftPayloads.Shown> sorted = new ArrayList<>(gates);
        sorted.sort(Comparator.comparingDouble((RiftPayloads.Shown g) -> g.pos().equals(here) ? -1 : g.pos().distSqr(here)));
        this.gates = List.copyOf(sorted);
    }

    /** The payload's way in. */
    public static void open(RiftPayloads.Gates payload) {
        Minecraft.getInstance().gui.setScreen(new RiftGateScreen(payload.here(), payload.gates()));
    }

    public List<RiftPayloads.Shown> gates() {
        return gates;
    }

    @Override
    protected void init() {
        h = TOP + Math.max(1, Math.min(ROWS, gates.size())) * ROW_H + 22;
        x0 = (width - W) / 2;
        y0 = (height - h) / 2;
    }

    /** A land's mark: a colour of its own, from the lands' palette (a pack's land gets one from its name). */
    static int mark(String biome) {
        return switch (Identifier.parse(biome).getPath()) {
            case "shattered_rim" -> 0xFF5F4D6B;
            case "chorus_thicket" -> 0xFF8A5C8C;
            case "chorus_canopy" -> 0xFFB07AB4;
            case "lumen_mire" -> 0xFFE8D27A;
            case "resonant_spires" -> 0xFF5FD6C8;
            case "hollow_highlands" -> 0xFF8E7B93;
            case "shulker_reefs" -> 0xFFA97CA9;
            case "void_fungus_barrens" -> 0xFF4FB09A;
            case "voidscar_wastes" -> 0xFF7B5CFF;
            case "skyfall_archipelago" -> 0xFFC8B4FF;
            case "bloom_meadows" -> 0xFFD88AC0;
            case "hanging_gardens" -> 0xFF7FB07A;
            default -> 0xFF000000 | (biome.hashCode() & 0x7F7F7F) + 0x404040;
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractRenderState(g, mx, my, partial);
        g.fill(x0, y0, x0 + W, y0 + h, STONE);
        g.fill(x0, y0, x0 + W, y0 + 1, EDGE);
        g.fill(x0, y0 + h - 1, x0 + W, y0 + h, EDGE);
        g.fill(x0, y0, x0 + 1, y0 + h, EDGE);
        g.fill(x0 + W - 1, y0, x0 + W, y0 + h, EDGE);
        g.text(font, title, x0 + (W - font.width(title)) / 2, y0 + 10, TEXT, false);
        var player = Minecraft.getInstance().player;
        for (int r = 0; r < Math.min(ROWS, gates.size() - scroll); r++) {
            var gate = gates.get(scroll + r);
            int rx = x0 + 10, ry = y0 + TOP + r * ROW_H;
            boolean at = gate.pos().equals(here);
            g.fill(rx, ry, x0 + W - 10, ry + ROW_H - 2, at ? ROW_HERE : row(mx, my) == scroll + r ? ROW_HOVER : ROW);
            g.fill(rx + 5, ry + 5, rx + 15, ry + 15, mark(gate.biome()));
            g.fill(rx + 5, ry + 5, rx + 15, ry + 6, 0x40FFFFFF);
            // a gate not yet renamed is called by its land; a renamed one has its land under the name
            var id = Identifier.parse(gate.biome());
            Component land = Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
            boolean named = !gate.name().isEmpty();
            String name = named ? gate.name() : Component.translatable("message.promisedend.rift_gate.name", land).getString();
            int room = W - 20 - 22 - 60;
            if (font.width(name) > room) name = font.plainSubstrByWidth(name, room - font.width("...")) + "...";
            g.text(font, Component.literal(name), rx + 22, named ? ry + 2 : ry + 6, at ? TEXT_MUTED : TEXT, false);
            if (named) g.text(font, land, rx + 22, ry + 11, TEXT_MUTED, false);
            Component how = at ? Component.translatable("screen.promisedend.rift_gate.here")
                    : Component.translatable("screen.promisedend.rift_gate.distance", player == null ? 0 : Math.round(Math.sqrt(player.blockPosition().distSqr(gate.pos()))));
            g.text(font, how, x0 + W - 16 - font.width(how), ry + 6, TEXT_MUTED, false);
        }
        Component foot = gates.size() > ROWS ? Component.translatable("screen.promisedend.rift_gate.scroll", gates.size())
                : gates.size() <= 1 ? Component.translatable("screen.promisedend.rift_gate.alone") : Component.translatable("screen.promisedend.rift_gate.footer");
        g.text(font, foot, x0 + (W - font.width(foot)) / 2, y0 + h - 14, TEXT_MUTED, false);
    }

    private int row(double mx, double my) {
        if (mx < x0 + 10 || mx >= x0 + W - 10) return -1;
        int r = (int) Math.floor((my - y0 - TOP) / ROW_H);
        if (my < y0 + TOP || r >= ROWS || scroll + r >= gates.size()) return -1;
        return scroll + r;
    }

    /** Goes to the gate at index {@code i}. Public for the harness, which picks as a click would. */
    public void choose(int i) {
        if (i < 0 || i >= gates.size() || gates.get(i).pos().equals(here)) return;
        PromisedEnd.toServer.accept(new RiftPayloads.Travel(here, gates.get(i).pos()));
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
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll = Math.max(0, Math.min(Math.max(0, gates.size() - ROWS), scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
