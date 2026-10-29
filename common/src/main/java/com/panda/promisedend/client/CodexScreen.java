package com.panda.promisedend.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.logic.CodexBook;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2i;

/**
 * The Promise Codex: an open book. The left page lists the four acts with their progress; the right page
 * shows the chosen act's goals, ticked from the client's own advancement progress. Read-only.
 * Layout constants match the page texture, textures/gui/codex.png.
 */
public class CodexScreen extends Screen {
    static final int W = 300, H = 184;
    static final int L_X = 16, L_W = 124, R_X = 160, R_W = 124;
    static final int ACT_Y = 38, ACT_H = 22;
    static final int GOAL_Y = 34, GOAL_H = 24;

    static final Identifier BG = PromisedEnd.id("textures/gui/codex.png");
    // ink on the pale pages (the Voidstone and purpur ramps)
    static final int INK = 0xFF1A1524, INK_MUTED = 0xFF4C4267, INK_RULE = 0xFFA97CA9, DONE = 0xFF6E4A6E, SEAL = 0xFF8C628C;

    private int x0, y0;
    private int selected;

    public CodexScreen() {
        super(Component.translatable("screen.promisedend.codex.title"));
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
    }

    // ---- progress, from the client's advancements ----------------------------------------------------------------

    private boolean done(Identifier id) {
        if (id == null) return true;
        var advancements = minecraft.player == null ? null : minecraft.player.connection.getAdvancements();
        if (advancements == null) return false;
        AdvancementHolder holder = advancements.get(id);
        if (holder == null) return false;
        AdvancementProgress progress = advancements.progress().get(holder);
        return progress != null && progress.isDone();
    }

    public boolean open(CodexBook.Act act) {
        return done(act.opensWith());
    }

    /** Whether the act's opening is in the game at all: an act whose guardian isn't yet has blank pages, not a seal. */
    public boolean written(CodexBook.Act act) {
        return act.opensWith() == null || minecraft.player == null || minecraft.player.connection.getAdvancements().get(act.opensWith()) != null;
    }

    public int doneCount(CodexBook.Act act) {
        int n = 0;
        for (CodexBook.Goal g : act.goals()) if (done(g.advancement())) n++;
        return n;
    }

    // ---- bounds ----------------------------------------------------------------------------------------------------

    public int[] actBounds(int i) {
        return new int[] {x0 + L_X - 2, y0 + ACT_Y + i * ACT_H, L_W + 4, ACT_H - 1};
    }

    private static boolean in(double mx, double my, int[] b) {
        return mx >= b[0] && my >= b[1] && mx < b[0] + b[2] && my < b[1] + b[3];
    }

    public void select(int act) {
        selected = Math.max(0, Math.min(CodexBook.ACTS.size() - 1, act));
    }

    // ---- drawing -----------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractBackground(g, mx, my, partial);
        g.blit(RenderPipelines.GUI_TEXTURED, BG, x0, y0, 0, 0, W, H, 512, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        super.extractRenderState(g, mx, my, partial);
        drawActs(g, mx, my);
        drawGoals(g);
        goalTooltip(g, mx, my);
    }

    public int[] goalBounds(int i) {
        return new int[] {x0 + R_X - 2, y0 + GOAL_Y + i * GOAL_H - 2, R_W + 4, GOAL_H - 2};
    }

    /** The goal's advancement description in full: the hints on the page are short on purpose. */
    private void goalTooltip(GuiGraphicsExtractor g, int mx, int my) {
        CodexBook.Act act = CodexBook.ACTS.get(selected);
        if (!open(act) || minecraft.player == null) return;
        for (int i = 0; i < act.goals().size(); i++) {
            if (!in(mx, my, goalBounds(i))) continue;
            AdvancementHolder holder = minecraft.player.connection.getAdvancements().get(act.goals().get(i).advancement());
            Component text = holder != null && holder.value().display().isPresent() ? holder.value().display().get().description()
                    : Component.translatable(act.goals().get(i).hint());
            // on the right page, just under the goal, wrapped to the page: a tooltip at the mouse flips over the act list
            // on a narrow screen
            int[] row = goalBounds(i);
            int px = x0 + R_X + 2, py = row[1] + row[3] + 4;
            ClientTooltipPositioner onPage = (sw, sh, x, y, tw, th) -> new Vector2i(Math.min(px, sw - tw - 4), Math.min(py, sh - th - 4));
            g.setTooltipForNextFrame(font, font.split(text, R_W - 10), onPage, mx, my, false);
            return;
        }
    }

    private void drawActs(GuiGraphicsExtractor g, int mx, int my) {
        int cx = x0 + L_X + L_W / 2;
        g.text(font, title, cx - font.width(title) / 2, y0 + 12, INK, false);
        Component sub = Component.translatable("screen.promisedend.codex.subtitle");
        g.text(font, sub, cx - font.width(sub) / 2, y0 + 23, INK_MUTED, false);
        rule(g, x0 + L_X, y0 + 35, L_W);
        for (int i = 0; i < CodexBook.ACTS.size(); i++) {
            CodexBook.Act act = CodexBook.ACTS.get(i);
            int[] b = actBounds(i);
            boolean open = open(act);
            if (i == selected) {
                g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], 0x30A97CA9);
                g.fill(b[0], b[1], b[0] + 2, b[1] + b[3], DONE); // a bar, so the choice isn't colour alone
            } else if (in(mx, my, b)) {
                g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], 0x18A97CA9);
            }
            Component numeral = Component.translatable("screen.promisedend.codex.act", roman(i + 1));
            g.text(font, numeral, b[0] + 6, b[1] + 3, open ? INK : INK_MUTED, false);
            g.text(font, Component.translatable(act.title()), b[0] + 6 + font.width(numeral) + 4, b[1] + 3, open ? INK : INK_MUTED, false);
            Component status;
            if (!written(act)) status = Component.translatable("screen.promisedend.codex.unwritten");
            else if (!open) status = Component.translatable("screen.promisedend.codex.sealed");
            else if (act.goals().isEmpty()) status = Component.translatable("screen.promisedend.codex.unwritten");
            else status = Component.translatable("screen.promisedend.codex.progress", doneCount(act), act.goals().size());
            g.text(font, status, b[0] + 6, b[1] + 13, open ? INK_MUTED : SEAL, false);
            if (!open && written(act)) seal(g, b[0] + b[2] - 13, b[1] + 6);
        }
        // a line of the Ancients under the acts, so the page isn't only a list
        int py = y0 + ACT_Y + CodexBook.ACTS.size() * ACT_H + 2;
        rule(g, x0 + L_X, py, L_W);
        Component prologue = Component.translatable(done(CodexBook.WARD_DOWN) ? "screen.promisedend.codex.prologue.open" : "screen.promisedend.codex.prologue")
                .withStyle(ChatFormatting.ITALIC);
        List<FormattedCharSequence> lines = font.split(prologue, L_W);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            FormattedCharSequence line = lines.get(i);
            g.text(font, line, cx - font.width(line) / 2, py + 6 + i * 10, INK_MUTED, false);
        }
    }

    private void drawGoals(GuiGraphicsExtractor g) {
        CodexBook.Act act = CodexBook.ACTS.get(selected);
        int x = x0 + R_X, y = y0 + 12;
        Component heading = Component.translatable(act.title());
        g.text(font, heading, x + (R_W - font.width(heading)) / 2, y, INK, false);
        rule(g, x, y0 + 24, R_W);
        if (!written(act)) {
            wrapped(g, Component.translatable("screen.promisedend.codex.unwritten.hint"), x, y0 + 40, INK_MUTED, 6);
            return;
        }
        if (!open(act)) {
            seal(g, x + R_W / 2 - 4, y0 + 40);
            wrapped(g, Component.translatable(act.sealedHint()), x, y0 + 56, INK_MUTED, 6);
            return;
        }
        if (act.goals().isEmpty()) {
            wrapped(g, Component.translatable("screen.promisedend.codex.unwritten.hint"), x, y0 + 40, INK_MUTED, 6);
            return;
        }
        int gy = y0 + GOAL_Y;
        int count = 0;
        for (CodexBook.Goal goal : act.goals()) {
            boolean done = done(goal.advancement());
            if (done) count++;
            box(g, x, gy + 1, done);
            g.text(font, fit(Component.translatable(goal.title()).getString(), R_W - 12), x + 12, gy, done ? INK_MUTED : INK, false);
            g.text(font, fit(Component.translatable(goal.hint()).getString(), R_W - 12), x + 12, gy + 10, INK_MUTED, false);
            gy += GOAL_H;
        }
        // progress: a bar and a count under the goals
        int by = y0 + H - 26;
        int filled = R_W * count / act.goals().size();
        g.fill(x, by, x + R_W, by + 3, 0x40A97CA9);
        g.fill(x, by, x + filled, by + 3, DONE);
        Component label = count == act.goals().size() ? Component.translatable("screen.promisedend.codex.complete")
                : Component.translatable("screen.promisedend.codex.progress", count, act.goals().size());
        g.text(font, label, x + (R_W - font.width(label)) / 2, by + 6, INK_MUTED, false);
    }

    private void wrapped(GuiGraphicsExtractor g, Component text, int x, int y, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, R_W);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) g.text(font, lines.get(i), x, y + i * 10, color, false);
    }

    /** A 9x9 tick box: an outline, and when done a filled centre with a tick, so it never relies on colour alone. */
    private static void box(GuiGraphicsExtractor g, int x, int y, boolean done) {
        g.fill(x, y, x + 8, y + 1, INK_MUTED);
        g.fill(x, y + 7, x + 8, y + 8, INK_MUTED);
        g.fill(x, y, x + 1, y + 8, INK_MUTED);
        g.fill(x + 7, y, x + 8, y + 8, INK_MUTED);
        if (done) {
            g.fill(x + 1, y + 1, x + 7, y + 7, 0x40A97CA9);
            // the tick: two strokes
            g.fill(x + 1, y + 4, x + 2, y + 5, DONE);
            g.fill(x + 2, y + 5, x + 3, y + 6, DONE);
            g.fill(x + 3, y + 4, x + 4, y + 5, DONE);
            g.fill(x + 4, y + 3, x + 5, y + 4, DONE);
            g.fill(x + 5, y + 2, x + 6, y + 3, DONE);
            g.fill(x + 6, y + 1, x + 7, y + 2, DONE);
        }
    }

    /** A small wax seal for a sealed act: a purpur disc with a darker rim. */
    private static void seal(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 2, y, x + 7, y + 9, SEAL);
        g.fill(x, y + 2, x + 9, y + 7, SEAL);
        g.fill(x + 1, y + 1, x + 8, y + 8, SEAL);
        g.fill(x + 3, y + 3, x + 6, y + 6, DONE);
        g.fill(x + 2, y + 1, x + 4, y + 2, 0xFFC69FC6);
    }

    /** A thin rule that fades out at both ends, with a small diamond in the middle. */
    private static void rule(GuiGraphicsExtractor g, int x, int y, int w) {
        int fade = Math.min(10, w / 4);
        for (int i = 0; i < w; i++) {
            int edge = Math.min(i, w - 1 - i);
            int a = edge >= fade ? 0xFF : 0xFF * (edge + 1) / (fade + 1);
            g.fill(x + i, y, x + i + 1, y + 1, (a << 24) | (INK_RULE & 0xFFFFFF));
        }
        int cx = x + w / 2;
        g.fill(cx - 1, y - 1, cx + 2, y + 2, INK_RULE);
        g.fill(cx, y - 2, cx + 1, y + 3, INK_RULE);
    }

    private String fit(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, maxWidth - font.width("...")) + "...";
    }

    private static Component roman(int n) {
        return Component.translatable("enchantment.level." + n);
    }

    // ---- input -------------------------------------------------------------------------------------------------

    private void choose(int act) {
        if (act == selected || act < 0 || act >= CodexBook.ACTS.size()) return;
        selected = act;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < CodexBook.ACTS.size(); i++) {
                if (in(event.x(), event.y(), actBounds(i))) {
                    choose(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isUp()) { choose(selected - 1); return true; }
        if (event.isDown()) { choose(selected + 1); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
