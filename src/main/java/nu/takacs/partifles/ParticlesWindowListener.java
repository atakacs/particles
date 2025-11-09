package nu.takacs.partifles;

import nu.takacs.partifles.math.Vec2;
import nu.takacs.partifles.window.WindowContext;
import nu.takacs.partifles.window.WindowListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParticlesWindowListener implements WindowListener {
    private static final Logger LOG = LoggerFactory.getLogger(ParticlesWindowListener.class);
    private long prevNanos = 0;

    private final ParticleEngine particleEngine = new ParticleEngine(1000);

    @Override
    public void onInit(final WindowContext windowContext) {
        LOG.info("Startup complete");

        particleEngine.init(windowContext);
    }

    @Override
    public void onRender(final WindowContext windowContext) {
        final long currentNanos = System.nanoTime();

        final double deltaSeconds;
        if (prevNanos == 0) {
            deltaSeconds = 0;
        } else {
            deltaSeconds = (currentNanos - prevNanos) / 1000000000.0;
        }

        prevNanos = currentNanos;

        particleEngine.render(windowContext, deltaSeconds);
    }

    @Override
    public void onMouseMove(final Vec2 location) {

    }
}
