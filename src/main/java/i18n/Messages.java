package i18n;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.DecimalFormat;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JComponent;
import javax.swing.UIManager;

/** Application messages. The selected locale is fixed before Swing components are built. */
public final class Messages {
    private static final String BASE = "i18n.messages";
    private static final ResourceBundle.Control CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);
    private static final Locale SYSTEM_LOCALE = Locale.getDefault();
    private static Locale locale = SYSTEM_LOCALE;
    private static ResourceBundle bundle = ResourceBundle.getBundle(BASE, locale, CONTROL);

    private Messages() {}

    /** Empty preference means the operating system's locale; unknown bundles use English. */
    public static void initialize(String languageTag) {
        locale = languageTag == null || languageTag.isBlank()
                ? SYSTEM_LOCALE : new Locale.Builder().setLanguageTag(languageTag).build();
        bundle = ResourceBundle.getBundle(BASE, locale, CONTROL);
        Locale.setDefault(Locale.Category.FORMAT, locale);
        JComponent.setDefaultLocale(locale);
        UIManager.getDefaults().setDefaultLocale(locale);
    }

    public static Locale locale() { return locale; }

    public static List<String> supportedLanguageTags() {
        return List.of(ResourceBundle.getBundle(BASE, Locale.ROOT, CONTROL)
                .getString("languages.supported").split(","));
    }

    public static String text(String key, Object... arguments) {
        String pattern = bundle.getString(key);
        return arguments.length == 0 ? pattern : new MessageFormat(pattern, locale).format(arguments);
    }

    /** Parse an entire decimal input, rejecting grouping and trailing junk. */
    public static double decimal(String input) {
        String value = input.trim();
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setGroupingUsed(false);
        ParsePosition position = new ParsePosition(0);
        Number number = format.parse(value, position);
        if (number == null || position.getIndex() != value.length() || !Double.isFinite(number.doubleValue())) {
            throw new IllegalArgumentException(text("input.invalidNumber"));
        }
        return number.doubleValue();
    }

    public static int integer(String input) {
        String value = input.trim();
        DecimalFormat format = (DecimalFormat) NumberFormat.getIntegerInstance(locale);
        format.setGroupingUsed(false);
        format.setParseBigDecimal(true);
        ParsePosition position = new ParsePosition(0);
        Number number = format.parse(value, position);
        if (number instanceof BigDecimal decimal && position.getIndex() == value.length()) {
            try { return decimal.intValueExact(); }
            catch (ArithmeticException ignored) { /* Report overflow like any other invalid input. */ }
        }
        throw new IllegalArgumentException(text("input.invalidInteger"));
    }

    public static String decimal(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setGroupingUsed(false);
        format.setMaximumFractionDigits(340);
        return format.format(value);
    }

    public static String enumLabel(Enum<?> value) {
        return text("enum." + value.getDeclaringClass().getSimpleName() + "." + value.name());
    }
}
