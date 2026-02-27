package net.multyfora.exampleStuff  ;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import net.multyfora.LocalWallpapers;
import net.multyfora.Wallpaper;

import java.io.ByteArrayInputStream;
import java.util.List;

public class WallpaperGridController {

    @FXML
    private FlowPane wallpaperGrid;

    @FXML
    public void initialize() {
        List<Wallpaper> wallpapers = LocalWallpapers.getAll();
        for (Wallpaper wallpaper : wallpapers) {
            wallpaperGrid.getChildren().add(createCard(wallpaper));
        }
    }

    private VBox createCard(Wallpaper wallpaper) {
        // Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(160);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(false);

        try {
            byte[] bytes = wallpaper.getPreview().getImage()
                    .getScaledInstance(-1, -1, 0) == null ? new byte[0]
                    : wallpaper.getPreview().getIconWidth() > 0
                    ? toBytes(wallpaper) : new byte[0];
            if (bytes.length > 0) {
                imageView.setImage(new Image(new ByteArrayInputStream(bytes)));
            }
        } catch (Exception e) {
            // leave imageview empty if preview fails
        }

        // Title label
        Label title = new Label(wallpaper.getTitle());
        title.setMaxWidth(160);
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 12px; -fx-text-fill: #ddd;");

        // Card container
        VBox card = new VBox(6, imageView, title);
        card.setPadding(new Insets(8));
        card.setPrefWidth(176);
        card.setStyle("""
                -fx-background-color: #2a2a2a;
                -fx-border-color: #444;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                """);

        // Click to run
        card.setOnMouseClicked(e -> wallpaper.run());

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("""
                -fx-background-color: #3a3a3a;
                -fx-border-color: #888;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                """));
        card.setOnMouseExited(e -> card.setStyle("""
                -fx-background-color: #2a2a2a;
                -fx-border-color: #444;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                """));

        return card;
    }

    private byte[] toBytes(Wallpaper wallpaper) {
        try {
            java.awt.image.BufferedImage buffered = new java.awt.image.BufferedImage(
                    wallpaper.getPreview().getIconWidth(),
                    wallpaper.getPreview().getIconHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_ARGB
            );
            buffered.getGraphics().drawImage(wallpaper.getPreview().getImage(), 0, 0, null);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(buffered, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
}