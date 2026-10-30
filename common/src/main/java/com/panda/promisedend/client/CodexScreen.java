package com.panda.promisedend.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.logic.CodexBook;
import java.util.ArrayList;
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
 * The Promise Codex: an open book. The left page lists the acts with their progress, and under them the Ancient
 * Tablets; the right page shows the chosen act's goals, ticked from the client's own advancement progress, or one read
 * tablet's page, turned with the arrows, or the constructs met, each with its one rule. Read-only. Layout constants match the page texture, textures/gui/codex.png.
 */
public class CodexScreen extends Screen {
    static final int W = 300, H = 184;
    static final int L_X = 16, L_W = 124, R_X = 160, R_W = 124;
    static final int ACT_Y = 38, ACT_H = 18;
    /** The left page's rows for the tablets and the constructs, after the acts. */
    public static final int TABLETS = CodexBook.ACTS.size(), CONSTRUCTS = TABLETS + 1;
    static final int GOAL_Y = 33, GOAL_H = 20;   // six goals, an act's most, above the progress bar

    static final Identifier BG = PromisedEnd.id("textures/gui/codex.png");
    // ink on the pale pages (the Voidstone and purpur ramps)
    static final int INK = 0xFF1A1524, INK_MUTED = 0xFF4C4267, INK_RULE = 0xFFA97CA9, DONE = 0xFF6E4A6E, SEAL = 0xFF8C628C;

    private int x0, y0;
    private int selected;
    /** The tablet page shown, 1 to {@link CodexBook#PAGES}; 0 is the newest read one. */
    private int page;

    public CodexScreen() {
        this(-1);
    }

    /** Open on act {@code act}, or on the first for -1 (the Architect's Promise opens it on the last). */
    public CodexScreen(int act) {
        super(Component.translatable("screen.promisedend.codex.title"));
        if (act >= 0) select(act);
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

    /**
     * Whether the act is in the game at all: it has goals, and its opening exists. An act whose guardian isn't yet has
     * blank pages, not a seal.
     */
    public boolean written(CodexBook.Act act) {
        if (act.goals().isEmpty()) return false;
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
        selected = Math.max(0, Math.min(CONSTRUCTS, act));
    }

    // ---- the tablets ------------------------------------------------------------------------------------------------

    /** The pages this player has read, in order. */
    public List<Integer> readPages() {
        List<Integer> read = new ArrayList<>();
        for (int n = 1; n <= CodexBook.PAGES; n++) if (done(CodexBook.page(n))) read.add(n);
        return read;
    }

    /** The page shown on the tablets' view, or 0 with none read. */
    public int shownPage() {
        List<Integer> read = readPages();
        if (read.isEmpty()) return 0;
        return read.contains(page) ? page : read.getLast();
    }

    /** Turns to the read page {@code step} on (or back) from the one shown. */
    public void turn(int step) {
        List<Integer> read = readPages();
        if (read.isEmpty()) return;
        int i = read.indexOf(shownPage()) + step;
        if (i < 0 || i >= read.size()) return;
        page = read.get(i);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F));
    }

    /** How many of the constructs this player has met. */
    public int metCount() {
        int n = 0;
        for (CodexBook.Construct c : CodexBook.CONSTRUCTS) if (done(c.met())) n++;
        return n;
    }

    private AdvancementHolder holder(Identifier id) {
        return minecraft.player == null ? null : minecraft.player.connection.getAdvancements().get(id);
    }

    public int[] arrowBounds(int side) {
        return new int[] {side < 0 ? x0 + R_X - 2 : x0 + R_X + R_W - 12, y0 + H - 22, 14, 12};
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
        drawGoals(g, mx, my);
        goalTooltip(g, mx, my);
    }

    public int[] goalBounds(int i) {
        return new int[] {x0 + R_X - 2, y0 + GOAL_Y + i * GOAL_H - 2, R_W + 4, GOAL_H - 2};
    }

    /** The goal's advancement description in full: the hints on the page are short on purpose. */
    private void goalTooltip(GuiGraphicsExtractor g, int mx, int my) {
        if (selected == TABLETS) return;
        if (selected == CONSTRUCTS) {
            for (int i = 0; i < CodexBook.CONSTRUCTS.size(); i++) {
                CodexBook.Construct c = CodexBook.CONSTRUCTS.get(i);
                if (!in(mx, my, goalBounds(i)) || !done(c.met())) continue;
                // in full, as its advancement says it: the page's line is short on purpose
                AdvancementHolder holder = holder(c.met());
                onPage(g, mx, my, i, holder != null && holder.value().display().isPresent() ? holder.value().display().get().description()
                        : Component.translatable(c.rule()));
            }
            return;
        }
        CodexBook.Act act = CodexBook.ACTS.get(selected);
        if (!open(act) || minecraft.player == null) return;
        for (int i = 0; i < act.goals().size(); i++) {
            if (!in(mx, my, goalBounds(i))) continue;
            AdvancementHolder holder = minecraft.player.connection.getAdvancements().get(act.goals().get(i).advancement());
            Component text = holder != null && holder.value().display().isPresent() ? holder.value().display().get().description()
                    : Component.translatable(act.goals().get(i).hint());
            onPage(g, mx, my, i, text);
            return;
        }
    }

    /**
     * {@code text} on the right page, just under row {@code i}, wrapped to the page: a tooltip at the mouse flips over
     * the act list on a narrow screen.
     */
    private void onPage(GuiGraphicsExtractor g, int mx, int my, int i, Component text) {
        int[] row = goalBounds(i);
        int px = x0 + R_X + 2, py = row[1] + row[3] + 4;
        ClientTooltipPositioner under = (sw, sh, x, y, tw, th) -> new Vector2i(Math.min(px, sw - tw - 4), Math.min(py, sh - th - 4));
        g.setTooltipForNextFrame(font, font.split(text, R_W - 10), under, mx, my, false);
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
            row(g, mx, my, i);
            Component numeral = Component.translatable("screen.promisedend.codex.act", roman(i + 1));
            g.text(font, numeral, b[0] + 6, b[1] + 1, open ? INK : INK_MUTED, false);
            g.text(font, Component.translatable(act.title()), b[0] + 6 + font.width(numeral) + 4, b[1] + 1, open ? INK : INK_MUTED, false);
            Component status;
            if (!written(act)) status = Component.translatable("screen.promisedend.codex.unwritten");
            else if (!open) status = Component.translatable("screen.promisedend.codex.sealed");
            else if (act.goals().isEmpty()) status = Component.translatable("screen.promisedend.codex.unwritten");
            else status = Component.translatable("screen.promisedend.codex.progress", doneCount(act), act.goals().size());
            g.text(font, status, b[0] + 6, b[1] + 10, open ? INK_MUTED : SEAL, false);
            if (!open && written(act)) seal(g, b[0] + b[2] - 13, b[1] + 4);
        }
        int[] t = actBounds(TABLETS);
        row(g, mx, my, TABLETS);
        g.text(font, Component.translatable("screen.promisedend.codex.tablets"), t[0] + 6, t[1] + 1, INK, false);
        g.text(font, Component.translatable("screen.promisedend.codex.tablets.read", readPages().size(), CodexBook.PAGES), t[0] + 6, t[1] + 10, INK_MUTED, false);
        int[] c = actBounds(CONSTRUCTS);
        row(g, mx, my, CONSTRUCTS);
        g.text(font, Component.translatable("screen.promisedend.codex.constructs"), c[0] + 6, c[1] + 1, INK, false);
        g.text(font, Component.translatable("screen.promisedend.codex.constructs.met", metCount(), CodexBook.CONSTRUCTS.size()), c[0] + 6, c[1] + 10, INK_MUTED, false);
        // a line of the Ancients under the rows, so the page isn't only a list: where the Ward stands
        int py = y0 + ACT_Y + (CONSTRUCTS + 1) * ACT_H + 2;
        rule(g, x0 + L_X, py, L_W);
        Component prologue = Component.translatable(done(CodexBook.WARD_DOWN) ? "screen.promisedend.codex.prologue.open" : "screen.promisedend.codex.prologue")
                .withStyle(ChatFormatting.ITALIC);
        FormattedCharSequence line = font.split(prologue, L_W).getFirst();
        g.text(font, line, cx - font.width(line) / 2, py + 5, INK_MUTED, false);
    }

    /** A row's ground on the left page: the chosen one marked with a bar, so the choice isn't colour alone. */
    private void row(GuiGraphicsExtractor g, int mx, int my, int i) {
        int[] b = actBounds(i);
        if (i == selected) {
            g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], 0x30A97CA9);
            g.fill(b[0], b[1], b[0] + 2, b[1] + b[3], DONE);
        } else if (in(mx, my, b)) {
            g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], 0x18A97CA9);
        }
    }

    /** The tablets' page: one read page, the newest at first, its title as its advancement has it, and arrows to the others. */
    private void drawTablet(GuiGraphicsExtractor g, int mx, int my) {
        int x = x0 + R_X;
        int n = shownPage();
        AdvancementHolder holder = n == 0 ? null : holder(CodexBook.page(n));
        if (holder == null || holder.value().display().isEmpty()) {
            Component heading = Component.translatable("screen.promisedend.codex.tablets");
            g.text(font, heading, x + (R_W - font.width(heading)) / 2, y0 + 12, INK, false);
            rule(g, x, y0 + 24, R_W);
            wrapped(g, Component.translatable("screen.promisedend.codex.tablets.none"), x, y0 + 40, INK_MUTED, 8);
            return;
        }
        var display = holder.value().display().get();
        Component heading = Component.literal(fit(display.title().getString(), R_W));
        g.text(font, heading, x + (R_W - font.width(heading)) / 2, y0 + 12, INK, false);
        rule(g, x, y0 + 24, R_W);
        wrapped(g, Component.translatable(CodexBook.text(n)), x, y0 + 32, INK, 12);
        List<Integer> read = readPages();
        int i = read.indexOf(n);
        Component where = Component.translatable("screen.promisedend.codex.tablets.page", n, CodexBook.PAGES);
        g.text(font, where, x + (R_W - font.width(where)) / 2, y0 + H - 20, INK_MUTED, false);
        if (i > 0) arrow(g, arrowBounds(-1), "<", in(mx, my, arrowBounds(-1)));
        if (i < read.size() - 1) arrow(g, arrowBounds(1), ">", in(mx, my, arrowBounds(1)));
    }

    /** The constructs' page: each one met, its name and its rule cut to a line (in full under it on hover); one not met yet, unknown. */
    private void drawConstructs(GuiGraphicsExtractor g) {
        int x = x0 + R_X;
        Component heading = Component.translatable("screen.promisedend.codex.constructs");
        g.text(font, heading, x + (R_W - font.width(heading)) / 2, y0 + 12, INK, false);
        rule(g, x, y0 + 24, R_W);
        int gy = y0 + GOAL_Y;
        for (CodexBook.Construct c : CodexBook.CONSTRUCTS) {
            boolean met = done(c.met());
            box(g, x, gy + 1, met);
            g.text(font, met ? fit(Component.translatable(c.name()).getString(), R_W - 12) : "???", x + 12, gy, met ? INK : INK_MUTED, false);
            g.text(font, fit(Component.translatable(met ? c.rule() : "screen.promisedend.codex.constructs.unmet").getString(), R_W - 12), x + 12, gy + 9, INK_MUTED, false);
            gy += GOAL_H;
        }
    }

    private void arrow(GuiGraphicsExtractor g, int[] b, String glyph, boolean hover) {
        if (hover) g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], 0x30A97CA9);
        g.text(font, Component.literal(glyph), b[0] + (b[2] - font.width(glyph)) / 2 + 1, b[1] + 2, hover ? INK : INK_MUTED, false);
    }

    private void drawGoals(GuiGraphicsExtractor g, int mx, int my) {
        if (selected == TABLETS) {
            drawTablet(g, mx, my);
            return;
        }
        if (selected == CONSTRUCTS) {
            drawConstructs(g);
            return;
        }
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
            g.text(font, fit(Component.translatable(goal.hint()).getString(), R_W - 12), x + 12, gy + 9, INK_MUTED, false);
            gy += GOAL_H;
        }
        if (selected == CodexBook.ACTS.size() - 1) closing(g, x, gy + 4);
        // progress: a bar and a count under the goals
        int by = y0 + H - 26;
        int filled = R_W * count / act.goals().size();
        g.fill(x, by, x + R_W, by + 3, 0x40A97CA9);
        g.fill(x, by, x + filled, by + 3, DONE);
        Component label = count == act.goals().size() ? Component.translatable("screen.promisedend.codex.complete")
                : Component.translatable("screen.promisedend.codex.progress", count, act.goals().size());
        g.text(font, label, x + (R_W - font.width(label)) / 2, by + 6, INK_MUTED, false);
    }

    /** The closing page under the last act's goals: written in once the Architect's Promise is used, its hint till then. */
    private void closing(GuiGraphicsExtractor g, int x, int y) {
        rule(g, x, y, R_W);
        if (!done(CodexBook.PROMISE_KEPT)) {
            wrapped(g, Component.translatable("screen.promisedend.codex.closing.unread"), x, y + 6, INK_MUTED, 3);
            return;
        }
        Component heading = Component.translatable("screen.promisedend.codex.closing").withStyle(ChatFormatting.ITALIC);
        g.text(font, heading, x + (R_W - font.width(heading)) / 2, y + 5, INK, false);
        wrapped(g, Component.translatable("codex.promisedend.closing"), x, y + 17, INK_MUTED, 6);
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
        if (act == selected || act < 0 || act > CONSTRUCTS) return;
        selected = act;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (selected == TABLETS) {
                for (int side : new int[] {-1, 1}) {
                    if (in(event.x(), event.y(), arrowBounds(side))) {
                        turn(side);
                        return true;
                    }
                }
            }
            for (int i = 0; i <= CONSTRUCTS; i++) {
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
        if (selected == TABLETS && event.isLeft()) { turn(-1); return true; }
        if (selected == TABLETS && event.isRight()) { turn(1); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
