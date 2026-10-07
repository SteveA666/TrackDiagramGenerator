package ui;

import java.awt.Rectangle;
import settings.AppSettings;

/** Keep the requested window within the usable area of its current monitor. */
public final class WindowPreferences {
    private WindowPreferences() {}

    public static Rectangle bounds(AppSettings settings, Rectangle usableScreen) {
        settings.validate();
        if (usableScreen.width <= 0 || usableScreen.height <= 0) {
            throw new IllegalArgumentException("The screen must have a usable area.");
        }
        int width = Math.min(settings.windowWidth, usableScreen.width);
        int height = Math.min(settings.windowHeight, usableScreen.height);
        return new Rectangle(usableScreen.x + (usableScreen.width - width) / 2,
                usableScreen.y + (usableScreen.height - height) / 2, width, height);
    }
}
