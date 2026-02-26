package util;

import java.util.Scanner;

public class RunCommand {



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
