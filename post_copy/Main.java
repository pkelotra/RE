// Main.java
// Main entry point for Post-Copy Live Migration Simulation

import java.util.Scanner;

/**
 * Main class to run the Post-Copy Live Migration simulation with user interaction.
 * Now accepts VM size in MB instead of pages.
 */
public class Main {
    
    // Helper method to repeat a string (Java 8 compatible)
    private static String repeat(String str, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(str);
        }
        return result.toString();
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.println(repeat("=", 60));
            System.out.println("    POST-COPY LIVE VM MIGRATION SIMULATOR");
            System.out.println(repeat("=", 60));
            System.out.println();
            
            // Get VM size in MB from user
            int vmSizeMB = getVMSizeInMB(scanner, args);
            
            // Convert MB to pages (4KB per page = 4096 bytes)
            // Use long to avoid integer overflow, then cast to int
            long vmSizePagesLong = ((long)vmSizeMB * 1024 * 1024) / 4096;
            int vmSizePages = (int)vmSizePagesLong;
            
            // Get free page ratio from user  
            double freePageRatio = getFreePageRatio(scanner, args);
            
            System.out.println("\n" + repeat("=", 50));
            System.out.println("    SIMULATION CONFIGURATION");
            System.out.println(repeat("=", 50));
            System.out.println("VM Size: " + vmSizeMB + " MB (" + vmSizePages + " pages)");
            System.out.println("Page Size: 4 KB (Standard OS page size)");
            System.out.println("Free Page Ratio: " + String.format("%.1f", freePageRatio * 100) + "%");
            System.out.println("Expected DSB Savings: ~" + (int)(vmSizePages * freePageRatio) + " pages");
            System.out.println();
            
            System.out.print("Press Enter to start the migration simulation...");
            scanner.nextLine();
            System.out.println();
            
            // Start the simulation
            long simulationStart = System.currentTimeMillis();
            PostCopyMigration migration = new PostCopyMigration(vmSizePages, freePageRatio);
            migration.start();
            long simulationEnd = System.currentTimeMillis();
            
            System.out.println("\n" + repeat("=", 60));
            System.out.println("SIMULATION COMPLETED IN " + (simulationEnd - simulationStart) + " ms");
            System.out.println(repeat("=", 60));
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("\nMigration simulation interrupted.");
        } catch (Exception e) {
            System.err.println("\nError during simulation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    private static int getVMSizeInMB(Scanner scanner, String[] args) {
        // Check if VM size is provided as command line argument
        if (args.length > 0) {
            try {
                int vmSizeMB = Integer.parseInt(args[0]);
                if (vmSizeMB > 0) {
                    return vmSizeMB;
                }
            } catch (NumberFormatException e) {
                // Fall through to interactive input
            }
        }
        
        // Interactive input
        System.out.print("Enter VM size in MB (default: 256): ");
        String input = scanner.nextLine().trim();
        
        if (input.isEmpty()) {
            return 256; // default 256 MB
        }
        
        try {
            int vmSizeMB = Integer.parseInt(input);
            if (vmSizeMB <= 0) {
                System.out.println("VM size must be positive. Using default: 256 MB");
                return 256;
            }
            return vmSizeMB;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Using default: 256 MB");
            return 256;
        }
    }
    
    private static double getFreePageRatio(Scanner scanner, String[] args) {
        // Check if free page ratio is provided as command line argument
        if (args.length > 1) {
            try {
                double ratio = Double.parseDouble(args[1]);
                if (ratio >= 0 && ratio <= 1) {
                    return ratio;
                }
            } catch (NumberFormatException e) {
                // Fall through to interactive input
            }
        }
        
        // Interactive input
        System.out.print("Enter free page ratio (0.0-1.0, default: 0.20): ");
        String input = scanner.nextLine().trim();
        
        if (input.isEmpty()) {
            return 0.20; // default
        }
        
        try {
            double ratio = Double.parseDouble(input);
            if (ratio < 0 || ratio > 1) {
                System.out.println("Ratio must be between 0.0 and 1.0. Using default: 0.20");
                return 0.20;
            }
            return ratio;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Using default: 0.20");
            return 0.20;
        }
    }
}