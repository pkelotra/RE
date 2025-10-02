import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class VirtualMachine {
    private final int id;
    private final int memorySizeMb;
    private final int pageDirtyRatePps;
    private final int totalPages;
    private final int[] memoryContents;
    private final BitSet dirtyPageTracker;
    private final Thread workloadThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicBoolean hasStarted = new AtomicBoolean(false);

    public enum State { RUNNING, PAUSED, MIGRATING }
    private volatile State state = State.RUNNING;

    public VirtualMachine(int id, int memorySizeMb, int pageDirtyRatePps) {
        this.id = id;
        this.memorySizeMb = memorySizeMb;
        this.pageDirtyRatePps = pageDirtyRatePps;
        int pageSizeKb = 4;
        this.totalPages = (memorySizeMb * 1024) / pageSizeKb;
        this.memoryContents = new int[this.totalPages];
        this.dirtyPageTracker = new BitSet(this.totalPages);

        this.workloadThread = new Thread(() -> {
            Random random = new Random();
            while (isRunning.get()) {
                // This correctly allows the workload to run during migration
                if (state == State.RUNNING || state == State.MIGRATING) {
                    int pagesToDirty = pageDirtyRatePps / 10;
                    for (int i = 0; i < pagesToDirty; i++) {
                        int pageIndex = random.nextInt(totalPages);
                        memoryContents[pageIndex] = random.nextInt();
                        synchronized (dirtyPageTracker) {
                            dirtyPageTracker.set(pageIndex);
                        }
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void start() {
        if (hasStarted.compareAndSet(false, true)) {
            this.workloadThread.start();
        }
    }

    public void stop() {
        this.isRunning.set(false);
        this.workloadThread.interrupt();
    }

    public BitSet getAndClearDirtyPages() {
        synchronized (dirtyPageTracker) {
            BitSet dirtyPagesSnapshot = (BitSet) dirtyPageTracker.clone();
            dirtyPageTracker.clear();
            return dirtyPagesSnapshot;
        }
    }

    public int getId() { return id; }
    public int getMemorySizeMb() { return memorySizeMb; }
    public int getTotalPages() { return totalPages; }
    public int[] getMemoryContents() { return memoryContents; }
    public void setState(State state) { this.state = state; }
    public State getState() { return state; }

    @Override
    public String toString() {
        return "VM-" + id + " (" + memorySizeMb + " MB)";
    }
}