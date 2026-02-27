package net.multyfora.util;

import java.util.Scanner;

public class RunCommand {

    public static void runWallpaperCommand(String wallpaperId){
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "bash", "-c", "exec linux-wallpaperengine --screen-root HDMI-A-1 " + wallpaperId
            );

            // Inherit normal output, but DISCARD errors to stop the GLEW message
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.to(new java.io.File("/dev/null")));

            pb.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            ProcessBuilder pb = new ProcessBuilder(
                    "bash", "-c", command
            );
            Process process = pb.start();

            // Read the output of the command
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    output.append(scanner.nextLine()).append("\n");
                }
            }

            // Wait for the process to finish
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }
}
