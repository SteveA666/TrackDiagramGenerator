package ui;

import i18n.Messages;
import java.awt.Component;
import java.util.Locale;
import javax.swing.*;

/** Shared language choices, shown in each language's own name. */
public final class LanguagePicker extends JComboBox<String> {
    public LanguagePicker() {
        addItem("");
        for (String tag : Messages.supportedLanguageTags()) { addItem(tag); }
        setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                String tag = value == null ? "" : value.toString();
                Locale locale = Locale.forLanguageTag(tag);
                return super.getListCellRendererComponent(list, tag.isEmpty()
                        ? Messages.text("settings.systemLanguage") : locale.getDisplayName(locale),
                        index, selected, focused);
            }
        });
    }

    public void selectLanguage(String tag) {
        // Preserve preferences whose translation is unavailable in this installation.
        for (int i = 0; i < getItemCount(); i++) {
            if (getItemAt(i).equals(tag)) { setSelectedItem(tag); return; }
        }
        addItem(tag);
        setSelectedItem(tag);
    }

    public String languageTag() { return (String) getSelectedItem(); }
}
