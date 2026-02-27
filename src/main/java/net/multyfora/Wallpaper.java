package net.multyfora;

import net.multyfora.util.RunCommand;

import javax.swing.*;
import java.util.*;

public class Wallpaper {
    private String title;
    private String id;
    private String description;
    private ImageIcon preview;
    private String[] tags;
    private Map<String, Property> properties = new LinkedHashMap<>();

    public Wallpaper(String title, String id, String description, ImageIcon preview, String[] tags) {
        this.title = title;
        this.id = id;
        this.description = description;
        this.preview = preview;
        this.tags = tags;
        loadProperties();
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

                    if (pline.startsWith("Description:")) {
                        desc = pline.substring("Description:".length()).trim();
                    } else if (pline.startsWith("Value:")) {
                        String val = pline.substring("Value:".length()).trim();
                        if (type == Property.Type.BOOLEAN) {
                            value = Double.parseDouble(val);
                        } else if (type == Property.Type.SLIDER) {
                            value = Double.parseDouble(val);
                        } else if (type == Property.Type.COMBOLIST) {
                            comboValue = val;
                        }
                    } else if (pline.startsWith("Minimum value:")) {
                        min = Double.parseDouble(pline.substring("Minimum value:".length()).trim());
                    } else if (pline.startsWith("Maximum value:")) {
                        max = Double.parseDouble(pline.substring("Maximum value:".length()).trim());
                    } else if (pline.startsWith("Step:")) {
                        step = Double.parseDouble(pline.substring("Step:".length()).trim());
                    } else if (pline.startsWith("R:")) {
                        // "R: 0.14902 G: 0.23137 B: 0.4 A: 1"
                        r = parseColorComponent(pline, "R:");
                        g = parseColorComponent(pline, "G:");
                        b = parseColorComponent(pline, "B:");
                        a = parseColorComponent(pline, "A:");
                    } else if (pline.contains(" -> ")) {
                        // combolist option: "label -> rawValue"  (indented further, still caught here)
                        String[] opt = pline.split(" -> ", 2);
                        comboOptions.add(new String[]{opt[0].trim(), opt[1].trim()});
                    }
                    i++;
                }

                Property prop = new Property(key, type, desc);
                switch (type) {
                    case SLIDER -> prop.setSliderValues(value, min, max, step);
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

    public Map<String, Property> getProperties() { return properties; }

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
        RunCommand.runWallpaperCommand(id);
    }
}