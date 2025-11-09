package nu.takacs.partifles.window;

import nu.takacs.partifles.math.Vec2;

public interface WindowListener {
    void onInit(final WindowContext windowContext);
    void onRender(final WindowContext windowContext);
    void onMouseMove(Vec2 location);
}
