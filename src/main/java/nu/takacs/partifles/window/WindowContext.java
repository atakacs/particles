package nu.takacs.partifles.window;

public interface WindowContext {
    void drawGradient();
    void drawRect(int x1, int y1, int width, int height, int r, int g, int b);
    void drawLine(int x1, int y1, int x2, int y2, int width, int r, int g, int b);
    void drawColumn(int x, int y, byte[] pixels);

    void drawGradientCircle(int x, int y, int radius, int r, int g, int b);

    void drawColumn(int x, int y, int height, int r, int g, int b);

    int getViewPortWidth();
    int getViewPortHeight();

    boolean isKeyDown(Key key);
}
