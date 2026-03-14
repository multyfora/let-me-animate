package net.multyfora.util;

import net.multyfora.Property;
import net.multyfora.WallpaperSettings;

import java.util.Map;
import java.util.Scanner;

public class RunCommand {

    public static void runWallpaperCommand(String wallpaperId, Map<String, Property> properties) {
        try {
            StringBuilder cmd = new StringBuilder("exec linux-wallpaperengine ");

            //  Global settings flags (from Settings page)
            String globalFlags = WallpaperSettings.buildFlags();
            if (!globalFlags.isBlank()) {
                cmd.append(globalFlags).append(" ");
            }

            //  Per-wallpaper property overrides
            for (Map.Entry<String, Property> entry : properties.entrySet()) {
                String key = entry.getKey();
                Property prop = entry.getValue();
                String value = switch (prop.getType()) {
                    case SLIDER    -> String.valueOf(prop.getValue());
                    case BOOLEAN   -> prop.getBoolValue() ? "1" : "0";
                    case COMBOLIST -> prop.getComboValue();
                    case COLOR     -> String.format("\"%.5f %.5f %.5f\"",
                            prop.getR(), prop.getG(), prop.getB());
                };
                cmd.append("--set-property ").append(key).append("=").append(value).append(" ");
            }

            cmd.append(wallpaperId);

            System.out.println("Running command: " + cmd);

            ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd.toString());
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.environment().put("__NV_PRIME_RENDER_OFFLOAD", "1");
            pb.environment().put("__GLX_VENDOR_LIBRARY_NAME", "nvidia");
            pb.environment().put("__VK_LAYER_NV_optimus", "NVIDIA_only");
            pb.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runWallpaperCommand(String wallpaperId) {
        runWallpaperCommand(wallpaperId, new java.util.LinkedHashMap<>());
    }

    public static String listProperties(String wallpaperId) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "bash", "-c", "linux-wallpaperengine --list-properties " + wallpaperId
            );
            pb.redirectError(ProcessBuilder.Redirect.to(new java.io.File("/dev/null")));
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            return output;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String runCustomCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            Process process = pb.start();
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    output.append(scanner.nextLine()).append("\n");
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    public static void stopWallpaper() {
        try {
            new ProcessBuilder("bash", "-c", "pkill -f linux-wallpaperengine")
                    .inheritIO()
                    .start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}