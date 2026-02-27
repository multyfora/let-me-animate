package net.multyfora;

import java.util.Scanner;

import static net.multyfora.util.RunCommand.runCustomCommand;

public class Main {
    public static void main(String[] args) {


//        LocalWallpapers.listAll();
        startApp();
    }


    private static void startApp() {

        Scanner scanner = new Scanner(System.in);
        LocalWallpapers.listNames();
        while (true) {
            System.out.println("enter a command or wallpaper id: ");
            String input = scanner.nextLine();
            if (input.equals("q")) {
                runCustomCommand("pkill -f linux-wallpaperengine");
            } else if (input.equals("list")) {
                LocalWallpapers.listNames();
            } else {
                LocalWallpapers.runById(input);
            }
        }

    }


    private static void killCommand() {
        System.out.printf("killing wallpaper engine...");
        try {
            new ProcessBuilder(
                    "bash", "-c", "pkill -f linux-wallpaperengine"
            ).inheritIO().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}