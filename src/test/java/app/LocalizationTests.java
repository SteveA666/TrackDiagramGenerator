package app;

import editor.DiagramEditor;
import i18n.Messages;
import io.DiagramIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;
import model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import settings.*;
import ui.SettingsPanel;
import ui.DiagramPanel;
import ui.EditorToolbar;
import ui.StartupPanel;
import ui.LanguagePicker;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.*;

class LocalizationTests {
    @TempDir Path temporary;
    private final Locale originalDefault = Locale.getDefault();
    private final Locale originalFormat = Locale.getDefault(Locale.Category.FORMAT);

    @AfterEach void restoreLocale() {
        Locale.setDefault(originalDefault);
        Messages.initialize("");
        Locale.setDefault(Locale.Category.FORMAT, originalFormat);
    }

    @Test void loadsUtf8AndFormatsReorderedArgumentsWithApostrophes() {
        Messages.initialize("qaa");
        assertEquals("Générateur de voies — 日本語", Messages.text("app.name"));
        assertEquals("Destination B; l'origine A.", Messages.text("test.arguments", "A", "B"));
        assertEquals("Save", Messages.text("menuBar.save"));
    }

    @Test void unsupportedLocaleDoesNotFallBackToAnotherSystemLanguage() {
        Locale.setDefault(Locale.forLanguageTag("qaa"));
        Messages.initialize("de-DE");
        assertEquals("Track Diagram Generator", Messages.text("app.name"));
        assertEquals("de-DE", Messages.locale().toLanguageTag());
    }

    @Test void parsesLocalizedDecimalsAndRejectsPartialOrGroupedInput() {
        Messages.initialize("de-DE");
        assertEquals(8.25, Messages.decimal("8,25"));
        assertEquals("8,25", Messages.decimal(8.25));
        assertEquals(12, Messages.integer("12"));
        for (String input : List.of("", "1.234", "8,25junk", "NaN", "∞")) {
            assertThrows(IllegalArgumentException.class, () -> Messages.decimal(input), input);
        }
        for (String input : List.of("", "1,5", "2147483648", "4junk")) {
            assertThrows(IllegalArgumentException.class, () -> Messages.integer(input), input);
        }
    }

    @Test void localeNeverChangesSavedDiagramDataAndHistoryUsesMessageKeys() throws Exception {
        Messages.initialize("en");
        DiagramEditor editor = new DiagramEditor(new Network());
        editor.createNode(10, 20, NodeType.REGULAR);
        editor.createStation("東京 — Gare", 30, 40);
        String json = DiagramIO.toJson(editor.getNetwork());
        Messages.initialize("qaa");
        assertEquals(json, DiagramIO.toJson(editor.getNetwork()));
        editor.getHistory().undo();
        assertEquals("Ajouter un nœud", editor.getHistory().undoName());
        editor.getHistory().redo();
        assertEquals(json, DiagramIO.toJson(editor.getNetwork()));
    }

    @Test void languagePreferenceSurvivesSettingsAndFormRoundTrips() throws Exception {
        Messages.initialize("en");
        SettingsStore store = new SettingsStore(temporary.resolve("settings.json"));
        AppSettings settings = store.loadDefaults();
        assertEquals("", settings.languageTag);
        settings.languageTag = "fr-CA";
        store.save(settings);
        assertEquals("fr-CA", store.load().languageTag);
        SwingUtilities.invokeAndWait(() -> assertEquals("fr-CA", new SettingsPanel(settings).readSettings().languageTag));
        settings.languageTag = "not_a_tag";
        assertThrows(IllegalArgumentException.class, settings::validate);
    }

    @Test void legacySettingsStillUseSystemLanguage() throws Exception {
        Files.writeString(temporary.resolve("settings.json"), "{\"gridSpacing\":25}");
        AppSettings settings = new SettingsStore(temporary.resolve("settings.json")).load();
        assertEquals("", settings.languageTag);
        assertEquals(25, settings.gridSpacing);
    }

    @Test void canadianFrenchIsSelectableAndUsesCanadianNumberFormatting() throws Exception {
        Messages.initialize("fr-CA");
        assertTrue(Messages.supportedLanguageTags().contains("fr-CA"));
        assertEquals("Fichier", Messages.text("menuBar.file"));
        assertEquals("Paramètres", Messages.text("programWindow.settings"));
        assertEquals("fr-CA", Messages.locale().toLanguageTag());
        assertEquals("8,25", Messages.decimal(8.25));
        assertEquals(8.25, Messages.decimal("8,25"));
        assertEquals("Rétablir : Ajouter un nœud", Messages.text("history.redo", Messages.text("history.action.add.node")));
        assertEquals("La valeur de offset doit être un nombre.",
                Messages.text("error.json.primitive", Messages.text("error.json.numberType"), "offset"));
        AppSettings settings = new AppSettings();
        settings.languageTag = "fr-CA";
        settings.platformOffset = 8.25;
        SwingUtilities.invokeAndWait(() -> {
            AppSettings result = new SettingsPanel(settings).readSettings();
            assertEquals("fr-CA", result.languageTag);
            assertEquals(8.25, result.platformOffset);
        });
    }

    @Test void shippedTranslationsAreCompleteAndPreserveEveryArgument() throws Exception {
        Properties base = loadBundle("messages");
        for (String tag : Messages.supportedLanguageTags()) {
            if (tag.equals("en")) { continue; }
            Properties translated = loadBundle("messages_" + tag.replace('-', '_'));
            Set<String> expected = new HashSet<>(base.stringPropertyNames());
            expected.remove("languages.supported");
            assertEquals(expected, translated.stringPropertyNames(), tag);
            for (String key : expected) {
                String pattern = translated.getProperty(key);
                assertFalse(pattern.isBlank(), tag + ": " + key);
                Set<String> arguments = arguments(base.getProperty(key));
                assertEquals(arguments, arguments(pattern), tag + ": " + key);
                if (!arguments.isEmpty()) {
                    String formatted = new MessageFormat(pattern, Locale.forLanguageTag(tag))
                            .format(new Object[]{"ARG0", "ARG1", "ARG2", "ARG3", "ARG4"});
                    for (String argument : arguments) {
                        assertTrue(formatted.contains("ARG" + argument), tag + ": " + key);
                    }
                }
            }
        }
    }

    @Test void translatedToolbarControlsFitAtTheMinimumWindowWidth() throws Exception {
        for (String tag : Messages.supportedLanguageTags()) {
            Messages.initialize(tag);
            SwingUtilities.invokeAndWait(() -> {
                EditorToolbar toolbar = new EditorToolbar(new DiagramPanel(new Network()));
                for (int width : new int[]{800, 1200, 800}) {
                    toolbar.setSize(width, 200);
                    toolbar.setSize(width, toolbar.getPreferredSize().height);
                    toolbar.doLayout();
                    Rectangle available = new Rectangle(toolbar.getSize());
                    for (Component child : toolbar.getComponents()) {
                        assertTrue(available.contains(child.getBounds()), tag + ": " + child);
                        assertTrue(child.getWidth() >= child.getPreferredSize().width, tag + ": " + child);
                    }
                }
            });
        }
    }

    @Test void startupLanguageChangesImmediatelyWithoutCreatingOrOpeningADiagram() throws Exception {
        Messages.initialize("en");
        SettingsStore store = new SettingsStore(temporary.resolve("settings.json"));
        AppSettings settings = new AppSettings();
        settings.languageTag = "en";
        settings.gridSpacing = 42;
        store.save(settings);
        AtomicInteger refreshes = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            StartupPanel panel = new StartupPanel(store,
                    () -> fail("Created a diagram"), () -> fail("Opened a diagram"), () -> fail("Exited"),
                    refreshes::incrementAndGet, failure -> fail(failure));
            LanguagePicker picker = findLanguagePicker(panel);
            picker.setSelectedItem("fr-CA");
            assertEquals("fr-CA", Messages.locale().toLanguageTag());
            assertEquals("Bienvenue chez Track Diagram Generator!", ((javax.swing.JLabel) panel.getComponent(0)).getText());
            AppSettings saved = assertDoesNotThrow(store::load);
            assertEquals("fr-CA", saved.languageTag);
            assertEquals(42, saved.gridSpacing);
            picker.setSelectedItem("en");
            assertEquals("en", Messages.locale().toLanguageTag());
            assertEquals("en", assertDoesNotThrow(store::load).languageTag);
            assertEquals(2, refreshes.get());
        });
    }

    @Test void chineseScriptAndRegionalLocalesResolveTheIntendedTranslation() {
        for (String tag : List.of("zh-hans", "zh-CN", "zh-Hans-SG")) {
            Messages.initialize(tag);
            assertEquals("节点", Messages.text("editorToolbar.add.node"), tag);
            assertEquals("站台", Messages.text("editorToolbar.add.platform"), tag);
            assertEquals("文件(F)", Messages.text("menuBar.file"), tag);
            assertEquals("8.25", Messages.decimal(8.25), tag);
            assertEquals(8.25, Messages.decimal("8.25"), tag);
        }
        for (String tag : List.of("zh-hant", "zh-TW", "zh-Hant-HK")) {
            Messages.initialize(tag);
            assertEquals("節點", Messages.text("editorToolbar.add.node"), tag);
            assertEquals("月台", Messages.text("editorToolbar.add.platform"), tag);
            assertEquals("檔案(F)", Messages.text("menuBar.file"), tag);
            assertEquals("8.25", Messages.decimal(8.25), tag);
            assertEquals(8.25, Messages.decimal("8.25"), tag);
        }
    }

    @Test void chineseLanguagesCanBeSelectedAndSavedWithoutOpeningADiagram() throws Exception {
        Messages.initialize("en");
        SettingsStore store = new SettingsStore(temporary.resolve("settings.json"));
        SwingUtilities.invokeAndWait(() -> {
            StartupPanel panel = new StartupPanel(store,
                    () -> fail("Created a diagram"), () -> fail("Opened a diagram"), () -> fail("Exited"),
                    () -> {}, failure -> fail(failure));
            LanguagePicker picker = findLanguagePicker(panel);
            for (String tag : List.of("zh-Hans", "zh-Hant")) {
                picker.setSelectedItem(tag);
                assertEquals(tag, Messages.locale().toLanguageTag());
                assertEquals(tag, assertDoesNotThrow(store::load).languageTag);
                String welcome = ((javax.swing.JLabel) panel.getComponent(0)).getText();
                assertEquals(Messages.text("startupDialogue.welcome.to.track.diagram.generator"), welcome);
            }
        });
    }

    @Test void failedStartupLanguageSaveKeepsThePreviousLanguageAndSettingsFile() throws Exception {
        Messages.initialize("en");
        SettingsStore store = new SettingsStore(temporary.resolve("settings.json"));
        AppSettings settings = new AppSettings();
        settings.languageTag = "en";
        store.save(settings);
        AtomicInteger errors = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            StartupPanel panel = new StartupPanel(store, () -> {}, () -> {}, () -> {},
                    () -> fail("Applied a language despite a save failure"), failure -> errors.incrementAndGet());
            assertDoesNotThrow(() -> Files.writeString(store.getPath(), "invalid settings"));
            LanguagePicker picker = findLanguagePicker(panel);
            picker.setSelectedItem("fr-CA");
            assertEquals("en", picker.languageTag());
            assertEquals("en", Messages.locale().toLanguageTag());
            assertEquals(1, errors.get());
            assertEquals("invalid settings", assertDoesNotThrow(() -> Files.readString(store.getPath())));
        });
    }

    private static LanguagePicker findLanguagePicker(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof LanguagePicker picker) { return picker; }
            if (child instanceof Container container) {
                LanguagePicker picker = findLanguagePicker(container);
                if (picker != null) { return picker; }
            }
        }
        return null;
    }

    private static Set<String> arguments(String pattern) {
        Set<String> result = new HashSet<>();
        var matcher = Pattern.compile("\\{(\\d+)[,}]").matcher(pattern);
        while (matcher.find()) { result.add(matcher.group(1)); }
        return result;
    }

    private static Properties loadBundle(String name) throws Exception {
        Properties properties = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                assertFalse(containsKey(key), "Duplicate translation key: " + key);
                return super.put(key, value);
            }
        };
        try (var input = LocalizationTests.class.getResourceAsStream("/i18n/" + name + ".properties")) {
            assertNotNull(input, name);
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        return properties;
    }

    @Test void baseBundleContainsEveryLiteralMessageReferenceAndNoDuplicateKeys() throws Exception {
        Properties base = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                assertFalse(containsKey(key), "Duplicate translation key: " + key);
                return super.put(key, value);
            }
        };
        try (var input = getClass().getResourceAsStream("/i18n/messages.properties")) {
            assertNotNull(input);
            base.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        Pattern references = Pattern.compile("Messages\\.text\\(\"([^\"]+)\"");
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                var matcher = references.matcher(Files.readString(file));
                while (matcher.find()) { assertTrue(base.containsKey(matcher.group(1)), file + ": " + matcher.group(1)); }
            }
        }
        for (String key : base.stringPropertyNames()) {
            if (base.getProperty(key).matches("(?s).*\\{\\d+.*")) {
                assertDoesNotThrow(() -> new MessageFormat(base.getProperty(key), Locale.ENGLISH), key);
            }
        }
    }
}
