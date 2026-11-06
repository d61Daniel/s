package com.ps5mod.client;

import com.ps5mod.Ps5Block;
import com.ps5mod.Ps5Mod;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** O "ecrã da TV": arranque da PS5, menu inicial e dois jogos (Snake e Pong). */
public class Ps5Screen extends Screen {
    /** PS5 (posições) que já arrancaram e continuam ligadas. */
    public static final Set<Long> BOOTED = new HashSet<>();

    private enum Mode { BOOT, HOME, SNAKE, PONG }

    private static final String[] TILES = {"Snake", "Pong", "Sair"};
    private static final int[] TILE_COLORS = {0xFF2E8B57, 0xFF3A6FD8, 0xFF8B2E3A};

    private final BlockPos tvPos;
    private final BlockPos ps5Pos;
    private final long startMs = System.currentTimeMillis();
    private final Random rng = new Random();

    private Mode mode = Mode.BOOT;
    private boolean chimed = false;
    private int selected = 0;

    // ---- Snake ----
    private static final int COLS = 20;
    private static final int ROWS = 14;
    private final Deque<int[]> snake = new ArrayDeque<>();
    private int dx = 1, dy = 0, ndx = 1, ndy = 0;
    private int[] food = {0, 0};
    private int score = 0;
    private boolean dead = false;
    private int stepTick = 0;

    // ---- Pong ----
    private static final int PW = 8;
    private static final int BS = 8;
    private float bx, by, vx, vy, py, ay;
    private int scorePl = 0, scoreAi = 0;

    public Ps5Screen(BlockPos tvPos, BlockPos ps5Pos) {
        super(Text.literal("PS5"));
        this.tvPos = tvPos;
        this.ps5Pos = ps5Pos;
        if (BOOTED.contains(ps5Pos.asLong())) {
            mode = Mode.HOME;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // =====================================================================
    // Lógica
    // =====================================================================

    @Override
    public void tick() {
        if (client == null || client.world == null) {
            close();
            return;
        }
        BlockState ps5 = client.world.getBlockState(ps5Pos);
        if (!ps5.isOf(Ps5Mod.PS5_BLOCK) || !ps5.get(Ps5Block.POWERED)
                || !client.world.getBlockState(tvPos).isOf(Ps5Mod.TV_BLOCK)) {
            close();
            return;
        }

        switch (mode) {
            case BOOT -> {
                long t = System.currentTimeMillis() - startMs;
                if (t > 1300 && !chimed) {
                    chimed = true;
                    if (client.player != null) {
                        client.player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.4f);
                    }
                }
                if (t > 4600) {
                    mode = Mode.HOME;
                    BOOTED.add(ps5Pos.asLong());
                }
            }
            case SNAKE -> {
                if (!dead && ++stepTick >= 3) {
                    stepTick = 0;
                    stepSnake();
                }
            }
            case PONG -> tickPong();
            default -> {}
        }
    }

    private void activate(int i) {
        if (i == 0) startSnake();
        else if (i == 1) startPong();
        else close();
    }

    // ---- Snake ----
    private void startSnake() {
        snake.clear();
        snake.addLast(new int[]{10, 7});
        snake.addLast(new int[]{9, 7});
        snake.addLast(new int[]{8, 7});
        dx = ndx = 1;
        dy = ndy = 0;
        score = 0;
        dead = false;
        stepTick = 0;
        placeFood();
        mode = Mode.SNAKE;
    }

    private void placeFood() {
        while (true) {
            int fx = rng.nextInt(COLS);
            int fy = rng.nextInt(ROWS);
            boolean onSnake = false;
            for (int[] p : snake) {
                if (p[0] == fx && p[1] == fy) { onSnake = true; break; }
            }
            if (!onSnake) {
                food = new int[]{fx, fy};
                return;
            }
        }
    }

    private void stepSnake() {
        dx = ndx;
        dy = ndy;
        int[] head = snake.peekFirst();
        int nx = head[0] + dx;
        int ny = head[1] + dy;
        if (nx < 0 || ny < 0 || nx >= COLS || ny >= ROWS) {
            dead = true;
            return;
        }
        for (int[] p : snake) {
            if (p[0] == nx && p[1] == ny) {
                dead = true;
                return;
            }
        }
        snake.addFirst(new int[]{nx, ny});
        if (nx == food[0] && ny == food[1]) {
            score++;
            placeFood();
        } else {
            snake.pollLast();
        }
    }

    // ---- Pong ----
    private int fx() { return 30; }
    private int fy() { return 40; }
    private int fw() { return width - 60; }
    private int fh() { return height - 80; }
    private float ph() { return Math.max(30f, fh() / 5f); }

    private void startPong() {
        scorePl = 0;
        scoreAi = 0;
        py = ay = fy() + fh() / 2f - ph() / 2f;
        resetBall(1);
        mode = Mode.PONG;
    }

    private void resetBall(int dir) {
        bx = fx() + fw() / 2f;
        by = fy() + fh() / 2f;
        vx = 4f * dir;
        vy = (rng.nextFloat() - 0.5f) * 4f;
    }

    private float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void tickPong() {
        float ph = ph();
        bx += vx;
        by += vy;

        if (by < fy() + BS / 2f) { by = fy() + BS / 2f; vy = -vy; }
        if (by > fy() + fh() - BS / 2f) { by = fy() + fh() - BS / 2f; vy = -vy; }

        float plX = fx() + 10 + PW;
        if (vx < 0 && bx - BS / 2f <= plX && bx > fx() && by >= py && by <= py + ph) {
            bx = plX + BS / 2f;
            vx = clamp(-vx * 1.05f, -12f, 12f);
            vy += ((by - (py + ph / 2f)) / (ph / 2f)) * 2f;
        }
        float aiX = fx() + fw() - 10 - PW;
        if (vx > 0 && bx + BS / 2f >= aiX && bx < fx() + fw() && by >= ay && by <= ay + ph) {
            bx = aiX - BS / 2f;
            vx = clamp(-vx * 1.03f, -12f, 12f);
            vy += ((by - (ay + ph / 2f)) / (ph / 2f)) * 2f;
        }
        vy = clamp(vy, -8f, 8f);

        if (bx < fx()) { scoreAi++; resetBall(1); }
        if (bx > fx() + fw()) { scorePl++; resetBall(-1); }

        float target = by - ph / 2f;
        ay += clamp(target - ay, -3.2f, 3.2f);
        ay = clamp(ay, fy(), fy() + fh() - ph);
    }

    // =====================================================================
    // Input
    // =====================================================================

    private int[] tileRect(int i) {
        int w = 100, h = 120, gap = 20;
        int total = TILES.length * w + (TILES.length - 1) * gap;
        int x0 = (width - total) / 2;
        return new int[]{x0 + i * (w + gap), height / 2 - 50, w, h};
    }

    private int tileAt(double mx, double my) {
        for (int i = 0; i < TILES.length; i++) {
            int[] r = tileRect(i);
            if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) return i;
        }
        return -1;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (mode == Mode.HOME) {
            int i = tileAt(mouseX, mouseY);
            if (i >= 0) selected = i;
        } else if (mode == Mode.PONG) {
            py = clamp((float) mouseY - ph() / 2f, fy(), fy() + fh() - ph());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mode == Mode.HOME) {
            int i = tileAt(mouseX, mouseY);
            if (i >= 0) {
                selected = i;
                activate(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (mode == Mode.BOOT) {
            return super.keyPressed(key, scancode, modifiers);
        }
        if (mode == Mode.HOME) {
            if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) {
                selected = (selected + TILES.length - 1) % TILES.length;
                return true;
            }
            if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) {
                selected = (selected + 1) % TILES.length;
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) {
                activate(selected);
                return true;
            }
            return super.keyPressed(key, scancode, modifiers);
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            mode = Mode.HOME;
            return true;
        }
        if (mode == Mode.SNAKE) {
            if ((key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_W) && dy == 0) { ndx = 0; ndy = -1; }
            else if ((key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_S) && dy == 0) { ndx = 0; ndy = 1; }
            else if ((key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) && dx == 0) { ndx = -1; ndy = 0; }
            else if ((key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) && dx == 0) { ndx = 1; ndy = 0; }
            else if (key == GLFW.GLFW_KEY_ENTER && dead) startSnake();
        } else if (mode == Mode.PONG) {
            if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_W) {
                py = clamp(py - 14f, fy(), fy() + fh() - ph());
            } else if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_S) {
                py = clamp(py + 14f, fy(), fy() + fh() - ph());
            }
        }
        return true;
    }

    // =====================================================================
    // Desenho
    // =====================================================================

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        switch (mode) {
            case BOOT -> renderBoot(c);
            case HOME -> renderHome(c);
            case SNAKE -> renderSnake(c);
            case PONG -> renderPong(c);
        }
    }

    private void bigText(DrawContext c, String s, int cx, int cy, float scale, int color) {
        Text t = Text.literal(s).formatted(Formatting.BOLD);
        int w = textRenderer.getWidth(t);
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate((float) cx, (float) cy, 0f);
        m.scale(scale, scale, 1f);
        c.drawText(textRenderer, t, -w / 2, -4, color, false);
        m.pop();
    }

    private void renderBoot(DrawContext c) {
        long t = System.currentTimeMillis() - startMs;
        c.fill(0, 0, width, height, 0xFF000000);
        float a = 0f;
        if (t > 800 && t <= 2000) a = (t - 800) / 1200f;
        else if (t > 2000 && t <= 3600) a = 1f;
        else if (t > 3600) a = Math.max(0f, 1f - (t - 3600) / 900f);
        if (a > 0.03f) {
            int alpha = Math.max(8, Math.min(255, (int) (a * 255)));
            int col = (alpha << 24) | 0x00FFFFFF;
            bigText(c, "PS5", width / 2, height / 2 - 6, Math.max(2f, height / 50f), col);
            int small = (Math.max(8, alpha / 2) << 24) | 0x00FFFFFF;
            bigText(c, "PlayStation", width / 2, height / 2 + height / 8, Math.max(1f, height / 150f), small);
        }
    }

    private void renderHome(DrawContext c) {
        c.fillGradient(0, 0, width, height, 0xFF05102B, 0xFF1E5BC6);
        c.drawText(textRenderer, Text.literal("PlayStation 5").formatted(Formatting.BOLD), 20, 14, 0xFFFFFFFF, true);
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        c.drawText(textRenderer, time, width - 20 - textRenderer.getWidth(time), 14, 0xFFFFFFFF, true);

        for (int i = 0; i < TILES.length; i++) {
            int[] r = tileRect(i);
            if (i == selected) {
                c.fill(r[0] - 3, r[1] - 3, r[0] + r[2] + 3, r[1] + r[3] + 3, 0xFFFFFFFF);
            }
            c.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], TILE_COLORS[i]);
            c.drawCenteredTextWithShadow(textRenderer, TILES[i], r[0] + r[2] / 2, r[1] + r[3] / 2 - 4, 0xFFFFFFFF);
        }
        c.drawCenteredTextWithShadow(textRenderer, "Setas: mover | Enter: abrir | Esc: sair", width / 2, height - 24, 0xFFB0C4E8);
    }

    private void renderSnake(DrawContext c) {
        c.fill(0, 0, width, height, 0xFF0B0F14);
        int cs = Math.max(8, Math.min((width - 40) / COLS, (height - 90) / ROWS));
        int ox = (width - COLS * cs) / 2;
        int oy = (height - ROWS * cs) / 2 + 8;
        c.fill(ox - 2, oy - 2, ox + COLS * cs + 2, oy + ROWS * cs + 2, 0xFF2B3A55);
        c.fill(ox, oy, ox + COLS * cs, oy + ROWS * cs, 0xFF101826);

        boolean first = true;
        for (int[] p : snake) {
            c.fill(ox + p[0] * cs + 1, oy + p[1] * cs + 1, ox + (p[0] + 1) * cs - 1, oy + (p[1] + 1) * cs - 1,
                    first ? 0xFF7CFC9A : 0xFF2E8B57);
            first = false;
        }
        c.fill(ox + food[0] * cs + 2, oy + food[1] * cs + 2, ox + (food[0] + 1) * cs - 2, oy + (food[1] + 1) * cs - 2, 0xFFE0453A);

        c.drawCenteredTextWithShadow(textRenderer, "Snake  -  Pontos: " + score, width / 2, oy - 16, 0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer, "Setas/WASD | Esc: menu", width / 2, oy + ROWS * cs + 8, 0xFF8FA3C4);
        if (dead) {
            c.drawCenteredTextWithShadow(textRenderer, "Game Over - Enter para recomecar", width / 2, oy + ROWS * cs / 2 - 4, 0xFFFF6B6B);
        }
    }

    private void renderPong(DrawContext c) {
        c.fill(0, 0, width, height, 0xFF0B0F14);
        int x = fx(), y = fy(), w = fw(), h = fh();
        c.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF2B3A55);
        c.fill(x, y, x + w, y + h, 0xFF101826);
        for (int yy = y; yy < y + h; yy += 14) {
            c.fill(x + w / 2 - 1, yy, x + w / 2 + 1, yy + 7, 0xFF3A4A66);
        }
        int ph = (int) ph();
        c.fill(x + 10, (int) py, x + 10 + PW, (int) py + ph, 0xFFFFFFFF);
        c.fill(x + w - 10 - PW, (int) ay, x + w - 10, (int) ay + ph, 0xFFFFFFFF);
        c.fill((int) (bx - BS / 2f), (int) (by - BS / 2f), (int) (bx + BS / 2f), (int) (by + BS / 2f), 0xFFFFD34D);

        c.drawCenteredTextWithShadow(textRenderer, scorePl + "   -   " + scoreAi, width / 2, y - 16, 0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer, "Rato / W S | Esc: menu", width / 2, y + h + 8, 0xFF8FA3C4);
    }
}
