package net.multyfora;

import java.util.LinkedHashMap;
import java.util.Map;

public class Property {
    public enum Type { SLIDER, BOOLEAN, COMBOLIST, COLOR }

    private final String key;
    private final Type type;
    private final String description;

    // slider
    private double value, min, max, step;

    // boolean
    private boolean boolValue;

    // combolist
    private String comboValue;
    private final Map<String, String> comboOptions = new LinkedHashMap<>(); // label -> rawValue

    // color
    private double r, g, b, a;

    public Property(String key, Type type, String description) {
        this.key = key;
        this.type = type;
        this.description = description;
    }

    // Setters for each type

    public void setSliderValues(double value, double min, double max, double step) {
        this.value = value; this.min = min; this.max = max; this.step = step;
    }

    public void setBoolValue(boolean v) { this.boolValue = v; }

    public void setComboValue(String v) { this.comboValue = v; }
    public void addComboOption(String label, String rawValue) { comboOptions.put(label, rawValue); }

    public void setColor(double r, double g, double b, double a) {
        this.r = r; this.g = g; this.b = b; this.a = a;
    }

    // Getters

    public String getKey() { return key; }
    public Type getType() { return type; }
    public String getDescription() { return description; }

    public double getValue() { return value; }
    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    public boolean getBoolValue() { return boolValue; }

    public String getComboValue() { return comboValue; }
    public Map<String, String> getComboOptions() { return comboOptions; }

    public double getR() { return r; }
    public double getG() { return g; }
    public double getB() { return b; }
    public double getA() { return a; }

    @Override
    public String toString() {
        return switch (type) {
            case SLIDER -> key + " [slider] = " + value + " (min=" + min + ", max=" + max + ", step=" + step + ")";
            case BOOLEAN -> key + " [boolean] = " + boolValue;
            case COMBOLIST -> key + " [combolist] = " + comboValue + " options=" + comboOptions;
            case COLOR -> key + " [color] R=" + r + " G=" + g + " B=" + b + " A=" + a;
        };
    }
}