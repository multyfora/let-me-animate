package net.multyfora.exampleStuff;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import net.multyfora.WallpaperSettings;

public class SettingsController {

    @FXML private ComboBox<String> scalingCombo;
    @FXML private TextField        screenField;
    @FXML private Slider           fpsSlider;
    @FXML private Label            fpsLabel;
    @FXML private CheckBox         silentCheck;
    @FXML private CheckBox         noAudioCheck;
    @FXML private Slider           volumeSlider;
    @FXML private Label            volumeLabel;
    @FXML private VBox             volumeRow;
    @FXML private CheckBox         parallaxCheck;
    @FXML private Label            commandPreview;
    @FXML private Button           saveBtn;
    @FXML private Label            savedLabel;

    @FXML
    public void initialize() {
        // Populate scaling options
        scalingCombo.getItems().addAll("default", "stretch", "fit", "fill");

        // Load saved values
        scalingCombo.setValue(WallpaperSettings.getScaling());
        screenField.setText(WallpaperSettings.getScreen());
        fpsSlider.setValue(WallpaperSettings.getFps());
        silentCheck.setSelected(WallpaperSettings.isSilent());
        noAudioCheck.setSelected(WallpaperSettings.isNoAudio());
        volumeSlider.setValue(WallpaperSettings.getVolume());
        parallaxCheck.setSelected(WallpaperSettings.isDisableParallax());

        // FPS label
        updateFpsLabel((int) fpsSlider.getValue());
        fpsSlider.valueProperty().addListener((obs, o, n) -> {
            updateFpsLabel(n.intValue());
            updatePreview();
        });

        // Volume label + disable when silent
        updateVolumeLabel((int) volumeSlider.getValue());
        volumeSlider.valueProperty().addListener((obs, o, n) -> {
            updateVolumeLabel(n.intValue());
            updatePreview();
        });

        silentCheck.selectedProperty().addListener((obs, o, n) -> {
            volumeRow.setDisable(n);
            updatePreview();
        });
        volumeRow.setDisable(silentCheck.isSelected());

        // Live preview on any change
        scalingCombo.valueProperty().addListener((obs, o, n) -> updatePreview());
        screenField.textProperty().addListener((obs, o, n) -> updatePreview());
        noAudioCheck.selectedProperty().addListener((obs, o, n) -> updatePreview());
        parallaxCheck.selectedProperty().addListener((obs, o, n) -> updatePreview());

        updatePreview();
    }

    @FXML
    private void saveSettings() {
        WallpaperSettings.setScaling(scalingCombo.getValue());
        WallpaperSettings.setScreen(screenField.getText().trim());
        WallpaperSettings.setFps((int) fpsSlider.getValue());
        WallpaperSettings.setSilent(silentCheck.isSelected());
        WallpaperSettings.setNoAudio(noAudioCheck.isSelected());
        WallpaperSettings.setVolume((int) volumeSlider.getValue());
        WallpaperSettings.setDisableParallax(parallaxCheck.isSelected());

        // Flash "Saved"
        savedLabel.setVisible(true);
        savedLabel.setManaged(true);
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (Exception ignored) {}
            javafx.application.Platform.runLater(() -> {
                savedLabel.setVisible(false);
                savedLabel.setManaged(false);
            });
        }).start();
    }

    private void updateFpsLabel(int val) {
        fpsLabel.setText(val == 0 ? "unlimited" : val + " fps");
    }

    private void updateVolumeLabel(int val) {
        volumeLabel.setText(val + "%");
    }

    private void updatePreview() {
        // Build a preview using current UI state (not saved state)
        StringBuilder sb = new StringBuilder();

        String scaling = scalingCombo.getValue();
        if (scaling != null && !scaling.equals("default"))
            sb.append("--scaling ").append(scaling).append("\n");

        String screen = screenField.getText().trim();
        if (!screen.isBlank())
            sb.append("--screen-root ").append(screen).append("\n");

        int fps = (int) fpsSlider.getValue();
        if (fps > 0)
            sb.append("--fps ").append(fps).append("\n");

        if (noAudioCheck.isSelected())
            sb.append("--no-audio-processing\n");

        if (silentCheck.isSelected()) {
            sb.append("--silent\n");
        } else {
            int vol = (int) volumeSlider.getValue();
            if (vol != WallpaperSettings.DEFAULT_VOLUME)
                sb.append("--volume ").append(vol).append("\n");
        }

        if (parallaxCheck.isSelected())
            sb.append("--disable-parallax\n");

        sb.append("<wallpaper-id>");
        commandPreview.setText(sb.toString().trim());
    }
}
