package net.multyfora.exampleStuff.workshop;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import com.google.gson.*;

public class WorkshopBrowserController {


    //Steam constants
    private static final String API_KEY;

    static {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(
                System.getProperty("user.dir") + "/src/main/resources/net/multyfora/config.properties")) {

            props.load(in);
            API_KEY = props.getProperty("steam.api.key");

        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }
    private static final String APP_ID    = "431960";  // Wallpaper Engine
    private static final String API_BASE  = "https://api.steampowered.com";
    private static final int    PAGE_SIZE = 20;

    //FXML fields
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button           searchBtn;

    @FXML private FlowPane  workshopGrid;
    @FXML private ScrollPane gridScrollPane;
    @FXML private VBox       loadingOverlay;
    @FXML private VBox       emptyState;

    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Label  pageLabel;

    @FXML private VBox detailBox;

    //State
    private int    currentPage     = 1;
    private String currentQuery    = "";
    private int    currentSortType = 1;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private static final Map<String, Integer> SORT_OPTIONS = new LinkedHashMap<>();
    static {
        SORT_OPTIONS.put("Trending",     1);
        SORT_OPTIONS.put("Most Recent",  9);
        SORT_OPTIONS.put("Top Rated",    3);
        SORT_OPTIONS.put("Most Popular", 12);
    }

    //Init
    @FXML
    public void initialize() {
        sortCombo.getItems().addAll(SORT_OPTIONS.keySet());
        sortCombo.setValue("Trending");
        sortCombo.setOnAction(e -> currentSortType = SORT_OPTIONS.getOrDefault(sortCombo.getValue(), 1));
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) onSearch();
        });
        setLoading(true);
        fetchPage(1, "", 1);
    }

    //FXML actions
    @FXML private void onSearch() {
        currentQuery    = searchField.getText().trim();
        currentSortType = SORT_OPTIONS.getOrDefault(sortCombo.getValue(), 1);
        fetchPage(1, currentQuery, currentSortType);
    }

    @FXML private void onPrev() {
        if (currentPage > 1) fetchPage(currentPage - 1, currentQuery, currentSortType);
    }

    @FXML private void onNext() {
        fetchPage(currentPage + 1, currentQuery, currentSortType);
    }

    //Network
    private void fetchPage(int page, String query, int sortType) {
        setLoading(true);
        executor.submit(() -> {
            try {
                String url = API_BASE + "/IPublishedFileService/QueryFiles/v1/" +
                        "?key=" + API_KEY +
                        "&appid=" + APP_ID +
                        "&query_type=" + sortType +
                        "&numperpage=" + PAGE_SIZE +
                        "&page=" + page +
                        "&return_previews=true" +
                        "&return_metadata=true" +
                        "&return_tags=true" +
                        (query.isBlank() ? "" : "&search_text=" + query.replace(" ", "%20"));

                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).build();
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                List<WorkshopItem> items = parseItems(res.body());

                Platform.runLater(() -> {
                    currentPage = page;
                    displayItems(items);
                    pageLabel.setText("Page " + currentPage);
                    prevBtn.setDisable(currentPage <= 1);
                    setLoading(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Failed to load Workshop:\n" + ex.getMessage());
                });
            }
        });
    }

    private List<WorkshopItem> parseItems(String json) {
        List<WorkshopItem> result = new ArrayList<>();
        try {
            JsonObject root     = JsonParser.parseString(json).getAsJsonObject();
            JsonObject response = root.getAsJsonObject("response");
            if (response == null || !response.has("publishedfiledetails")) return result;

            for (JsonElement el : response.getAsJsonArray("publishedfiledetails")) {
                JsonObject obj  = el.getAsJsonObject();
                WorkshopItem item = new WorkshopItem();
                item.id            = str(obj, "publishedfileid");
                item.title         = str(obj, "title");
                item.description   = str(obj, "file_description");
                item.previewUrl    = str(obj, "preview_url");
                item.fileSize      = obj.has("file_size")      ? obj.get("file_size").getAsLong()      : 0;
                item.subscriptions = obj.has("subscriptions")  ? obj.get("subscriptions").getAsInt()   : 0;
                if (obj.has("tags")) {
                    for (JsonElement t : obj.getAsJsonArray("tags"))
                        item.tags.add(t.getAsJsonObject().get("tag").getAsString());
                }
                if (!item.id.isBlank()) result.add(item);
            }
        } catch (Exception ignored) {}
        return result;
    }

    private String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    //UI
    private void displayItems(List<WorkshopItem> items) {
        workshopGrid.getChildren().clear();
        emptyState.setVisible(items.isEmpty());
        emptyState.setManaged(items.isEmpty());
        for (WorkshopItem item : items) workshopGrid.getChildren().add(createCard(item));
    }

    private VBox createCard(WorkshopItem item) {
        ImageView iv = new ImageView();
        iv.setFitWidth(160);
        iv.setFitHeight(110);
        iv.setPreserveRatio(false);

        if (!item.previewUrl.isBlank()) {
            executor.submit(() -> {
                try {
                    Image img = new Image(item.previewUrl, 160, 110, false, true, true);
                    Platform.runLater(() -> iv.setImage(img));
                } catch (Exception ignored) {}
            });
        }

        Label title = new Label(item.title.isBlank() ? "Untitled" : item.title);
        title.setMaxWidth(160);
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 11px; -fx-text-fill: #ddd;");

        Label subs = new Label("👤 " + fmt(item.subscriptions));
        subs.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

        VBox card = new VBox(5, iv, title, subs);
        card.setPadding(new Insets(8));
        card.setPrefWidth(176);
        cardStyle(card, false);
        card.setOnMouseClicked(e -> showDetail(item));
        card.setOnMouseEntered(e -> cardStyle(card, true));
        card.setOnMouseExited(e -> cardStyle(card, false));
        return card;
    }

    private void cardStyle(VBox card, boolean hovered) {
        card.setStyle("""
            -fx-background-color: %s;
            -fx-border-color: %s;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            """.formatted(hovered ? "#3a3a3a" : "#2a2a2a", hovered ? "#1565C0" : "#444"));
    }

    private void showDetail(WorkshopItem item) {
        detailBox.getChildren().clear();

        ImageView preview = new ImageView();
        preview.setFitWidth(268);
        preview.setFitHeight(160);
        preview.setPreserveRatio(false);
        if (!item.previewUrl.isBlank()) {
            executor.submit(() -> {
                try {
                    Image img = new Image(item.previewUrl, 268, 160, false, true, true);
                    Platform.runLater(() -> preview.setImage(img));
                } catch (Exception ignored) {}
            });
        }

        Label title = new Label(item.title.isBlank() ? "Untitled" : item.title);
        title.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");
        title.setWrapText(true);

        Label idLbl   = new Label("ID: " + item.id);
        idLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");
        Label subsLbl = new Label("👤 " + fmt(item.subscriptions) + " subscribers");
        subsLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        Label sizeLbl = new Label("💾 " + fmtSize(item.fileSize));
        sizeLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

        FlowPane tags = new FlowPane(6, 6);
        for (String tag : item.tags) {
            Label t = new Label(tag);
            t.setStyle("-fx-background-color: #1a3a5c; -fx-text-fill: #7eb8f7; -fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 4;");
            tags.getChildren().add(t);
        }

        String raw = item.description.isBlank() ? "No description." : item.description;
        Label desc = new Label(raw.length() > 300 ? raw.substring(0, 300) + "…" : raw);
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa;");

        Button downloadBtn = new Button("⬇  Download via SteamCMD");
        downloadBtn.setMaxWidth(Double.MAX_VALUE);
        downloadBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 10px; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        downloadBtn.setOnAction(e -> startDownload(item, downloadBtn));

        Button browserBtn = new Button("🌐 Open in Steam");
        browserBtn.setMaxWidth(Double.MAX_VALUE);
        browserBtn.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #ccc; -fx-font-size: 12px; -fx-padding: 8px; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-color: #444; -fx-cursor: hand;");
        browserBtn.setOnAction(e -> {
            try {
                new ProcessBuilder("xdg-open",
                        "https://steamcommunity.com/sharedfiles/filedetails/?id=" + item.id).start();
            } catch (Exception ex) { /* ignore */ }
        });

        detailBox.getChildren().addAll(
                preview, title, idLbl, new Separator(),
                subsLbl, sizeLbl, tags, new Separator(),
                desc, new Separator(), downloadBtn, browserBtn
        );
    }

    //Download

    private String findSteamCmd() {
        String[] candidates = {
                "/usr/bin/steamcmd",
                "/usr/games/steamcmd",
                System.getProperty("user.home") + "/.steam/steamcmd/steamcmd.sh",
                "/usr/lib/steamcmd/steamcmd",
        };
        for (String path : candidates) {
            if (new java.io.File(path).exists()) return path;
        }
        return null;
    }

    private void startDownload(WorkshopItem item, Button btn) {
        String steamcmd = findSteamCmd();

        if (steamcmd == null) {
            showDownloadError(btn, "❌ steamcmd not found!\n\nInstall with:\n  yay -S steamcmd");
            return;
        }

        if (SteamLoginManager.hasSavedSession()) {
            // Token exists - go straight to download, no login needed
            doDownload(item, btn, steamcmd);
        } else {
            // No token - show login dialog first
            final String sc = steamcmd;
            SteamLoginManager.showLoginDialog(
                    steamcmd, executor,
                    () -> doDownload(item, btn, sc),
                    err -> showDownloadError(btn, err)
            );
        }
    }

    private boolean steamCmdTokenExists() {
        // steamcmd stores its token here after a successful login
        java.io.File tokenDir = new java.io.File(
                System.getProperty("user.home") + "/Steam/config"
        );
        return tokenDir.exists() && tokenDir.list() != null && tokenDir.list().length > 0;
    }

    private void doDownload(WorkshopItem item, Button btn, String steamcmd) {
        btn.setDisable(true);
        btn.setText("⏳ Downloading...");

        // Reuse existing TextArea log or create a new one
        TextArea log = detailBox.getChildren().stream()
                .filter(n -> n instanceof TextArea)
                .map(n -> (TextArea) n)
                .findFirst()
                .orElseGet(() -> {
                    TextArea t = new TextArea();
                    t.setEditable(false);
                    t.setPrefHeight(140);
                    t.setStyle("-fx-background-color: #111; -fx-text-fill: #0f0; " +
                            "-fx-font-family: monospace; -fx-font-size: 10px;");
                    t.setWrapText(true);
                    detailBox.getChildren().add(t);
                    return t;
                });

        log.clear();
        String username = SteamLoginManager.getSavedUsername();
        log.appendText("Logging in as: " + username + "\n");

        executor.submit(() -> {
            try {
                // Pass only username - steamcmd uses its cached token (no password needed after first login)
                ProcessBuilder pb = new ProcessBuilder(
                        steamcmd,
                        "+login", SteamLoginManager.getSavedUsername(),
                        "+workshop_download_item", APP_ID, item.id,
                        "+quit"
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();

                StringBuilder fullOutput = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        fullOutput.append(line).append("\n");
                        final String l = line;
                        Platform.runLater(() -> log.appendText(l + "\n"));
                    }
                }

                int exit = proc.waitFor();
                String out = fullOutput.toString().toLowerCase();

                Platform.runLater(() -> {
                    if (exit == 0 && (out.contains("success") || out.contains("downloaded"))) {
                        btn.setText("✅ Downloaded!");
                        btn.setStyle(btn.getStyle().replace("#1565C0", "#2e7d32"));
                        String path = System.getProperty("user.home") +
                                "/.local/share/Steam/steamapps/workshop/content/" + APP_ID + "/" + item.id;
                        log.appendText("\n✅ Saved to:\n" + path + "\n");
                        log.appendText("Restart the app to see it in Local Wallpapers.\n");

                    } else if (out.contains("invalid session") || out.contains("not logged on") ||
                            (out.contains("login") && out.contains("failed"))) {
                        // Token expired — clear and re-prompt
                        SteamLoginManager.clearSession();
                        log.appendText("\n⚠ Session expired. Please log in again.\n");
                        btn.setDisable(false);
                        btn.setText("⬇  Download via SteamCMD");
                        final String sc = steamcmd;
                        SteamLoginManager.showLoginDialog(
                                steamcmd, executor,
                                () -> doDownload(item, btn, sc),
                                err -> log.appendText("❌ " + err + "\n")
                        );
                    } else {
                        btn.setDisable(false);
                        btn.setText("⬇  Download via SteamCMD");
                        log.appendText("\n❌ Download failed (exit code " + exit + ")\n");
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("⬇  Download via SteamCMD");
                    log.appendText("❌ Error: " + ex.getMessage() + "\n");
                });
            }
        });
    }

    private void showDownloadError(Button btn, String msg) {
        btn.setDisable(false);
        btn.setText("⬇  Download via SteamCMD");
        Label err = new Label(msg);
        err.setStyle("-fx-text-fill: #e57373; -fx-font-size: 11px;");
        err.setWrapText(true);
        detailBox.getChildren().add(err);
    }

    //Helpers
    private void setLoading(boolean on) {
        loadingOverlay.setVisible(on);
        loadingOverlay.setManaged(on);
        searchBtn.setDisable(on);
    }

    private void showError(String msg) {
        workshopGrid.getChildren().clear();
        Label err = new Label("⚠ " + msg);
        err.setStyle("-fx-text-fill: #e57373; -fx-font-size: 13px;");
        err.setWrapText(true);
        workshopGrid.getChildren().add(err);
    }

    private String fmt(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    private String fmtSize(long b) {
        if (b <= 0)              return "Unknown size";
        if (b >= 1_073_741_824L) return String.format("%.1f GB", b / 1_073_741_824.0);
        if (b >= 1_048_576L)     return String.format("%.1f MB", b / 1_048_576.0);
        return String.format("%.1f KB", b / 1_024.0);
    }

    //Model
    public static class WorkshopItem {
        String id = "", title = "", description = "", previewUrl = "";
        long   fileSize      = 0;
        int    subscriptions = 0;
        List<String> tags    = new ArrayList<>();
    }
}
