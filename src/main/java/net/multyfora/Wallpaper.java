package net.multyfora;

import javax.swing.*;

public class Wallpaper {
    private String title;
    private String id;
    private String description;
    private ImageIcon preview;
    private String[] tags;


    public Wallpaper(String title, String id, String description, ImageIcon preview, String[] tags) {
        this.title = title;
        this.id = id;
        this.description = description;
        this.preview = preview;
        this.tags = tags;
    }


    public void showImage(){
        JFrame frame = new JFrame();
        JLabel label = new JLabel(preview);
        frame.add(label);
        frame.pack();
        frame.setVisible(true);
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public ImageIcon getPreview() {
        return preview;
    }

    public String[] getTags() {
        return tags;
    }


    public void printInfo() {
        System.out.println("Title: " + title);
        System.out.println("ID: " + id);
        System.out.println("Description: " + description);
        System.out.println("Tags: " + String.join(", ", tags));
    }
}
