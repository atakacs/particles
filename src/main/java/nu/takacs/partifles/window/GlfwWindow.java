package nu.takacs.partifles.window;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray;
import static org.lwjgl.opengl.ARBVertexArrayObject.glGenVertexArrays;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class GlfwWindow {

    private static final Logger LOG = LoggerFactory.getLogger(GlfwWindow.class);

    private static final String VERTEX_SHADER_SOURCE =
            "#version 400\n"
                    + "layout (location = 0) in vec3 position;"
                    + "layout (location = 1) in vec2 texCoord;"

                    + "uniform mat4 projection;"

                    + "out vec2 TexCoord;"

                    + "void main() {"
                    + "  gl_Position = projection * vec4(position, 1.0);"
                    + "  TexCoord = texCoord;"
                    + "}";

    private static final String FRAGMENT_SHADER_SOURCE =
            "#version 400\n"
                    + "in vec2 TexCoord;"

                    + "uniform sampler2D texture_sampler;"

                    + "out vec4 frag_colour;"

                    + "void main() {"
                    + "  frag_colour = texture(texture_sampler, TexCoord);"
                    //+ "  frag_colour = vec4(TexCoord, 0.0, 1.0);"
                    + "}";

    // TODO: flip around texture coordinates to make y coords sane
    private static final float SQUARE_POINTS[] = {
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
            1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f
    };

    private static final float IDENTITY_MATRIX[] = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    private Map<Key, Boolean> inputKeyStates;

    private class GlfwWindowContext implements WindowContext {
        private GlfwWindowContext() {
        }

        @Override
        public void drawGradient() {
            final int textureNumPixels = viewPortWidth * viewPortHeight;
            for (int i = 0; i < textureNumPixels; ++i) {
                frameBuffer.put(i * 3, (byte) (255 * ((i % viewPortWidth) / (float) viewPortWidth))); // Red
                frameBuffer.put(i * 3 + 1, (byte) 0x00); // Green
                frameBuffer.put(i * 3 + 2, (byte) 0x00); // Blue
            }
        }

        @Override
        public void drawRect(final int x, final int y,
                             final int width, final int height,
                             final int r, final int g, final int b) {
            // TODO: range checks

            // Going row by row as it fits our data structure
            for (int row = y; row < y + height; ++row) {
                for (int i = (row * viewPortWidth + x); i < (row * viewPortWidth + x + width); ++i) {
                    if (0 <= i && i < numPixels) {
                        frameBuffer.put(i * 3, (byte) (r & 0xFF));
                        frameBuffer.put(i * 3 + 1, (byte) (g & 0xFF));
                        frameBuffer.put(i * 3 + 2, (byte) (b & 0xFF));
                    }
                }
            }
        }

        @Override
        public void drawLine(
                final int x1, final int y1, final int x2, final int y2,
                final int width,
                final int r, final int g, final int b) {
            // TODO: remove multiplication and other optimzations
            // TODO: thickness

            // Compute slope k
            // Draw line by line
            // Maybe draw from the other axis when slope is above 1 for more consistent thickness?

            final float k = Math.abs(y2 - y1) / (float) Math.abs(x1 - x2);

            if (Float.isInfinite(k) || k > 1.0f) {
                drawLineAlongY(x1, y1, x2, y2, width, r, g, b);
            } else {
                drawLineAlongX(x1, y1, x2, y2, width, r, g, b);
            }
        }

        @Override
        public void drawColumn(final int x, final int y, final byte[] pixels) {
            int j = (y * viewPortWidth + x) * 3;

            for (int i = 0; i < pixels.length; i += 3) {
                frameBuffer.put(j, pixels[i]);
                frameBuffer.put(j + 1, pixels[i + 1]);
                frameBuffer.put(j + 2, pixels[i + 2]);

                j += viewPortWidth * 3;
            }
        }

        @Override
        public void drawGradientCircle(int x, int y, int radius, int r, int g, int b) {
            for(int row = Math.max(0, y - radius); row < viewPortHeight && row < (y + radius); ++row) {
                final int dy = Math.abs(y - row);

                final int w = (int) Math.sqrt(radius * radius - dy * dy);

                for (int col = Math.max(0, x - w); col < x + w && col < viewPortWidth; ++col) {
                    double dist = Math.sqrt((col - x) * (col - x) + dy * dy) / radius;
                    double alpha = 1.0 - dist;

                    alphaBlend((row * viewPortWidth + col) * 3, alpha, r);
                    alphaBlend((row * viewPortWidth + col) * 3 + 1, alpha, g);
                    alphaBlend((row * viewPortWidth + col) * 3 + 2, alpha, b);
                }
            }
        }

        private void alphaBlend(final int i, final double a, final int color) {
            final int blended = (int)(a * color + (1.0 - a) * Byte.toUnsignedInt(frameBuffer.get(i)));
            frameBuffer.put(i, (byte) (blended & 0xFF));
        }

        @Override
        public void drawColumn(final int x, final int y, final int height, int r, int g, int b) {

            int j = (y * viewPortWidth + x) * 3;
            for (int i = 0; i < height; ++i) {
                frameBuffer.put(j, (byte) (r & 0xFF));
                frameBuffer.put(j + 1, (byte) (g & 0xFF));
                frameBuffer.put(j + 2, (byte) (b & 0xFF));

                j += viewPortWidth * 3;
            }
        }

        @Override
        public int getViewPortWidth() {
            return viewPortWidth;
        }

        @Override
        public int getViewPortHeight() {
            return viewPortHeight;
        }

        @Override
        public boolean isKeyDown(final Key key) {
            return inputKeyStates.get(key);
        }
    }

    private void drawLineAlongX(
            final int x1, final int y1, final int x2, final int y2,
            final int width,
            final int r, final int g, final int b) {
        int xa;
        int ya;
        int xb;
        int yb;

        if (x1 < x2) {
            xa = x1;
            ya = y1;
            xb = x2;
            yb = y2;
        } else {
            xa = x2;
            ya = y2;
            xb = x1;
            yb = y1;
        }

        final float k = (yb - ya) / (float) (xb - xa);

        int dx = (xb - xa);

        for (int ix = 0; ix < dx; ++ix) {
            float y = ya + ix * k;
            int x = xa + ix;
            int i = x + (int) (y) * viewPortWidth;

            for (int j = Math.max(0, i - width / 2); j <= i + width / 2 && j < viewPortWidth * viewPortHeight; ++j) {
                frameBuffer.put(j * 3, (byte) (r & 0xFF));
                frameBuffer.put(j * 3 + 1, (byte) (g & 0xFF));
                frameBuffer.put(j * 3 + 2, (byte) (b & 0xFF));
            }
        }
    }

    private void drawLineAlongY(
            final int x1, final int y1, final int x2, final int y2,
            final int width,
            final int r, final int g, final int b) {
        int xa;
        int ya;
        int xb;
        int yb;

        if (y1 < y2) {
            xa = x1;
            ya = y1;
            xb = x2;
            yb = y2;
        } else {
            xa = x2;
            ya = y2;
            xb = x1;
            yb = y1;
        }

        final float k = (xb - xa) / (float) (yb - ya);

        for (float i = ya * viewPortWidth + xa; i < yb * viewPortWidth; i += k + viewPortWidth) {
            //TODO: fix the sides, dont' want to wrap around
            for (int j = Math.max(0, (int) i - width / 2); j <= (int) i + width / 2 && j < viewPortWidth * viewPortHeight; ++j) {
                frameBuffer.put(j * 3, (byte) (r & 0xFF));
                frameBuffer.put(j * 3 + 1, (byte) (g & 0xFF));
                frameBuffer.put(j * 3 + 2, (byte) (b & 0xFF));
            }
        }
    }

    private final WindowListener windowListener;
    private final WindowContext windowContext;
    private final int windowWidth;
    private final int windowHeight;
    private final int viewPortHeight;
    private final int viewPortWidth;
    private final int numPixels;
    private final String title;
    private ByteBuffer frameBuffer;

    private long window;

    public GlfwWindow(final WindowListener windowListener,
                      final int windowWidth,
                      final int windowHeight,
                      final int viewPortWidth,
                      final int viewPortHeight,
                      final String title) {
        this.windowListener = windowListener;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.viewPortWidth = viewPortWidth;
        this.viewPortHeight = viewPortHeight;
        this.numPixels = viewPortWidth * viewPortHeight; // Pre-computation
        this.title = title;
        windowContext = new GlfwWindowContext();

        inputKeyStates = new HashMap<>();
        Arrays.stream(Key.values())
                .forEach(key -> inputKeyStates.put(key, false));
    }

    public void init() {
        this.frameBuffer = ByteBuffer.allocateDirect(numPixels * 3);

        Configuration.DEBUG.set(true);

        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();
        //GLFWErrorCallback.createThrow().set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        //glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(windowWidth, windowHeight, title, NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {

            if (action == GLFW_PRESS || action == GLFW_RELEASE) {
                switch (key) {
                    case GLFW_KEY_W:
                        inputKeyStates.put(Key.W, action == GLFW_PRESS);
                        break;
                    case GLFW_KEY_A:
                        inputKeyStates.put(Key.A, action == GLFW_PRESS);
                        break;
                    case GLFW_KEY_S:
                        inputKeyStates.put(Key.S, action == GLFW_PRESS);
                        break;
                    case GLFW_KEY_D:
                        inputKeyStates.put(Key.D, action == GLFW_PRESS);
                        break;
                    case GLFW_KEY_SPACE:
                        inputKeyStates.put(Key.SPACE, action == GLFW_PRESS);
                        break;
                }
            }

            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            }
        });

        //glfwSetWindowCloseCallback(window, (window) -> {
        //    glfwSetWindowShouldClose(window, true);
        //});

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            final var pWidth = stack.mallocInt(1); // int*
            final var pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            final var vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        windowListener.onInit(windowContext);

        // Make the window visible
        glfwShowWindow(window);
    }

    public void start() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
        // Not supported by my opengl version?
        //Callback debugProc = GLUtil.setupDebugMessageCallback();

        //LOG.info("OpenGL version: {}", glGetString(GL_VERSION));

        final var texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        final var buffers = new int[]{0};
        final var vertexArrays = new int[]{0};

        glGenBuffers(buffers);
        glBindBuffer(GL_ARRAY_BUFFER, buffers[0]);
        glBufferData(GL_ARRAY_BUFFER, SQUARE_POINTS, GL_STATIC_DRAW);

        glGenVertexArrays(vertexArrays);
        glBindVertexArray(vertexArrays[0]);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * 4, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * 4, 3 * 4);

        checkGlErrors("glVertexAttribPointer");

        final int shaderProgram = createRectangleShaderProgram();

        // Set the clear color
        glClearColor(0.5f, 0.5f, 1.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {

            frameBuffer.clear(); // TODO: what does this actually do?
            for (int i = 0; i < viewPortWidth * viewPortHeight * 3; ++i) {
                frameBuffer.put(i, (byte) 0);
            }

            // Execute software rendering logic
            windowListener.onRender(windowContext);

            frameBuffer.flip();

            // https://stackoverflow.com/questions/67813361/opengl-lwjgl-texture-creation-sigsegv
            // https://learnopengl.com/Getting-started/Textures
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, viewPortWidth, viewPortHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, frameBuffer);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
            checkGlErrors("glClear");

            glUseProgram(shaderProgram);
            checkGlErrors("glUseProgram");

            final int textureSamplerUniform = glGetUniformLocation(shaderProgram, "texture_sampler");
            checkGlErrors("glGetUniformLocation");
            glUniform1i(textureSamplerUniform, 0);
            checkGlErrors("glUniform1i");

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, texture);
            checkGlErrors("glBindTexture");

            glBindVertexArray(vertexArrays[0]);
            checkGlErrors("glBindVertexArray");

            final int uniProjection = glGetUniformLocation(shaderProgram, "projection");

            //final var m = orthographicProjectionMatrix();
            final var m = IDENTITY_MATRIX;
            glUniformMatrix4fv(uniProjection, true, m);

            glBindVertexArray(vertexArrays[0]);
            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);

            checkGlErrors("glEnableVertexAttribArray");

            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();

            //checkGlErrors();
        }

        terminate();
    }

    private void checkGlErrors(final String operation) {
        int glErrorCode;
        while ((glErrorCode = glGetError()) != GL_NO_ERROR) {
            LOG.error("Got GL error after {} code: 0x{} ", operation, Integer.toHexString(glErrorCode));
            throw new RuntimeException();
        }
    }

    private int createRectangleShaderProgram() {
        final int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, VERTEX_SHADER_SOURCE);
        glCompileShader(vs);

        final var vertexCompiled = new int[1];
        glGetShaderiv(vs, GL_COMPILE_STATUS, vertexCompiled);
        if (vertexCompiled[0] != GL_TRUE) {
            throw new RuntimeException("Vertex shader compilation error: " + glGetShaderInfoLog(vs));
        }

        final int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, FRAGMENT_SHADER_SOURCE);
        glCompileShader(fs);

        final var fragmentCompiled = new int[1];
        glGetShaderiv(fs, GL_COMPILE_STATUS, fragmentCompiled);
        if (fragmentCompiled[0] != GL_TRUE) {
            throw new RuntimeException("Fragment shader compilation error: " + glGetShaderInfoLog(fs));
        }

        int shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, fs);
        glAttachShader(shaderProgram, vs);
        glLinkProgram(shaderProgram);

        return shaderProgram;
    }

    public void terminate() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}
