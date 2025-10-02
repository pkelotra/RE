import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;

public class MigrationManager {
    private static final int LINK_SPEED_MBPS = 1000;
    private static final int MAX_PRE_COPY_ITERATIONS = 29;
    private static final double MAX_DATA_SENT_FACTOR = 3.0;
    private static final int MIN_DIRTY_PAGES_TO_CONTINUE = 50;

    public static void migrate(VirtualMachine sourceVm, Server source, Server destination) {
        new Thread(() -> {
            Instant startTime = Instant.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                                                        .withZone(ZoneId.systemDefault());

            System.out.printf("\n---> MIGRATION: Starting for %s at %s\n", sourceVm, formatter.format(startTime));
            
            sourceVm.setState(VirtualMachine.State.MIGRATING);

            // 1. Reservation: Create a new dormant VM on the destination
            VirtualMachine dormantVm = new VirtualMachine(sourceVm.getId(), sourceVm.getMemorySizeMb(), 0); // Dirty rate is 0 as it's not running
            destination.addDormantVm(dormantVm);
            System.out.printf("---> MIGRATION: Dormant VM created on %s.\n", destination.getName());
            
            long pageSizeBytes = 4L * 1024;
            double linkSpeedBps = (LINK_SPEED_MBPS * 1_000_000.0) / 8.0;
            double linkCapacityPps = linkSpeedBps / pageSizeBytes;
            long totalPagesSent = 0;
            int iteration = 0;
            
            // 2. Iterative Pre-copy Phase
            System.out.println("---> MIGRATION: Entering iterative pre-copy phase...");
            
            // First iteration copies all of the source VM's current memory state
            int[] initialMemory = sourceVm.getMemoryContents();
            System.arraycopy(initialMemory, 0, dormantVm.getMemoryContents(), 0, initialMemory.length);
            totalPagesSent += sourceVm.getTotalPages();
            double timeForFirstRound = sourceVm.getTotalPages() / linkCapacityPps;
            try {
                Thread.sleep((long) (timeForFirstRound * 1000));
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }


            while (true) {
                iteration++;
                BitSet pagesToTransfer = sourceVm.getAndClearDirtyPages();
                int numPagesToTransfer = pagesToTransfer.cardinality();

                double timeThisRoundS = numPagesToTransfer / linkCapacityPps;
                System.out.printf("---> MIGRATION: Iteration %d: %d dirty pages found. Transfer will take %.3f s.\n",
                        iteration, numPagesToTransfer, timeThisRoundS);
                
                try {
                    Thread.sleep((long) (timeThisRoundS * 1000));
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                
                // Simulate the (value, index) transfer
                for (int i = pagesToTransfer.nextSetBit(0); i >= 0; i = pagesToTransfer.nextSetBit(i+1)) {
                    dormantVm.getMemoryContents()[i] = sourceVm.getMemoryContents()[i];
                }
                totalPagesSent += numPagesToTransfer;
                
                // Check stop conditions against the *next* set of dirty pages
                int pagesDirtiedDuringRound = sourceVm.getMemoryContents().length; // A placeholder to enter loop
                synchronized (sourceVm.getAndClearDirtyPages()) {
                    pagesDirtiedDuringRound = sourceVm.getAndClearDirtyPages().cardinality();
                }

                if (pagesDirtiedDuringRound < MIN_DIRTY_PAGES_TO_CONTINUE) {
                    System.out.println("---> MIGRATION: Stop Condition Met: Dirty pages below threshold.");
                    break;
                }
                if (iteration >= MAX_PRE_COPY_ITERATIONS) {
                     System.out.println("---> MIGRATION: Stop Condition Met: Max iterations reached.");
                     break;
                }
                if (((double)totalPagesSent / sourceVm.getTotalPages()) > MAX_DATA_SENT_FACTOR) {
                    System.out.println("---> MIGRATION: Stop Condition Met: Data sent exceeds 3x VM RAM.");
                    break;
                }
            }
            
            // 3. Stop-and-Copy Phase
            System.out.printf("---> MIGRATION: Entering stop-and-copy phase. Pausing %s.\n", sourceVm);
            sourceVm.setState(VirtualMachine.State.PAUSED);
            BitSet finalPages = sourceVm.getAndClearDirtyPages();
            for (int i = finalPages.nextSetBit(0); i >= 0; i = finalPages.nextSetBit(i+1)) {
                dormantVm.getMemoryContents()[i] = sourceVm.getMemoryContents()[i];
            }
            
            // 4. Dissolve and Activation
            System.out.printf("---> MIGRATION: Dissolving %s on %s.\n", sourceVm, source.getName());
            source.removeVm(sourceVm);
            System.out.printf("---> MIGRATION: Activating %s on %s.\n", dormantVm, destination.getName());
            destination.removeVm(dormantVm); // Remove dormant placeholder
            destination.addNewVm(dormantVm);  // Re-add as a new, active VM
            
            Instant endTime = Instant.now();
            Duration migrationDuration = Duration.between(startTime, endTime);
            System.out.printf("---> MIGRATION: Completed for %s at %s\n", dormantVm, formatter.format(endTime));
            System.out.printf("---> MIGRATION: Total time taken: %d seconds.\n\n", migrationDuration.toSeconds());

        }).start();
    }
}