package nu.takacs.partifles;

import nu.takacs.partifles.math.Vec2;
import nu.takacs.partifles.window.WindowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ParticleEngine {
    private static final Logger LOG = LoggerFactory.getLogger(ParticlesWindowListener.class);

    private static final long TTL_MS = 5000;
    private static final long TTL_NS = TTL_MS * 1000000;
    private final int maxParticles;
    private Vec2 position;
    private Particle[] particles;

    private long lastEmissionCycle = 0;

    private int frequency = 200;

    private static class Particle {
        boolean live = false;
        Vec2 position;
        Vec2 velocity;
        long expiryTimestampNs;
    }

    public ParticleEngine(int maxParticles) {
        this.maxParticles = maxParticles;

        this.particles = new Particle[maxParticles];
    }

    public void setPosition(final Vec2 position) {
        this.position = position;
    }

    public void init(final WindowContext windowContext) {
        position = new Vec2(windowContext.getViewPortWidth()/2.0,
                windowContext.getViewPortHeight()/4.0);

        for (int i = 0; i < maxParticles; ++i) {
            particles[i] = new Particle();
        }
    }
    public void render(final WindowContext windowContext, final double deltaSeconds) {
        final long currentNanos = System.nanoTime();

        // Handle emission of new particles
        // Only do this with a certain frequency
        // Should we do this on every render?
        if(currentNanos - lastEmissionCycle > (1000000000/frequency)) {
            for (int i = 0; i < maxParticles; ++i) {
                final Particle particle = particles[i];

                if(!particle.live) {
                    particle.position = new Vec2(
                            (Math.random() - 0.5) * 200,
                            (Math.random() - 0.5) * 10);

                    particle.velocity = new Vec2(
                            (Math.random() - 0.5) * 2.0 * 20.0,
                            10 + Math.random() * 2.0 * 20.0);

                    particle.expiryTimestampNs = currentNanos + TTL_NS;
                    particle.live = true;
                    break;
                }
            }
            lastEmissionCycle = currentNanos;
        }

        // Update and draw live particles
        for (int i = 0; i < maxParticles; ++i) {
            final Particle particle = particles[i];

            if(!particle.live) {
                continue;
            }

            final var timeRemaining = particle.expiryTimestampNs - currentNanos;

            if(timeRemaining <= 0) {
                particle.live = false;
                continue;
            }

            final double alpha = timeRemaining / (double)TTL_NS;

            particle.position.addLocal(particle.velocity.mult(deltaSeconds));

            windowContext.drawGradientCircle((int) (particle.position.x + position.x), (int) particle.position.y,
                    50,
                    (int) (150 * alpha),
                    (int)(60 * alpha),
                    0);
        }
    }
}
