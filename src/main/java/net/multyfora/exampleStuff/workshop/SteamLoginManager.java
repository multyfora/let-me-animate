package net.multyfora.exampleStuff.workshop;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class SteamLoginManager {

    private static final String PREF_USERNAME  = "steam_username";
    private static final String PREF_LOGGED_IN = "steam_logged_in";
    private static final Preferences PREFS =
            Preferences.userNodeForPackage(SteamLoginManager.class);

    public static String getSavedUsername() {
        return PREFS.get(PREF_USERNAME, "");
    }

    public static boolean hasSavedSession() {
        if (!PREFS.getBoolean(PREF_LOGGED_IN, false)) {
            System.out.println("[Steam] PREF_LOGGED_IN is false");
            return false;
        }
        String username = getSavedUsername();
        if (username.isBlank()) {
            System.out.println("[Steam] No saved username");
            return false;
        }
        boolean hasToken = hasLoginToken(username);
        System.out.println("[Steam] username=" + username + " hasToken=" + hasToken);
        return hasToken;
    }

    private static boolean hasLoginToken(String username) {
        String home = System.getProperty("user.home");
        String[] candidates = {
                home + "/.local/share/Steam/config/config.vdf",
                home + "/.steam/steam/config/config.vdf",
                home + "/Steam/config/config.vdf",
                home + "/.steam/steamcmd/config/config.vdf",
        };

        for (String path : candidates) {
            java.io.File f = new java.io.File(path);
            if (!f.exists()) continue;
            try {
                String content = java.nio.file.Files.readString(f.toPath());
                // steamcmd stores the username as a quoted key in the Accounts section
                if (content.contains("\"" + username + "\"")) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        System.out.println("[Steam] Token not found in any of the candidate paths");
        // Print what we actually searched for:
        System.out.println("[Steam] Looking for: \"" + username + "\"");
        // Print which files existed:
        for (String path : candidates) {
            System.out.println("[Steam] " + path + " exists=" + new java.io.File(path).exists());
        }
        return false;
    }

    public static void clearSession() {
        PREFS.remove(PREF_USERNAME);
        PREFS.putBoolean(PREF_LOGGED_IN, false);
    }

    private static void saveSession(String username) {
        PREFS.put(PREF_USERNAME, username.trim());
        PREFS.putBoolean(PREF_LOGGED_IN, true);
    }

    //Login dialog

    public static void showLoginDialog(String steamcmdPath,
                                       ExecutorService executor,
                                       Runnable onSuccess,
                                       Consumer<String> onFailure) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Steam Login");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #1e1e1e;");
        pane.setPrefWidth(400);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        Label title = new Label("🔐 Sign in to Steam");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitle = new Label("Your password is never stored. steamcmd caches a session token after login.");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        subtitle.setWrapText(true);

        // Username
        Label userLabel = new Label("Steam Username");
        userLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
        TextField usernameField = new TextField(getSavedUsername());
        usernameField.setPromptText("your_steam_username");
        applyFieldStyle(usernameField);

        // Password
        Label passLabel = new Label("Password");
        passLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        applyFieldStyle(passwordField);

        // Steam Guard code (shown after first attempt)
        Label guardLabel = new Label("Steam Mobile Authenticator Code");
        guardLabel.setStyle("-fx-text-fill: #f0a500; -fx-font-size: 12px;");
        guardLabel.setVisible(false);
        guardLabel.setManaged(false);

        Label guardHint = new Label("Open the Steam app → Guard tab → enter the 5-character code");
        guardHint.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        guardHint.setVisible(false);
        guardHint.setManaged(false);

        TextField guardField = new TextField();
        guardField.setPromptText("XXXXX");
        applyFieldStyle(guardField);
        guardField.setStyle(guardField.getStyle() + "-fx-border-color: #f0a500;");
        guardField.setVisible(false);
        guardField.setManaged(false);

        // Status
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        content.getChildren().addAll(
                title, subtitle, new Separator(),
                userLabel, usernameField,
                passLabel, passwordField,
                guardLabel, guardHint, guardField,
                statusLabel
        );
        pane.setContent(content);

        ButtonType loginBtnType  = new ButtonType("Sign In", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtnType = new ButtonType("Cancel",  ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(loginBtnType, cancelBtnType);

        Button signInBtn = (Button) pane.lookupButton(loginBtnType);
        Button cancelBtn = (Button) pane.lookupButton(cancelBtnType);

        signInBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        cancelBtn.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #ccc; -fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");

        // Prevent auto-close - handle manually
        signInBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();

            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String guard    = guardField.getText().trim().toUpperCase();

            if (username.isBlank()) {
                showStatus(statusLabel, "⚠ Please enter your username.", false);
                return;
            }
            if (password.isBlank()) {
                showStatus(statusLabel, "⚠ Please enter your password.", false);
                return;
            }

            signInBtn.setDisable(true);
            signInBtn.setText("Signing in...");
            showStatus(statusLabel, "Connecting to Steam...", true);

            executor.submit(() -> {
                try {
                    ProcessBuilder pb;
                    if (guard.isBlank()) {
                        pb = new ProcessBuilder(steamcmdPath,
                                "+login", username, password,
                                "+quit");
                    } else {
                        pb = new ProcessBuilder(steamcmdPath,
                                "+login", username, password, guard,
                                "+quit");
                    }
                    pb.redirectErrorStream(true);
                    Process proc = pb.start();

                    StringBuilder output = new StringBuilder();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(proc.getInputStream()));

                    // Stream output line by line in real time
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        final String l = line;
                        System.out.println("[SteamLogin] " + l);
                        String outSoFar = output.toString().toLowerCase();

                        Platform.runLater(() -> showStatus(statusLabel,
                                "⏳ " + l.replaceAll("\\[\\d+m", "").trim(), true));

                        // Detect mobile auth waiting
                        if (outSoFar.contains("waiting for mobile confirmation") ||
                                outSoFar.contains("use the steam mobile app") ||
                                outSoFar.contains("check your phone")) {
                            Platform.runLater(() -> showStatus(statusLabel,
                                    "📱 Check your phone — approve the login in the Steam app.", true));
                        }

                        // Detect Guard code needed (email/old Guard)
                        if (outSoFar.contains("two-factor") || outSoFar.contains("steam guard") ||
                                outSoFar.contains("enter current code") ||
                                outSoFar.contains("invalid login auth code")) {
                            Platform.runLater(() -> {
                                guardLabel.setVisible(true);  guardLabel.setManaged(true);
                                guardHint.setVisible(true);   guardHint.setManaged(true);
                                guardField.setVisible(true);  guardField.setManaged(true);
                                guardField.requestFocus();
                                showStatus(statusLabel,
                                        "🔒 Enter the code from your Steam mobile app or email.", true);
                                signInBtn.setDisable(false);
                                signInBtn.setText("Sign In");
                            });
                            // Don't exit - let process finish naturally
                        }
                    }

                    proc.waitFor();
                    String out = output.toString().toLowerCase();

                    Platform.runLater(() -> {
                        if (out.contains("logged in ok") || out.contains("loading steam api")) {
                            saveSession(username);
                            showStatus(statusLabel, "✅ Logged in successfully!", true);
                            signInBtn.setText("✅ Done");
                            new Thread(() -> {
                                try { Thread.sleep(700); } catch (Exception ignored) {}
                                Platform.runLater(() -> {
                                    dialog.setResult(loginBtnType);
                                    dialog.close();
                                    onSuccess.run();
                                });
                            }).start();
                        } else if (out.contains("invalid password") || out.contains("incorrect login")) {
                            showStatus(statusLabel, "❌ Wrong username or password.", false);
                            signInBtn.setDisable(false);
                            signInBtn.setText("Sign In");
                        } else {
                            String snippet = output.toString().replaceAll("\\[\\d+m", "").trim();
                            snippet = snippet.length() > 300 ? snippet.substring(snippet.length() - 300) : snippet;
                            showStatus(statusLabel, "❌ Login failed:\n" + snippet, false);
                            signInBtn.setDisable(false);
                            signInBtn.setText("Sign In");
                        }
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        showStatus(statusLabel, "❌ Error: " + ex.getMessage(), false);
                        signInBtn.setDisable(false);
                        signInBtn.setText("Sign In");
                    });
                }
            });
        });

        dialog.showAndWait();
    }

    //Sign out dialog

    /**
     * Shows a small confirmation dialog to sign out.
     * Call this from a "Sign Out" button in the Workshop UI.
     */
    public static void showSignOutDialog() {
        String username = getSavedUsername();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sign Out");
        alert.setHeaderText(null);
        alert.setContentText("Sign out of Steam account \"" + username + "\"?\n\n" +
                "You will need to log in again to download wallpapers.");

        alert.getDialogPane().setStyle("-fx-background-color: #1e1e1e;");

        Label content = new Label("Sign out of Steam account\n\"" + username + "\"?\n\nYou will need to log in again to download wallpapers.");
        content.setStyle("-fx-text-fill: #ccc; -fx-font-size: 13px;");
        content.setWrapText(true);
        alert.getDialogPane().setContent(content);

        ButtonType signOutBtn = new ButtonType("Sign Out", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn  = new ButtonType("Cancel",   ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(signOutBtn, cancelBtn);

        ((Button) alert.getDialogPane().lookupButton(signOutBtn))
                .setStyle("-fx-background-color: #c62828; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        ((Button) alert.getDialogPane().lookupButton(cancelBtn))
                .setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #ccc; -fx-background-radius: 6; -fx-cursor: hand;");

        alert.showAndWait().ifPresent(result -> {
            if (result == signOutBtn) {
                clearSession();
            }
        });
    }

    //Helpers

    private static void applyFieldStyle(TextField field) {
        field.setStyle("""
            -fx-background-color: #2a2a2a;
            -fx-text-fill: white;
            -fx-prompt-text-fill: #555;
            -fx-border-color: #444;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 7 10;
            -fx-font-size: 13px;
            """);
        field.setMaxWidth(Double.MAX_VALUE);
    }

    private static void showStatus(Label label, String msg, boolean neutral) {
        label.setText(msg);
        label.setStyle("-fx-font-size: 11px; -fx-wrap-text: true; -fx-text-fill: " +
                (neutral ? "#aaa" : "#e57373") + ";");
        label.setVisible(true);
        label.setManaged(true);
    }
}