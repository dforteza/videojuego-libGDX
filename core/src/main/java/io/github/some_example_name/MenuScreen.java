package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Pantalla de menú principal.
 *
 * Muestra el título del juego, las instrucciones de control y los distintos
 * tipos de gota. Pulse ESPACIO o haga clic/toque para empezar a jugar.
 */
public class MenuScreen implements Screen {

    private static final int SCREEN_W = 640;
    private static final int SCREEN_H = 480;

    private final Main game;

    private Texture            bgTexture;
    private Texture            logoTexture;
    private Texture            dropTexture;
    private BitmapFont         titleFont;
    private BitmapFont         bodyFont;
    private OrthographicCamera camera;

    public MenuScreen(Main game) {
        this.game = game;
    }

    // ==========================================================================
    // Ciclo de vida de Screen
    // ==========================================================================

    @Override
    public void show() {
        bgTexture   = new Texture("background.png");
        logoTexture = new Texture("libgdx.png");
        dropTexture = new Texture("drop.png");

        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.5f);

        bodyFont = new BitmapFont();
        bodyFont.getData().setScale(1.4f);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, SCREEN_W, SCREEN_H);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0.15f, 1f);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        drawMenu();
        game.batch.end();

        // Navegar al juego
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.justTouched()) {
            game.setScreen(new GameScreen(game));
        }
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause()  {}
    @Override public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        bgTexture.dispose();
        logoTexture.dispose();
        dropTexture.dispose();
        titleFont.dispose();
        bodyFont.dispose();
    }

    // ==========================================================================
    // Dibujo
    // ==========================================================================

    private void drawMenu() {
        // Fondo
        game.batch.draw(bgTexture, 0, 0, SCREEN_W, SCREEN_H);

        // Logo centrado
        game.batch.draw(logoTexture, SCREEN_W / 2f - 48, SCREEN_H - 110, 96, 96);

        // Título
        titleFont.setColor(Color.CYAN);
        titleFont.draw(game.batch, "RAIN DROP CATCHER", 70, SCREEN_H - 120);

        // ── Cómo jugar ─────────────────────────────────────────────────────────
        bodyFont.setColor(Color.WHITE);
        bodyFont.draw(game.batch, "COMO JUGAR:",                                60, 300);
        bodyFont.draw(game.batch, "  <- ->  /  A D     mover el cubo",          60, 278);
        bodyFont.draw(game.batch, "  Clic / Toque      seguir el raton/dedo",   60, 257);
        bodyFont.draw(game.batch, "  Atrapa gotas para sumar puntos",            60, 236);
        bodyFont.draw(game.batch, "  Perder 3 gotas = GAME OVER",                60, 215);
        bodyFont.draw(game.batch, "  Cada 10 puntos el nivel sube de velocidad", 60, 194);

        // ── Tipos de gota ──────────────────────────────────────────────────────
        bodyFont.setColor(Color.WHITE);
        bodyFont.draw(game.batch, "TIPOS DE GOTA:", 60, 168);

        // Gota normal: icono + texto
        game.batch.setColor(Color.WHITE);
        game.batch.draw(dropTexture, 60, 110, 20, 20);
        bodyFont.setColor(Color.WHITE);
        bodyFont.draw(game.batch, "Blanca   - Normal  (+1 pto)",  86, 130);

        // Gota rápida
        game.batch.setColor(Color.RED);
        game.batch.draw(dropTexture, 60, 90, 20, 20);
        bodyFont.setColor(Color.RED);
        bodyFont.draw(game.batch, "Roja  - Rapida  (+2 ptos, mas velocidad)", 86, 110);

        // Gota bonus
        game.batch.setColor(Color.GOLD);
        game.batch.draw(dropTexture, 60, 70, 20, 20);
        bodyFont.setColor(Color.GOLD);
        bodyFont.draw(game.batch, "Dorada   - Bonus   (+5 ptos, cae lento)",    86, 90);

        // Restaurar color del batch
        game.batch.setColor(Color.WHITE);

        // ── Llamada a la acción ────────────────────────────────────────────────
        bodyFont.setColor(Color.YELLOW);
        bodyFont.draw(game.batch, "Pulsa ESPACIO o haz clic para jugar", 115, 55);
    }
}
