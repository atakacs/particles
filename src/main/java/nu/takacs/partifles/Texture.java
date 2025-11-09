package nu.takacs.partifles;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Texture {
    private final List<BufferedImage> frameImages;
    private final String filePath;

    public static final String RESOURCE_PATH_PREFIX = "partifles/";

    public Texture(final String filePath,
                   final int numFrames,
                   final int numXFrames,
                   final int numYFrames,
                   final int frameWidth,
                   final int frameHeight) {
        this.filePath = filePath;

        try {
            final var ioStream =
                    ClassLoader.getSystemResourceAsStream(RESOURCE_PATH_PREFIX + filePath);

            if(ioStream == null) {
                throw new RuntimeException("Failed to load image");
            }

            final BufferedImage spriteSetImage = ImageIO.read(ioStream);

            if(spriteSetImage.getWidth() != numXFrames*frameWidth) {
                throw new RuntimeException("Unexpected frame width");
            }

            if(spriteSetImage.getHeight() != numYFrames*frameHeight) {
                throw new RuntimeException("Unexpected frame height");
            }

            frameImages = new ArrayList<>(numXFrames * numYFrames);

            for (int y = 0; y < numYFrames; y++) {
                for (int x = 0; x < numXFrames; x++) {
                    // Check for blank frames at the end
                    if(frameImages.size() == numFrames) {
                        break;
                    }

                    frameImages.add(
                            spriteSetImage
                                    .getSubimage(x * frameWidth, y * frameHeight,
                                            frameWidth, frameHeight));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load sprite " + filePath, e);
        }
    }

    public Texture(final String filePath) {
        this.filePath = filePath;

        try {
            final var ioStream =
                    ClassLoader.getSystemResourceAsStream(RESOURCE_PATH_PREFIX + filePath);

            if(ioStream == null) {
                throw new RuntimeException("Failed to load image");
            }

            final BufferedImage spriteSetImage = ImageIO.read(ioStream);
            frameImages = List.of(spriteSetImage);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load sprite " + filePath);
        }
    }

    public BufferedImage getFrame(final int i) {
        return frameImages.get(i);
    }

    public int getNumFrames() {
        return frameImages.size();
    }

    public String getFilePath() {
        return filePath;
    }
}
