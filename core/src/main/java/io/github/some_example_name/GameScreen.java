package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Pantalla de juego principal.
 *
 * Mecánica:
 *  - Mueve el cubo (← → / A D / ratón) para atrapar las gotas que caen.
 *  - Cada tipo de gota cae a distinta velocidad y otorga distintos puntos.
 *  - Cada 10 puntos sube el nivel: mayor velocidad base e intervalo de spawn
 *    más corto, lo que aumenta la dificultad progresivamente.
 *  - Dejar escapar una gota resta 1 vida; al llegar a 0 es GAME OVER.
 *  - En GAME OVER: [R] reinicia, [M] vuelve al menú.
 */
public class GameScreen implements Screen {

    // ── Tipos de gota ─────────────────────────────────────────────────────────

    /**
     * Define los tres tipos de gota disponibles.
     * Cada tipo tiene un multiplicador de velocidad, puntos al atraparla
     * y un color de tinte para el sprite.
     */
    private enum DropType {
        /** Gota normal: velocidad base, vale 1 punto. Probabilidad 70 %. */
        NORMAL(1.0f, 1, Color.WHITE),
        /** Gota rápida: cae más deprisa, vale 2 puntos. Probabilidad 20 %. */
        FAST  (1.8f, 2, Color.RED),
        /** Gota bonus: cae despacio pero vale 5 puntos. Probabilidad 10 %. */
        BONUS (0.6f, 5, Color.GOLD);

        /** Multiplicador aplicado sobre la velocidad base actual. */
        final float speedMult;
        /** Puntos otorgados al atrapar esta gota. */
        final int   points;
        /** Color de tinte del sprite. */
        final Color color;

        DropType(float speedMult, int points, Color color) {
            this.speedMult = speedMult;
            this.points    = points;
            this.color     = color;
        }
    }

    // ── Clase interna Drop ────────────────────────────────────────────────────

    /** Representa una gota activa en pantalla: posición (AABB) + tipo. */
    private static class Drop {
        final Rectangle rect;
        final DropType  type;

        Drop(float x, float y, DropType type) {
            rect      = new Rectangle(x, y, DROP_W, DROP_H);
            this.type = type;
        }
    }

    // ── Constantes ────────────────────────────────────────────────────────────

    private static final int   SCREEN_W               = 640;
    private static final int   SCREEN_H               = 480;
    private static final int   BUCKET_W               = 80;
    private static final int   BUCKET_H               = 70;
    private static final int   DROP_W                 = 32;
    private static final int   DROP_H                 = 32;
    private static final float BUCKET_SPEED           = 400f;
    private static final int   MAX_LIVES              = 3;

    // Dificultad progresiva
    private static final float BASE_DROP_SPEED        = 200f;
    private static final float SPEED_GAIN_PER_LEVEL   = 30f;
    private static final float BASE_SPAWN_INTERVAL    = 1.2f;
    private static final float SPAWN_REDUCE_PER_LEVEL = 0.08f;
    private static final float MIN_SPAWN_INTERVAL     = 0.35f;
    private static final int   POINTS_PER_LEVEL       = 10;

    // ── Referencia al juego ───────────────────────────────────────────────────

    private final Main game;

    // ── Assets ────────────────────────────────────────────────────────────────

    private Texture    bgTexture;
    private Texture    bucketTexture;
    private Texture    dropTexture;
    private Sound      dropSound;
    private Music      bgMusic;
    private BitmapFont font;

    // ── Cámara ────────────────────────────────────────────────────────────────

    /** Cámara ortogonal: 1 unidad = 1 píxel, origen abajo-izquierda. */
    private OrthographicCamera camera;

    // ── Estado del juego ──────────────────────────────────────────────────────

    private Rectangle  bucket;
    private Array<Drop> drops;
    private float spawnTimer;
    private int   score;
    private int   lives;
    private int   level;
    private float currentDropSpeed;
    private float currentSpawnInterval;
    private boolean gameOver;

    // Vector auxiliar reutilizable (evita allocations en render)
    private final Vector3 touchPos = new Vector3();

    // ── Constructor ───────────────────────────────────────────────────────────

    public GameScreen(Main game) {
        this.game = game;
    }

    // ==========================================================================
    // Ciclo de vida de Screen
    // ==========================================================================

    @Override
    public void show() {
        bgTexture     = new Texture("background.png");
        bucketTexture = new Texture("bucket.png");
        dropTexture   = new Texture("drop.png");

        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
        bgMusic   = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));

        font = new BitmapFont();
        font.getData().setScale(1.5f);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, SCREEN_W, SCREEN_H);

        bgMusic.setLooping(true);
        bgMusic.setVolume(0.5f);
        bgMusic.play();

        resetGame();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0.15f, 1f);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        drawWorld();
        drawHUD();
        game.batch.end();

        if (gameOver) {
            handleGameOverInput();
            return;
        }

        handleInput(delta);
        spawnDrops(delta);
        updateDrops(delta);
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
        bucketTexture.dispose();
        dropTexture.dispose();
        dropSound.dispose();
        bgMusic.dispose();
        font.dispose();
    }

    // ==========================================================================
    // Dibujo
    // ==========================================================================

    /** Dibuja el fondo, el cubo y todas las gotas con su color de tinte. */
    private void drawWorld() {
        game.batch.draw(bgTexture, 0, 0, SCREEN_W, SCREEN_H);
        game.batch.draw(bucketTexture, bucket.x, bucket.y, BUCKET_W, BUCKET_H);

        for (Drop drop : drops) {
            game.batch.setColor(drop.type.color);
            game.batch.draw(dropTexture, drop.rect.x, drop.rect.y, DROP_W, DROP_H);
        }
        // Restaurar color blanco para el HUD
        game.batch.setColor(Color.WHITE);
    }

    /** Dibuja el HUD (puntuación, vidas, nivel) y el overlay de Game Over. */
    private void drawHUD() {
        font.setColor(Color.WHITE);
        font.draw(game.batch, "Score: " + score, 10, SCREEN_H - 10);
        font.draw(game.batch, "Lives: " + lives,  10, SCREEN_H - 36);
        font.draw(game.batch, "Nivel: " + level,  10, SCREEN_H - 62);

        if (gameOver) {
            font.setColor(Color.RED);
            font.draw(game.batch, "GAME OVER!",
                SCREEN_W / 2f - 75, SCREEN_H / 2f + 48);
            font.setColor(Color.WHITE);
            font.draw(game.batch, "Puntuacion final: " + score,
                SCREEN_W / 2f - 115, SCREEN_H / 2f + 18);
            font.setColor(Color.YELLOW);
            font.draw(game.batch, "[R] Reiniciar     [M] Menu",
                SCREEN_W / 2f - 115, SCREEN_H / 2f - 18);
            font.setColor(Color.WHITE);
        }
    }

    // ==========================================================================
    // Input
    // ==========================================================================

    /**
     * Mueve el cubo con teclado (← →, A D) o siguiendo la posición horizontal
     * del ratón/toque. Usa delta time para independencia del hardware.
     */
    private void handleInput(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)  || Gdx.input.isKeyPressed(Input.Keys.A)) {
            bucket.x -= BUCKET_SPEED * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            bucket.x += BUCKET_SPEED * delta;
        }

        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);
            bucket.x = touchPos.x - BUCKET_W / 2f;
        }

        bucket.x = MathUtils.clamp(bucket.x, 0, SCREEN_W - BUCKET_W);
    }

    /** Tras Game Over: [R] reinicia la partida, [M] vuelve al menú. */
    private void handleGameOverInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetGame();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    // ==========================================================================
    // Lógica del juego
    // ==========================================================================

    /** Deja todas las variables en el estado inicial de partida. */
    private void resetGame() {
        drops                = new Array<>();
        spawnTimer           = 0f;
        score                = 0;
        lives                = MAX_LIVES;
        level                = 1;
        currentDropSpeed     = BASE_DROP_SPEED;
        currentSpawnInterval = BASE_SPAWN_INTERVAL;
        gameOver             = false;
        bucket = new Rectangle(SCREEN_W / 2f - BUCKET_W / 2f, 20f, BUCKET_W, BUCKET_H);
    }

    /**
     * Recalcula velocidad base e intervalo de spawn según la puntuación actual.
     * Se llama cada vez que el jugador atrapa una gota.
     */
    private void updateDifficulty() {
        int newLevel = score / POINTS_PER_LEVEL + 1;
        if (newLevel > level) {
            level                = newLevel;
            currentDropSpeed     = BASE_DROP_SPEED + SPEED_GAIN_PER_LEVEL * (level - 1);
            currentSpawnInterval = Math.max(
                MIN_SPAWN_INTERVAL,
                BASE_SPAWN_INTERVAL - SPAWN_REDUCE_PER_LEVEL * (level - 1)
            );
        }
    }

    /**
     * Elige un tipo de gota aleatoriamente:
     *   70 % NORMAL  ·  20 % FAST  ·  10 % BONUS
     */
    private DropType randomDropType() {
        float r = MathUtils.random();
        if (r < 0.10f) return DropType.BONUS;
        if (r < 0.30f) return DropType.FAST;
        return DropType.NORMAL;
    }

    /** Genera una nueva gota en X aleatoria cada {@code currentSpawnInterval} segundos. */
    private void spawnDrops(float delta) {
        spawnTimer += delta;
        if (spawnTimer >= currentSpawnInterval) {
            float x = MathUtils.random(0f, SCREEN_W - DROP_W);
            drops.add(new Drop(x, SCREEN_H, randomDropType()));
            spawnTimer = 0f;
        }
    }

    /**
     * Mueve cada gota hacia abajo (delta time × velocidad base × multiplicador de tipo).
     * Detecta colisión AABB con el cubo y salida por la parte inferior.
     */
    private void updateDrops(float delta) {
        for (int i = drops.size - 1; i >= 0; i--) {
            Drop drop = drops.get(i);
            // Movimiento independiente del hardware gracias al delta time
            drop.rect.y -= currentDropSpeed * drop.type.speedMult * delta;

            if (drop.rect.overlaps(bucket)) {
                // Gota atrapada: puntúa y ajusta dificultad
                dropSound.play();
                score += drop.type.points;
                drops.removeIndex(i);
                updateDifficulty();
            } else if (drop.rect.y + DROP_H < 0) {
                // Gota perdida
                lives--;
                drops.removeIndex(i);
                if (lives <= 0) {
                    gameOver = true;
                }
            }
        }
    }
}
