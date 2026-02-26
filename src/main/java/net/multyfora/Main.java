package net.multyfora;

import java.util.Scanner;

import static util.RunCommand.runCustomCommand;

public class Main {
    public static void main(String[] args){


        LocalWallpapers.listAll();
//        startApp();
    }


    private static void startApp(){
        Scanner scanner = new Scanner(System.in);
        while(true){

            System.out.println("enter the wallpaper id:");
            String wallpaperId = scanner.nextLine();

            if(runCustomCommand("pgrep -f linux-wallpaperengine").isEmpty()){
                runCommand(wallpaperId);
            }
            else if(wallpaperId.equals("q")){
                killCommand();
            }
            else{
                System.out.printf("wallpaper engine is already running, killing it...");
            }
        }
    }

    private static void runCommand(String wallpaperId) {
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
    private static void killCommand() {
        System.out.printf("killing wallpaper engine...");
        try {
            new ProcessBuilder(
                    "bash", "-c", "killall linux-wallpaperengine"
            ).inheritIO().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}