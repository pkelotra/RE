// CompletePostCopySimulation.java

/**
 * Main class to run the Post-Copy Live Migration simulation.
 */
public class CompletePostCopySimulation {
    public static void main(String[] args) {
        try {
            int vmSizeMB = 256; // default VM size in MB
            double freePageRatio = 0.20; // default 20% free pages
            
            // Check if VM size is provided as command line argument
            if (args.length > 0) {
                try {
                    vmSizeMB = Integer.parseInt(args[0]);
                    if (vmSizeMB <= 0) {
                        System.err.println("VM size must be positive. Using default: 256 MB");
                        vmSizeMB = 256;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid VM size. Using default: 256 MB");
                }
            }
            
            // Optional: Accept free page ratio as second argument
            if (args.length > 1) {
                try {
                    freePageRatio = Double.parseDouble(args[1]);
                    if (freePageRatio < 0 || freePageRatio > 1) {
                        System.err.println("Free page ratio must be between 0 and 1. Using default: 0.20");
                        freePageRatio = 0.20;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid free page ratio. Using default: 0.20");
                }
            }
            
            // Convert MB to pages (4KB per page)
            long vmSizePagesLong = ((long)vmSizeMB * 1024 * 1024) / 4096;
            int vmSizePages = (int)vmSizePagesLong;
            
            System.out.println("Starting Post-Copy Migration Simulation...");
            System.out.println("VM Size: " + vmSizeMB + " MB (" + vmSizePages + " pages)");
            System.out.println("Free Page Ratio: " + String.format("%.1f", freePageRatio * 100) + "%");
            System.out.println();
            
            new PostCopyMigration(vmSizePages, freePageRatio).start(); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Migration interrupted.");
        }
    }
}