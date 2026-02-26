package net.multyfora;

import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static util.RunCommand.runCustomCommand;

public class LocalWallpapers {


    public static void listAll(){

        getAll().forEach(Wallpaper::printInfo);

    }


    public static List<Wallpaper> getAll(){
        List<String> wallpaperIDs = new java.util.ArrayList<>(runCustomCommand("ls -1 ~/.local/share/Steam/steamapps/workshop/content/431960").lines().toList());
        List<Wallpaper> wallpapers = new ArrayList<>();
        for (int i = 0; i < wallpaperIDs.size(); i++) {
            String wallpaperID = wallpaperIDs.get(i);
            Path jsonFilePath = Path.of(System.getProperty("user.home"), ".local", "share", "Steam", "steamapps", "workshop", "content", "431960", wallpaperID, "project.json");
            try {
                String title = new LocalWallpapers().getTitleFromProjectJson(jsonFilePath);
                String description = new LocalWallpapers().getDescriptionFromProjectJson(jsonFilePath);
                List<String> tags = new LocalWallpapers().getTagsFromProjectJson(jsonFilePath);
                String imagePath = new LocalWallpapers().getPreviewNameFromProjectJson(jsonFilePath);
                ImageIcon preview = new ImageIcon(Files.readAllBytes(Path.of(System.getProperty("user.home"), ".local", "share", "Steam", "steamapps", "workshop", "content", "431960", wallpaperID, imagePath)));

                Wallpaper wallpaper = new Wallpaper(title, wallpaperID, description, preview, tags.toArray(new String[0]));
                wallpapers.add(wallpaper);
            } catch (JSONException e){
                System.err.println(e);
            }

            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return wallpapers;
    }

    public String getTitleFromProjectJson(Path jsonFilePath) throws Exception {
        String content = Files.readString(jsonFilePath);
        JSONObject json = new JSONObject(content);
        return json.getString("title");
    }
    public String getDescriptionFromProjectJson(Path jsonFilePath) throws Exception {
        String content = Files.readString(jsonFilePath);
        JSONObject json = new JSONObject(content);
        return json.getString("description");
    }
    public List<String> getTagsFromProjectJson(Path jsonFilePath) throws Exception {
        String content = Files.readString(jsonFilePath);
        JSONObject json = new JSONObject(content);
        return json.getJSONArray("tags").toList().stream().map(Object::toString).toList();
    }
    public String getPreviewNameFromProjectJson(Path jsonFilePath) throws Exception {
        String content = Files.readString(jsonFilePath);
        JSONObject json = new JSONObject(content);
        return json.getString("preview");
    }


}
