package ui;

import io.DiagramImageIO;
import io.DiagramImageIO.Format;
import java.awt.*;
import java.nio.file.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import model.Network;

/** Whole-diagram export options and destination selection. */
public final class ImageExportDialog {
    private ImageExportDialog() {}

    public static void show(Component owner, Network network, String suggestedName, Color canvasBackground) {
        JComboBox<Format> format = new JComboBox<>(Format.values());
        JSpinner scale = new JSpinner(new SpinnerNumberModel(200, 1, 800, 25));
        JSpinner padding = new JSpinner(new SpinnerNumberModel(20, 0, 1000, 5));
        JSpinner quality = new JSpinner(new SpinnerNumberModel(90, 1, 100, 5));
        JComboBox<String> background = new JComboBox<>(new String[]{"White", "Canvas color", "Transparent (PNG)"});
        JLabel size = new JLabel();
        JPanel options = new JPanel(new GridLayout(0, 2, 12, 8));
        options.add(new JLabel("Format")); options.add(format);
        options.add(new JLabel("Scale (%)")); options.add(scale);
        options.add(new JLabel("Padding (pixels)")); options.add(padding);
        options.add(new JLabel("Background")); options.add(background);
        options.add(new JLabel("JPG quality (%)")); options.add(quality);
        options.add(new JLabel("Image dimensions")); options.add(size);
        Runnable refresh = () -> {
            boolean jpg = format.getSelectedItem() == Format.JPG;
            quality.setEnabled(jpg);
            if (jpg && background.getSelectedIndex() == 2) { background.setSelectedIndex(0); }
            try {
                Dimension dimensions = DiagramPanel.imageSize(network, ((Number) scale.getValue()).doubleValue() / 100,
                        ((Number) padding.getValue()).intValue());
                size.setText(dimensions.width + " × " + dimensions.height + " px");
            } catch (IllegalArgumentException failure) { size.setText("Reduce scale or padding"); }
        };
        format.addActionListener(event -> refresh.run());
        background.addActionListener(event -> refresh.run());
        scale.addChangeListener(event -> refresh.run()); padding.addChangeListener(event -> refresh.run());
        refresh.run();
        while (JOptionPane.showConfirmDialog(owner, options, "Export image — whole diagram",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                scale.commitEdit(); padding.commitEdit(); quality.commitEdit();
                double factor = ((Number) scale.getValue()).doubleValue() / 100;
                int margin = ((Number) padding.getValue()).intValue();
                DiagramPanel.imageSize(network, factor, margin);
                Format selected = (Format) format.getSelectedItem();
                JFileChooser chooser = new JFileChooser(Path.of(".").toAbsolutePath().normalize().toFile());
                chooser.setDialogTitle("Export " + selected);
                chooser.setAcceptAllFileFilterUsed(false);
                chooser.setFileFilter(selected == Format.PNG ? new FileNameExtensionFilter("PNG image", "png")
                        : new FileNameExtensionFilter("JPG image", "jpg", "jpeg"));
                chooser.setSelectedFile(DiagramImageIO.withExtension(Path.of(suggestedName), selected).toFile());
                if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) { continue; }
                Path target = DiagramImageIO.withExtension(chooser.getSelectedFile().toPath(), selected);
                if (Files.exists(target) && JOptionPane.showConfirmDialog(owner, "Replace " + target.getFileName() + "?",
                        "Confirm overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) { continue; }
                Color color = background.getSelectedIndex() == 2 ? null
                        : background.getSelectedIndex() == 1 ? canvasBackground : Color.WHITE;
                var image = DiagramPanel.renderImage(network, factor, margin, color);
                DiagramImageIO.save(image, target, selected, ((Number) quality.getValue()).intValue());
                JOptionPane.showMessageDialog(owner, "Exported " + image.getWidth() + " × " + image.getHeight()
                        + " px to:\n" + target.toAbsolutePath(), "Image exported", JOptionPane.INFORMATION_MESSAGE);
                return;
            } catch (java.io.IOException | IllegalArgumentException | java.text.ParseException failure) {
                DiagramFileDialogs.showError(owner, "Export image", failure);
            }
        }
    }
}
