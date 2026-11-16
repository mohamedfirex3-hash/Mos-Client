package com.moxmc.mosclient;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Dark / orange "Mod Menu" screen (opened with Right Shift). */
public final class MosMenuScreen extends Screen {

    // ---------- colours ----------
    private static final int[] ACCENTS = {0xFFF97316, 0xFF3B82F6, 0xFFA855F7, 0xFFEF4444, 0xFF14B8A6};
    private static int accentIdx = 0;

    private static final int PANEL = 0xFF0D0F12;
    private static final int CARD = 0xFF13161B;
    private static final int CARD_BORDER = 0xFF1C2026;
    private static final int CARD_HOVER = 0xFF283040;
    private static final int CHIP_OFF = 0xFF171A1F;
    private static final int BOX = 0xFF1A1D22;
    private static final int GREEN = 0xFF84CC16;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF7A808A;
    private static final int DIM = 0xFF9CA3AF;

    private static final String[] FILTERS = {"All", "New", "HUD", "Hypixel", "PvP"};
    private static final String[] HEART = {
            ".##.##.",
            "#######",
            "#######",
            ".#####.",
            "..###..",
            "...#..."
    };
    private static final String[] GEAR = {
            "#.###.#",
            ".#####.",
            "###.###",
            "##...##",
            "###.###",
            ".#####.",
            "#.###.#"
    };
    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("yyyy/MM/dd  hh:mm a", Locale.ENGLISH);

    private static final int CARD_H = 66;
    private static final int GAP = 6;

    // ---------- data (static so it survives closing / reopening the menu) ----------
    private static final class Feature {
        final String name, category;
        final ItemStack icon;
        boolean enabled;

        Feature(String name, String category, Item icon, boolean enabled) {
            this.name = name;
            this.category = category;
            this.icon = new ItemStack(icon);
            this.enabled = enabled;
        }

        boolean hasTag(String t) {
            return category.equals(t);
        }
    }

    private static final List<Feature> FEATURES = new ArrayList<>();
    private static final Set<String> FAVORITES = new HashSet<>();

    static {
        FEATURES.add(new Feature("Animations", "PvP", Items.IRON_SWORD, true));
        FEATURES.add(new Feature("Armor Bar", "HUD", Items.IRON_CHESTPLATE, true));
        FEATURES.add(new Feature("Armor Status", "HUD", Items.DIAMOND_CHESTPLATE, true));
        FEATURES.add(new Feature("Attack Indicator", "PvP", Items.IRON_AXE, false));
        FEATURES.add(new Feature("Autohide HUD", "HUD", Items.ITEM_FRAME, false));
        FEATURES.add(new Feature("Auto Perspective", "PvP", Items.ENDER_EYE, false));
        FEATURES.add(new Feature("Auto Text", "Hypixel", Items.OAK_SIGN, false));
        FEATURES.add(new Feature("Backups", "New", Items.CHEST, false));
        FEATURES.add(new Feature("Block Indicator", "HUD", Items.GRASS_BLOCK, false));
        FEATURES.add(new Feature("Block Overlay", "HUD", Items.GLASS, false));
        FEATURES.add(new Feature("Boss Bar", "HUD", Items.DRAGON_HEAD, false));
        FEATURES.add(new Feature("Brightness", "PvP", Items.GLOWSTONE, true));
    }

    // ---------- state ----------
    private TextFieldWidget searchField;
    private String query = "";
    private int filter = 0;
    private boolean favOnly = false;
    private double scroll = 0;

    // layout (computed in init)
    private int px, py, pw, ph, headerY, tabsY, toolbarY;
    private int gx, gy, gw, gh, cols, cw;
    private int searchX, searchW;
    private final int[] chipX = new int[FILTERS.length];
    private final int[] chipW = new int[FILTERS.length];

    public MosMenuScreen() {
        super(Text.literal("Mos Client"));
    }

    private static int accent() {
        return ACCENTS[accentIdx];
    }

    // ---------- init ----------
    @Override
    protected void init() {
        pw = Math.min(width - 8, Math.max(300, Math.min((int) (width * 0.78), 520)));
        px = (width - pw) / 2;
        headerY = 10;
        tabsY = headerY + 26;
        py = tabsY + 24;
        ph = Math.max(110, height - py - 10);
        toolbarY = py + 8;

        gx = px + 8;
        gy = toolbarY + 16 + 8;
        gw = pw - 16;
        gh = py + ph - gy - 6;
        cols = Math.max(2, (gw - 6 + GAP) / (96 + GAP));
        cw = (gw - 6 - GAP * (cols - 1)) / cols;

        int x = gx;
        for (int i = 0; i < FILTERS.length; i++) {
            chipW[i] = textRenderer.getWidth(FILTERS[i]) + 16;
            chipX[i] = x;
            x += chipW[i] + 4;
        }

        searchW = Math.min(130, pw / 3);
        searchX = px + pw - 8 - 16 - 4 - searchW;
        searchField = new TextFieldWidget(textRenderer, searchX + 6, toolbarY, searchW - 12, 16, Text.literal("Search"));
        searchField.setDrawsBackground(false);
        searchField.setMaxLength(24);
        searchField.setPlaceholder(Text.literal("Search"));
        searchField.setText(query);
        searchField.setChangedListener(s -> {
            query = s;
            scroll = 0;
        });
        addDrawableChild(searchField);
    }

    // ---------- helpers ----------
    private List<Feature> visible() {
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Feature> out = new ArrayList<>();
        for (Feature f : FEATURES) {
            if (filter > 0 && !f.hasTag(FILTERS[filter])) continue;
            if (favOnly && !FAVORITES.contains(f.name)) continue;
            if (!q.isEmpty() && !f.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(f);
        }
        return out;
    }

    private int maxScroll(int count) {
        int rows = (count + cols - 1) / cols;
        return Math.max(0, rows * (CARD_H + GAP) - GAP - gh);
    }

    private int cardX(int i) {
        return gx + (i % cols) * (cw + GAP);
    }

    private int cardY(int i) {
        return gy + (i / cols) * (CARD_H + GAP) - (int) scroll;
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean inGrid(double mx, double my) {
        return in(mx, my, gx, gy, gw, gh);
    }

    private static int lighten(int argb, float t) {
        int a = argb >>> 24 & 255, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        r += (int) ((255 - r) * t);
        g += (int) ((255 - g) * t);
        b += (int) ((255 - b) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    /** Rectangle with 1px rounded corners. */
    private static void rect(DrawContext g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + w, y + h - 1, color);
    }

    private void text(DrawContext g, String s, int x, int y, int color) {
        g.drawText(textRenderer, s, x, y, color, false);
    }

    private void ctext(DrawContext g, String s, int cx, int y, int color) {
        text(g, s, cx - textRenderer.getWidth(s) / 2, y, color);
    }

    private static void bitmap(DrawContext g, String[] rows, int x, int y, int color) {
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length(); c++) {
                if (rows[r].charAt(c) == '#') g.fill(x + c, y + r, x + c + 1, y + r + 1, color);
            }
        }
    }

    // ---------- render ----------
    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        renderBackground(g, mx, my, delta);
        g.fill(0, 0, width, height, 0x66000000);
        int acc = accent();

        // header: logo + clock pill
        rect(g, px, headerY, 22, 22, WHITE);
        ctext(g, "M", px + 11, headerY + 7, 0xFF111111);
        String clock = LocalDateTime.now().format(CLOCK);
        int pillW = textRenderer.getWidth(clock) + 20;
        rect(g, px + 30, headerY + 3, pillW, 16, BOX);
        g.fill(px + 36, headerY + 8, px + 40, headerY + 14, DIM);
        g.fill(px + 38, headerY + 8, px + 41, headerY + 14, BOX);
        text(g, clock, px + 44, headerY + 7, WHITE);

        // tab row
        String tab = "MOD MENU";
        int tabW = textRenderer.getWidth(tab) + 26;
        rect(g, px, tabsY, tabW, 20, acc);
        for (int i = 0; i < 4; i++) {
            g.fill(px + 6 + (i % 2) * 5, tabsY + 5 + (i / 2) * 5,
                    px + 10 + (i % 2) * 5, tabsY + 9 + (i / 2) * 5, WHITE);
        }
        text(g, tab, px + 20, tabsY + 6, WHITE);

        // palette + close
        int closeX = px + pw - 20;
        int palX = closeX - 24;
        boolean hClose = in(mx, my, closeX, tabsY, 20, 20);
        boolean hPal = in(mx, my, palX, tabsY, 20, 20);
        rect(g, palX, tabsY, 20, 20, acc);
        rect(g, palX + 1, tabsY + 1, 18, 18, hPal ? 0xFF2A1F17 : 0xFF1A1410);
        rect(g, palX + 5, tabsY + 5, 10, 10, acc);
        rect(g, closeX, tabsY, 20, 20, acc);
        rect(g, closeX + 1, tabsY + 1, 18, 18, hClose ? 0xFF2A1F17 : 0xFF1A1410);
        ctext(g, "x", closeX + 10, tabsY + 6, acc);

        // panel
        rect(g, px, py, pw, ph, PANEL);

        // chips
        for (int i = 0; i < FILTERS.length; i++) {
            boolean active = i == filter;
            boolean h = in(mx, my, chipX[i], toolbarY, chipW[i], 16);
            int bg = active ? acc : (h ? lighten(CHIP_OFF, 0.1f) : CHIP_OFF);
            rect(g, chipX[i], toolbarY, chipW[i], 16, bg);
            ctext(g, FILTERS[i], chipX[i] + chipW[i] / 2, toolbarY + 4, active ? WHITE : MUTED);
        }

        // search box + favourites-only
        rect(g, searchX, toolbarY, searchW, 16, BOX);
        int favBtnX = px + pw - 8 - 16;
        boolean hFav = in(mx, my, favBtnX, toolbarY, 16, 16);
        rect(g, favBtnX, toolbarY, 16, 16, hFav ? lighten(BOX, 0.1f) : BOX);
        bitmap(g, HEART, favBtnX + 4, toolbarY + 5, favOnly ? acc : DIM);

        // grid
        List<Feature> list = visible();
        scroll = Math.max(0, Math.min(scroll, maxScroll(list.size())));
        g.enableScissor(gx, gy, gx + gw, gy + gh);
        for (int i = 0; i < list.size(); i++) {
            int x = cardX(i), y = cardY(i);
            if (y + CARD_H < gy || y > gy + gh) continue;
            drawCard(g, list.get(i), x, y, mx, my);
        }
        g.disableScissor();

        if (list.isEmpty()) ctext(g, "No mods found", px + pw / 2, gy + 20, MUTED);

        int max = maxScroll(list.size());
        if (max > 0) {
            int trackX = gx + gw - 3;
            g.fill(trackX, gy, trackX + 3, gy + gh, 0xFF15181D);
            int thumbH = Math.max(14, gh * gh / (gh + max));
            int thumbY = gy + (int) ((gh - thumbH) * (scroll / max));
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, acc);
        }

        searchField.render(g, mx, my, delta);
    }

    private void drawCard(DrawContext g, Feature f, int x, int y, int mx, int my) {
        boolean hover = inGrid(mx, my) && in(mx, my, x, y, cw, CARD_H);
        boolean on = f.enabled;
        int acc = accent();

        rect(g, x, y, cw, CARD_H, CARD_BORDER);
        rect(g, x + 1, y + 1, cw - 2, CARD_H - 2, hover ? CARD_HOVER : CARD);

        String title = f.name;
        int maxTitle = cw - 12 - 20;
        if (textRenderer.getWidth(title) > maxTitle) {
            title = textRenderer.trimToWidth(title, maxTitle - 6) + "..";
        }
        text(g, title, x + 6, y + 7, on ? WHITE : MUTED);

        int hx = x + cw - 6 - 14, hy = y + 4;
        boolean hHeart = inGrid(mx, my) && in(mx, my, hx, hy, 14, 14);
        rect(g, hx, hy, 14, 14, hHeart ? 0xFF2A2F38 : 0xFF1C2027);
        bitmap(g, HEART, hx + 3, hy + 4, FAVORITES.contains(f.name) ? acc : DIM);

        g.drawItem(f.icon, x + (cw - 16) / 2, y + 21);

        // gear (decorative) + Enabled/Disabled button
        int by = y + CARD_H - 6 - 16;
        rect(g, x + 6, by, 16, 16, BOX);
        bitmap(g, GEAR, x + 6 + 4, by + 4, DIM);

        int bx = x + 6 + 16 + 4, bw = cw - 12 - 16 - 4;
        boolean hBtn = inGrid(mx, my) && in(mx, my, bx, by, bw, 16);
        int bg = on ? GREEN : BOX;
        rect(g, bx, by, bw, 16, hBtn ? lighten(bg, 0.12f) : bg);
        ctext(g, on ? "Enabled" : "Disabled", bx + bw / 2, by + 4, on ? WHITE : MUTED);
    }

    // ---------- input (1.21.11 signatures) ----------
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        boolean consumed = super.mouseClicked(click, doubled); // lets the search box take focus
        if (click.button() != 0) return consumed;
        double mx = click.x(), my = click.y();

        int closeX = px + pw - 20;
        int palX = closeX - 24;
        if (in(mx, my, closeX, tabsY, 20, 20)) {
            close();
            return true;
        }
        if (in(mx, my, palX, tabsY, 20, 20)) {
            accentIdx = (accentIdx + 1) % ACCENTS.length;
            return true;
        }
        for (int i = 0; i < FILTERS.length; i++) {
            if (in(mx, my, chipX[i], toolbarY, chipW[i], 16)) {
                filter = i;
                scroll = 0;
                return true;
            }
        }
        if (in(mx, my, px + pw - 8 - 16, toolbarY, 16, 16)) {
            favOnly = !favOnly;
            scroll = 0;
            return true;
        }

        if (inGrid(mx, my)) {
            List<Feature> list = visible();
            for (int i = 0; i < list.size(); i++) {
                int x = cardX(i), y = cardY(i);
                if (!in(mx, my, x, y, cw, CARD_H)) continue;
                Feature f = list.get(i);

                if (in(mx, my, x + cw - 6 - 14, y + 4, 14, 14)) {
                    if (!FAVORITES.remove(f.name)) FAVORITES.add(f.name);
                    return true;
                }
                int by = y + CARD_H - 6 - 16;
                int bx = x + 6 + 16 + 4, bw = cw - 12 - 16 - 4;
                if (in(mx, my, bx, by, bw, 16)) {
                    f.enabled = !f.enabled;
                    return true;
                }
            }
        }
        return consumed;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inGrid(mouseX, mouseY)) {
            scroll = Math.max(0, Math.min(scroll - verticalAmount * 16, maxScroll(visible().size())));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 344) { // RIGHT SHIFT closes the menu too
            close();
            return true;
        }
        return super.keyPressed(input); // ESC is handled by Screen
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
