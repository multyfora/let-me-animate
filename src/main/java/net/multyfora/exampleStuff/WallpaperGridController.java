package net.multyfora.exampleStuff;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import net.multyfora.LocalWallpapers;
import net.multyfora.Property;
import net.multyfora.Wallpaper;
import net.multyfora.util.RunCommand;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WallpaperGridController {

    //Home page nodes
    @FXML private FlowPane   wallpaperGrid;
    @FXML private VBox       propertiesBox;

    //  Page container
    @FXML private BorderPane pageContainer;
    @FXML private VBox       homePage;

    //  Sidebar buttons
    @FXML private Button btnHome;
    @FXML private Button btnWorkshop;
    @FXML private Button btnSettings;
    @FXML private Button refreshBtn;

    //  Cached pages (lazy-loaded)
    private Node workshopPageNode = null;
    private Node settingsPageNode = null;

    //  Loader executor
    private final ExecutorService loader =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("wallpaper-loader");
                return t;
            });

    //  Init
    @FXML
    public void initialize() {
        loadWallpapersAsync();
    }

    //  Loading

    private void loadWallpapersAsync() {
        if (refreshBtn != null) {
            refreshBtn.setDisable(true);
            refreshBtn.setText("⏳ Loading...");
        }

        loader.submit(() -> {
            List<Wallpaper> wallpapers = LocalWallpapers.getAll();
            for (Wallpaper wallpaper : wallpapers) {
                VBox card = createCard(wallpaper);
                Platform.runLater(() -> wallpaperGrid.getChildren().add(card));
            }
            Platform.runLater(() -> {
                if (refreshBtn != null) {
                    refreshBtn.setDisable(false);
                    refreshBtn.setText("🔄 Refresh");
                }
            });
        });
    }

    @FXML
    private void refreshWallpapers() {
        wallpaperGrid.getChildren().clear();
        propertiesBox.getChildren().clear();
        loadWallpapersAsync();
    }

    //  Sidebar navigation

    @FXML
    private void showHome() {
        pageContainer.setCenter(homePage);
        setActiveButton(btnHome);
    }

    @FXML
    private void showWorkshop() {
        if (workshopPageNode == null) {
            workshopPageNode = loadPage("/net/multyfora/workshop/WorkshopBrowser.fxml", "Workshop");
            if (workshopPageNode == null) return;
        }
        pageContainer.setCenter(workshopPageNode);
        setActiveButton(btnWorkshop);
    }

    @FXML
    private void showSettings() {
        if (settingsPageNode == null) {
            settingsPageNode = loadPage("/net/multyfora/Settings.fxml", "Settings");
            if (settingsPageNode == null) return;
        }
        pageContainer.setCenter(settingsPageNode);
        setActiveButton(btnSettings);
    }

    private Node loadPage(String fxmlPath, String name) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            return loader.load();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Failed to load " + name + ":\n" + e.getMessage(),
                    ButtonType.OK).showAndWait();
            return null;
        }
    }

    @FXML
    private void onQuit() {
        Platform.exit();
    }

    private void setActiveButton(Button active) {
        Button[] all = {btnHome, btnWorkshop, btnSettings};
        for (Button b : all) {
            if (b == null) continue;
            boolean isActive = b == active;
            b.setStyle(
                "-fx-background-color: " + (isActive ? "#1a1a1a" : "transparent") + ";" +
                "-fx-text-fill: "        + (isActive ? "white"   : "#888")        + ";" +
                "-fx-font-size: 18px;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 10;"
            );
        }
    }

    //  Wallpaper card

    private VBox createCard(Wallpaper wallpaper) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(160);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(false);

        try {
            File file = new File(wallpaper.getPreviewPath());
            if (file.exists()) {
                imageView.setImage(
                    new Image(file.toURI().toString(), 160, 160, false, true, true)
                );
            }
        } catch (Exception ignored) {}

        Label title = new Label(wallpaper.getTitle());
        title.setMaxWidth(160);
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 12px; -fx-text-fill: #ddd;");

        VBox card = new VBox(6, imageView, title);
        card.setPadding(new Insets(8));
        card.setPrefWidth(176);
        setCardStyle(card, false);

        card.setOnMouseClicked(e -> showProperties(wallpaper));
        card.setOnMouseEntered(e -> setCardStyle(card, true));
        card.setOnMouseExited(e -> setCardStyle(card, false));

        return card;
    }

    private void setCardStyle(VBox card, boolean hovered) {
        card.setStyle("""
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-cursor: hand;
                """.formatted(
                hovered ? "#3a3a3a" : "#2a2a2a",
                hovered ? "#888" : "#444"
        ));
    }

    //  Properties panel

    private void showProperties(Wallpaper wallpaper) {
        propertiesBox.getChildren().clear();

        Label name = new Label(wallpaper.getTitle());
        name.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");
        name.setWrapText(true);

        Button runBtn = new Button("▶ Run Wallpaper");
        runBtn.setMaxWidth(Double.MAX_VALUE);
        runBtn.getStyleClass().add("neu-btn");
        runBtn.setOnAction(e -> wallpaper.run());

        Button stopBtn = new Button("⏹ Stop Wallpaper");
        stopBtn.setMaxWidth(Double.MAX_VALUE);
        stopBtn.getStyleClass().add("neu-btn");
        stopBtn.setOnAction(e -> RunCommand.stopWallpaper());
        Label loading = new Label("Loading properties...");
        loading.setStyle("-fx-text-fill: #888;");

        propertiesBox.getChildren().addAll(name, runBtn, stopBtn, new Separator(), loading);

        Thread thread = new Thread(() -> {
            Map<String, Property> properties = wallpaper.getProperties();
            Platform.runLater(() -> {
                propertiesBox.getChildren().remove(loading);
                if (properties.isEmpty()) {
                    Label none = new Label("No configurable properties.");
                    none.setStyle("-fx-text-fill: #888;");
                    propertiesBox.getChildren().add(none);
                    return;
                }
                for (Map.Entry<String, Property> entry : properties.entrySet()) {
                    VBox propRow = buildPropertyRow(entry.getValue());
                    if (propRow != null) propertiesBox.getChildren().add(propRow);
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    private VBox buildPropertyRow(Property prop) {
        Label label = new Label(
                prop.getDescription().isBlank() ? prop.getKey() : prop.getDescription()
        );
        label.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
        label.setWrapText(true);

        switch (prop.getType()) {
            case SLIDER -> {
                double min   = prop.getMin();
                double max   = prop.getMax() > prop.getMin() ? prop.getMax() : 1.0;
                double value = Math.max(min, Math.min(max, prop.getValue()));

                Slider slider = new Slider(min, max, value);
                slider.setBlockIncrement(prop.getStep() > 0 ? prop.getStep() : (max - min) / 100);
                slider.setShowTickLabels(true);
                slider.setStyle("-fx-control-inner-background: #333;");

                Label valueLabel = new Label(String.format("%.2f", value));
                valueLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");

                slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    valueLabel.setText(String.format("%.2f", newVal.doubleValue()));
                    prop.setSliderValues(newVal.doubleValue(), prop.getMin(), prop.getMax(), prop.getStep());
                });
                return new VBox(4, label, slider, valueLabel);
            }
            case BOOLEAN -> {
                CheckBox checkBox = new CheckBox();
                checkBox.setSelected(prop.getBoolValue());
                checkBox.setStyle("-fx-text-fill: #ccc;");
                checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> prop.setBoolValue(newVal));
                return new VBox(4, label, checkBox);
            }
            case COMBOLIST -> {
                ComboBox<String> combo = new ComboBox<>();
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.getItems().addAll(prop.getComboOptions().keySet());

                String currentLabel = prop.getComboOptions().entrySet().stream()
                        .filter(e -> e.getValue().equals(prop.getComboValue()))
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(null);
                combo.setValue(currentLabel);

                combo.valueProperty().addListener((obs, oldVal, newVal) ->
                        prop.setComboValue(prop.getComboOptions().get(newVal)));
                return new VBox(4, label, combo);
            }
            case COLOR -> {
                ColorPicker colorPicker = new ColorPicker(
                        Color.color(prop.getR(), prop.getG(), prop.getB(), prop.getA())
                );
                colorPicker.setMaxWidth(Double.MAX_VALUE);
                colorPicker.valueProperty().addListener((obs, oldVal, newVal) ->
                        prop.setColor(newVal.getRed(), newVal.getGreen(), newVal.getBlue(), newVal.getOpacity())
                );
                return new VBox(4, label, colorPicker);
            }
        }
        return null;
    }
}
