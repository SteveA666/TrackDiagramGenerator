package io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;

/** Encodes images to a sibling temporary file before replacing the destination. */
public final class DiagramImageIO {
    public enum Format { PNG, JPG }
    private DiagramImageIO() {}

    public static Path withExtension(Path path, Format format) {
        String name = path.getFileName().toString();
        String lower = name.toLowerCase(Locale.ROOT);
        if (format == Format.PNG && lower.endsWith(".png")
                || format == Format.JPG && (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) { return path; }
        if (lower.matches(".*\\.(png|jpg|jpeg)$")) { name = name.substring(0, name.lastIndexOf('.')); }
        return path.resolveSibling(name + (format == Format.PNG ? ".png" : ".jpg"));
    }

    public static void save(BufferedImage image, Path path, Format format, int quality) throws IOException {
        if (quality < 1 || quality > 100) { throw new IllegalArgumentException("JPG quality must be 1–100."); }
        if (format == Format.JPG && image.getColorModel().hasAlpha()) {
            throw new IllegalArgumentException("JPG requires an opaque background.");
        }
        Path target = path.toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".export-", ".tmp");
        try {
            var writers = ImageIO.getImageWritersByFormatName(format.name());
            if (!writers.hasNext()) { throw new IOException("No image encoder available for " + format); }
            ImageWriter writer = writers.next();
            try (ImageOutputStream output = ImageIO.createImageOutputStream(temporary.toFile())) {
                writer.setOutput(output);
                ImageWriteParam parameters = writer.getDefaultWriteParam();
                if (format == Format.JPG) {
                    parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    parameters.setCompressionQuality(quality / 100f);
                }
                writer.write(null, new IIOImage(image, null, null), parameters);
            } finally { writer.dispose(); }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
