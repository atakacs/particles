package nu.takacs.partifles.math;

public class Vec2 {
    public double x;
    public double y;

    public Vec2() {
        x = 0f;
        y = 0f;
    }

    public Vec2(final double x, final double y) {
        this.x = x;
        this.y = y;
    }

    public void set(final double x, final double y) {
        this.x = x;
        this.y = y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public static Vec2 shortest(final Vec2... vectors) {
        Vec2 shortestV = null;
        double shortestLengthSquared = Double.MAX_VALUE;

        for (Vec2 v : vectors) {
            if (v == null) {
                continue;
            }

            final double squaredLength = v.x * v.x + v.y * v.y;
            if (shortestV == null || squaredLength < shortestLengthSquared) {
                shortestV = v;
                shortestLengthSquared = squaredLength;
            }
        }

        return shortestV;
    }

    public Vec2 copy() {
        return new Vec2(x, y);
    }

    public Vec2 mult(final double a) {
        return new Vec2(x * a, y * a);
    }

    public Vec2 multLocal(final double a) {
        x = x * a;
        y = y * a;

        return this;
    }

    public Vec2 add(final Vec2 v) {
        return new Vec2(x + v.x, y + v.y);
    }

    public Vec2 addLocal(final Vec2 v) {
        x = x + v.x;
        y = y + v.y;

        return this;
    }

    public Vec2 sub(final Vec2 v) {
        return new Vec2(x - v.x, y - v.y);
    }

    public Vec2 sub(final double x2, final double y2) {
        return new Vec2(x - x2, y - y2);
    }

    public Vec2 subLocal(final Vec2 v) {
        x = x - v.x;
        y = y - v.y;

        return this;
    }

    public Vec2 normalize() {
        final var length = (double) Math.sqrt(x * x + y * y);
        return new Vec2(x / length, y / length);
    }

    public Vec2 normalizeLocal() {
        final var length = (double) Math.sqrt(x * x + y * y);
        x = x / length;
        y = y / length;

        return this;
    }

    public double polarAngleRadians() {
        if (x == 0) {
            return 0;
        }

        return (double) Math.atan2(-y, x);
    }

    public double polarAngleDegrees() {
        if (x == 0) {
            return 0;
        }

        final double t = (double) (-Math.atan2(-y, x) * (180 / Math.PI));

        if (t < 0) {
            return 360 + t;
        }

        return t;
    }

    public double length() {
        return (double) Math.sqrt(x * x + y * y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
