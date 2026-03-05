package io.github.some_example_name;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Punto de entrada de la aplicación.
 * Extiende {@link Game} para poder gestionar múltiples {@link com.badlogic.gdx.Screen}s.
 * El {@link SpriteBatch} se crea aquí y se comparte con las pantallas para
 * evitar crear/destruir el batch en cada cambio de pantalla.
 */
public class Main extends Game {

    /** SpriteBatch compartido por todas las pantallas. */
    public SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        // Libera primero la pantalla activa, luego el batch compartido.
        super.dispose();
        batch.dispose();
    }
}
