package net.multyfora.exampleStuff;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
public class HelloFX extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/net/multyfora/wallpaper_grid.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);
        stage.setTitle("Let Me Animate");
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("/net/multyfora/style/scrollpane.css").toExternalForm());
        scene.getStylesheets().add(
                getClass().getResource("/net/multyfora/style/button.css").toExternalForm()
        );
        stage.show();
    }
    public static void main(String[] args){
        launch();
    }
}