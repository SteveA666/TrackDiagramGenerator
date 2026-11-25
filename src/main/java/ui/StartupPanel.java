package ui;

import i18n.Messages;
import java.awt.*;
import java.io.IOException;
import java.util.function.Consumer;
import javax.swing.*;
import settings.SettingsStore;

/** Welcome actions and an immediately applied, persistent language choice. */
public final class StartupPanel extends JPanel {
    private final JLabel welcome = new JLabel("", SwingConstants.CENTER);
    private final JLabel languageLabel = new JLabel();
    private final LanguagePicker language = new LanguagePicker();
    private final JButton create = new JButton(), open = new JButton(), exit = new JButton();
    private String currentLanguage;

    public StartupPanel(SettingsStore store, Runnable onNew, Runnable onOpen, Runnable onExit,
            Runnable onLanguageChanged, Consumer<Exception> onError) {
        super(new BorderLayout(12, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        try { currentLanguage = store.load().languageTag; }
        catch (IOException failure) {
            try { currentLanguage = store.loadDefaults().languageTag; }
            catch (IOException ignored) { currentLanguage = ""; }
        }
        language.selectLanguage(currentLanguage);
        languageLabel.setLabelFor(language);
        JPanel languages = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        languages.add(languageLabel);
        languages.add(language);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actions.add(create); actions.add(open); actions.add(exit);
        create.addActionListener(event -> onNew.run());
        open.addActionListener(event -> onOpen.run());
        exit.addActionListener(event -> onExit.run());
        add(welcome, BorderLayout.NORTH);
        add(languages, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        refreshText();
        language.addActionListener(event -> {
            String selected = language.languageTag();
            if (selected.equals(currentLanguage)) { return; }
            try {
                // Reload before saving so changing language preserves all other preferences.
                var settings = store.load();
                settings.languageTag = selected;
                store.save(settings);
                currentLanguage = selected;
                Messages.initialize(selected);
                refreshText();
                onLanguageChanged.run();
            } catch (IOException | IllegalArgumentException failure) {
                language.selectLanguage(currentLanguage);
                onError.accept(failure);
            }
        });
    }

    private void refreshText() {
        welcome.setText(Messages.text("startupDialogue.welcome.to.track.diagram.generator"));
        languageLabel.setText(Messages.text("settings.language"));
        create.setText(Messages.text("startupDialogue.new.network"));
        open.setText(Messages.text("startupDialogue.open.network"));
        exit.setText(Messages.text("startupDialogue.exit"));
        language.repaint();
        revalidate();
        repaint();
    }
}
