# Localization

The application uses Java `ResourceBundle` property files and `java.text.MessageFormat`. No additional runtime dependency is needed. English, Canadian French (`fr-CA`), Simplified Chinese (`zh-Hans`), and Traditional Chinese (`zh-Hant`) are shipped.

## Choosing a language

Use the language picker on the welcome screen to choose **System default** or an installed translation. The welcome screen updates immediately and saves the choice without creating or opening a network. Language names appear in their own language.

Once a diagram is open, use **Settings > Language**, save, and restart to change the editor's language. Other settings continue to apply immediately.

The preference is stored in `settings.json` as `languageTag`, a BCP 47 tag such as `en` or `fr-CA`. An empty string selects the system locale captured at application startup. Older settings files without this property continue to work. A valid preference from a newer installation is preserved even if its translation is unavailable.

The saved locale is selected before the startup dialog is constructed. Changing it on the welcome screen refreshes the startup controls before the editor is created. Lookup uses the requested locale, its parent bundles, and the English base bundle. It does not fall back through an unrelated JVM default language. Missing keys in a partial translation use English; a key missing from the base bundle raises an error so development mistakes remain visible.

## Adding a translation

1. Copy `src/main/resources/i18n/messages.properties` to a locale-specific file, for example `messages_fr.properties` or `messages_fr_CA.properties`. Save it as UTF-8; Unicode escapes are unnecessary.
2. Translate the values, preserving the keys. Regional bundles can contain only regional overrides. Keep the existing English base as the complete reference.
3. Add the language tag to `languages.supported` in the **base bundle**, for example `languages.supported=en,fr,fr-CA`. This is the language selector's registry; translated copies of that entry are ignored.
4. Run `mvnw.cmd verify` with JDK 26 or newer. The normal Maven resources step includes the bundles in the executable JAR and native packages.
5. Select the translation, restart, and check startup, menus, toolbars, each settings tab, file prompts, export, editing dialogs, status messages, validation, and undo/redo. Check long labels, font coverage, and menu mnemonic conflicts at the smallest supported window size. Right-to-left layout mirroring is not currently implemented; validate and adapt the UI before shipping such a translation. Diagram coordinates and track-side semantics must remain unchanged.

Chinese translations use script-specific bundles: `zh-Hans` maps to `messages_zh_Hans.properties` and `zh-Hant` to `messages_zh_Hant.properties`. Locale tags are case insensitive. Java also resolves regional system locales such as `zh-CN` and `zh-TW` to the corresponding script bundle. The language picker displays each script's native name.

## Message conventions

Use `Messages.text("key")` for a plain message and `Messages.text("key", arguments...)` for a parameterized message. Keys identify a message's purpose and should remain stable when wording changes. Translate whole messages instead of concatenating sentence fragments. Error details and user content can be inserted as arguments.

```properties
file.replace=Replace {0}?
history.undo=Undo {0}
```

Parameterized values follow `java.text.MessageFormat` syntax. Arguments may be reordered. An apostrophe in a parameterized pattern must be doubled (`l''origine`); braces used as literal text must be quoted. Plain messages with no arguments are returned verbatim, so their apostrophes need no escaping. Backslashes and line breaks follow `.properties` escaping rules.

Menu mnemonics have separate `menu.mnemonic.*` entries. Keyboard accelerators and canvas tool shortcuts retain their existing keys. Enum values are translated by UI renderers; their Java names and JSON values remain stable. Undo history stores action keys and resolves their names for display.

Platform decimal fields display and parse the selected locale's decimal separator. Input must be finite and consume the entire field; grouping and trailing characters are rejected. Integer fields reject fractions and overflow. Swing numeric spinners use the same formatting locale. JSON numbers remain locale independent. Station names, annotations, filenames, fonts, colors, object IDs, and document structure are never translated during persistence.

Swing supplies its own standard dialog and file chooser labels for the selected locale where the runtime supports them. Third-party/JDK exception details may still use the runtime's wording; the application's own dialog titles and validation messages are in the bundle.

## Verification

`LocalizationTests` checks complete key and argument coverage for shipped translations, toolbar layout at the minimum window width, base-key coverage and duplicate keys, UTF-8 loading, reordered arguments and apostrophe escaping, missing-key and unsupported-locale fallback, numeric input, legacy preferences, settings form persistence, and diagram/history round trips across locales. Its partial `qaa` bundle is a test fixture and is excluded from the application JAR.
