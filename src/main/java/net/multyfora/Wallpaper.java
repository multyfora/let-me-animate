package net.multyfora;

import net.multyfora.util.RunCommand;

import javax.swing.*;
import java.util.*;

public class Wallpaper {
    private String title;
    private String id;
    private String description;
    private ImageIcon preview;
    private String previewPath;
    private String[] tags;
    private Map<String, Property> properties = new LinkedHashMap<>();

    public Wallpaper(String title, String id, String description, String previewPath, ImageIcon preview, String[] tags) {
        this.title = title;
        this.id = id;
        this.description = description;
        this.preview = preview;
        this.previewPath = previewPath;
        this.tags = tags;
        // removed loadProperties() from here
    }

    public Map<String, Property> getProperties() {
        if (properties.isEmpty()) {
            loadProperties(); // only load when first accessed
        }
        return properties;
    }

    //TODO: this is really slow, need to optimize by caching results or only loading when requested. Maybe even load lazily when getProperties() is called, and have a separate method to force reload if needed.
    private void loadProperties() {
        String output = RunCommand.listProperties(id); // assumes it now returns a String
        properties = parseProperties(output);
    }


    private Map<String, Property> parseProperties(String raw) {
        Map<String, Property> result = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return result;

        String[] lines = raw.split("\\r?\\n");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();
            if (line.isEmpty()) { i++; continue; }

            // Header line: "key - type"
            if (line.contains(" - ")) {
                String[] parts = line.split(" - ", 2);
                String key = parts[0].trim();
                String typStr = parts[1].trim().toLowerCase();

                Property.Type type = switch (typStr) {
                    case "slider" -> Property.Type.SLIDER;
                    case "boolean" -> Property.Type.BOOLEAN;
                    case "combolist" -> Property.Type.COMBOLIST;
                    case "color" -> Property.Type.COLOR;
                    default -> null;
                };

                if (type == null) { i++; continue; }

                i++;
                String desc = "";
                double value = 0, min = 0, max = 0, step = 0;
                double r = 0, g = 0, b = 0, a = 1;
                String comboValue = null;
                List<String[]> comboOptions = new ArrayList<>();

                // Read indented property lines
                while (i < lines.length && (lines[i].startsWith("\t") || lines[i].startsWith("    "))) {
                    String pline = lines[i].trim();

                    if (pline.startsWith("Text:")) {
                        desc = pline.substring("Text:".length()).trim();
                    } else if (pline.startsWith("Value:")) {
                        String val = pline.substring("Value:".length()).trim();
                        if (type == Property.Type.COLOR) {
                            String[] parts1 = val.split(",");
                            if (parts1.length >= 3) {
                                r = Double.parseDouble(parts1[0].trim());
                                g = Double.parseDouble(parts1[1].trim());
                                b = Double.parseDouble(parts1[2].trim());
                                a = parts1.length >= 4 ? Double.parseDouble(parts1[3].trim()) : 1.0;
                            }
                        } else if (type == Property.Type.BOOLEAN) {
                            value = Double.parseDouble(val);
                        } else if (type == Property.Type.SLIDER) {
                            value = Double.parseDouble(val);
                        } else if (type == Property.Type.COMBOLIST) {
                            comboValue = val;
                        }
                    } else if (pline.startsWith("Min:")) {
                        min = Double.parseDouble(pline.substring("Min:".length()).trim());
                    } else if (pline.startsWith("Max:")) {
                        max = Double.parseDouble(pline.substring("Max:".length()).trim());
                    } else if (pline.startsWith("Step:")) {
                        step = Double.parseDouble(pline.substring("Step:".length()).trim());
                    } else if (pline.contains(" -> ")) {
                        String[] opt = pline.split(" -> ", 2);
                        comboOptions.add(new String[]{opt[0].trim(), opt[1].trim()});
                    }
                    i++;
                }

                Property prop = new Property(key, type, desc);
                switch (type) {
                    case SLIDER -> {
                        prop.setSliderValues(value, min, max, step);
                        System.out.println("Slider: " + key + " min=" + min + " max=" + max + " val=" + value + " id: " + id);
                    }
                    case BOOLEAN -> prop.setBoolValue(value == 1);
                    case COMBOLIST -> {
                        prop.setComboValue(comboValue);
                        for (String[] opt : comboOptions) prop.addComboOption(opt[0], opt[1]);
                    }
                    case COLOR -> prop.setColor(r, g, b, a);
                }
                result.put(key, prop);
            } else {
                i++;
            }
        }
        return result;
    }

    private double parseColorComponent(String line, String component) {
        int idx = line.indexOf(component);
        if (idx == -1) return 0;
        String rest = line.substring(idx + component.length()).trim();
        String[] tokens = rest.split("\\s+");
        try { return Double.parseDouble(tokens[0]); } catch (NumberFormatException e) { return 0; }
    }

    // Property access

    public Property getProperty(String key) { return properties.get(key); }

    // Existing methods

    public void showImage() {
        JFrame frame = new JFrame();
        JLabel label = new JLabel(preview);
        frame.add(label);
        frame.pack();
        frame.setVisible(true);
    }

    public String getTitle() { return title; }
    public String getId() { return id; }
    public String getPreviewPath() { return previewPath; }
    public String getDescription() { return description; }
    public ImageIcon getPreview() { return preview; }
    public String[] getTags() { return tags; }

    public void printInfo() {
        System.out.println("Title: " + title);
        System.out.println("ID: " + id);
        System.out.println("Description: " + description);
        System.out.println("Tags: " + String.join(", ", tags));
    }

    public void printNames() {
        System.out.println(" (" + id + ") " + title);
    }

    public void run() {
        RunCommand.runWallpaperCommand(id, properties);
    }
}