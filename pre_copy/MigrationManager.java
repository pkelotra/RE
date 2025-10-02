import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;

public class MigrationManager {
    private static final int MAX_PRE_COPY_ITERATIONS = 29;
    private static final double MAX_DATA_SENT_FACTOR = 3.0;
    private static final int MIN_DIRTY_PAGES_TO_CONTINUE = 50;

    public static void migrate(VirtualMachine sourceVm, Server source, Server destination, int linkSpeedMbps) {
        new Thread(() -> {
            source.setMigrationInProgress(true);
            try {
                Instant startTime = Instant.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                        .withZone(ZoneId.of("Asia/Kolkata"));

                System.out.printf("\n---> MIGRATION: Starting for %s at %s\n", sourceVm, formatter.format(startTime));
                sourceVm.setState(VirtualMachine.State.MIGRATING);

                VirtualMachine dormantVm = new VirtualMachine(sourceVm.getId(), sourceVm.getMemorySizeMb(), 0);
                destination.addDormantVm(dormantVm);
                System.out.printf("---> MIGRATION: Dormant VM created on %s.\n", destination.getName());

                long pageSizeBytes = 4L * 1024;
                double linkSpeedBps = (linkSpeedMbps * 1_000_000.0) / 8.0;
                double linkCapacityPps = linkSpeedBps / pageSizeBytes;
                long totalPagesSent = 0;
                int iteration = 0;

                System.out.println("---> MIGRATION: Entering iterative pre-copy phase...");

                // --- ROUND 1: Send all memory and pages dirtied before migration started ---
                iteration++;
                BitSet pagesToTransfer = sourceVm.getAndClearDirtyPages();
                pagesToTransfer.set(0, sourceVm.getTotalPages());
                
                int numPagesToTransfer = pagesToTransfer.cardinality();
                double timeThisRoundS = numPagesToTransfer / linkCapacityPps;

                System.out.printf("---> MIGRATION: Iteration %d (Full Copy): Transferring %d pages (will take %.3f s).\n",
                        iteration, numPagesToTransfer, timeThisRoundS);
                
                try {
                    Thread.sleep((long) (timeThisRoundS * 1000));
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                
                for (int i = pagesToTransfer.nextSetBit(0); i >= 0; i = pagesToTransfer.nextSetBit(i + 1)) {
                    dormantVm.getMemoryContents()[i] = sourceVm.getMemoryContents()[i];
                }
                totalPagesSent += numPagesToTransfer;


                // --- SUBSEQUENT ROUNDS ---
                while (true) {
                    iteration++;
                    pagesToTransfer = sourceVm.getAndClearDirtyPages();
                    numPagesToTransfer = pagesToTransfer.cardinality();

                    System.out.printf("---> MIGRATION: Debug: Iteration %d starting with %d dirty pages to transfer.\n",
                            iteration, numPagesToTransfer);

                    if (numPagesToTransfer < MIN_DIRTY_PAGES_TO_CONTINUE) {
                        System.out.println("---> MIGRATION: Stop Condition Met: Dirty pages below threshold.");
                        break;
                    }
                    if (iteration > MAX_PRE_COPY_ITERATIONS) {
                        System.out.println("---> MIGRATION: Stop Condition Met: Max iterations reached.");
                        break;
                    }
                    if (((double) totalPagesSent / sourceVm.getTotalPages()) > MAX_DATA_SENT_FACTOR) {
                        System.out.println("---> MIGRATION: Stop Condition Met: Data sent exceeds 3x VM RAM.");
                        break;
                    }

                    timeThisRoundS = numPagesToTransfer / linkCapacityPps;
                    System.out.printf("---> MIGRATION: Iteration %d: Transferring %d pages (will take %.3f s).\n",
                            iteration, numPagesToTransfer, timeThisRoundS);
                    
                    try {
                        Thread.sleep((long) (timeThisRoundS * 1000));
                    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

                    for (int i = pagesToTransfer.nextSetBit(0); i >= 0; i = pagesToTransfer.nextSetBit(i + 1)) {
                        dormantVm.getMemoryContents()[i] = sourceVm.getMemoryContents()[i];
                    }
                    totalPagesSent += numPagesToTransfer;
                }

                System.out.printf("---> MIGRATION: Entering stop-and-copy phase. Pausing %s.\n", sourceVm);
                sourceVm.setState(VirtualMachine.State.PAUSED);
                for (int i = pagesToTransfer.nextSetBit(0); i >= 0; i = pagesToTransfer.nextSetBit(i + 1)) {
                    dormantVm.getMemoryContents()[i] = sourceVm.getMemoryContents()[i];
                }

                System.out.printf("---> MIGRATION: Dissolving %s on %s.\n", sourceVm, source.getName());
                source.removeVm(sourceVm);
                System.out.printf("---> MIGRATION: Activating %s on %s.\n", dormantVm, destination.getName());
                destination.removeVm(dormantVm);
                destination.addNewVm(dormantVm);

                Instant endTime = Instant.now();
                Duration migrationDuration = Duration.between(startTime, endTime);
                System.out.printf("---> MIGRATION: Completed for %s at %s\n", dormantVm, formatter.format(endTime));
                System.out.printf("---> MIGRATION: Total time taken: %d seconds.\n\n", migrationDuration.toSeconds());
            } finally {
                source.setMigrationInProgress(false);
            }
        }).start();
    }
}