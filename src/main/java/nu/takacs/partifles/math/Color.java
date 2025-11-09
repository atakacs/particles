package nu.takacs.partifles.math;

import java.beans.ConstructorProperties;

public class Color {
    public static final Color RED = new Color(255, 0, 0);

    public final int r;
    public final int g;
    public final int b;

    @ConstructorProperties({"r", "g", "b"})
    public Color(final int r, final int g, final int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
}
