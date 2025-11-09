package nu.takacs.partifles;

import nu.takacs.partifles.window.GlfwWindow;

/**
 * Hello world!
 *
 */
public class App 
{

    public static void main( String[] args )
    {
        final var listener = new ParticlesWindowListener();
        final var window = new GlfwWindow(listener, 1600, 1200, 800, 600, "Rayworld");
        window.init();
        window.start();
    }
}
