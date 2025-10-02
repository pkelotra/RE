public class HostMonitor extends Thread {
    private final Server serverToMonitor;
    private final Server destinationServer;
    private final int loadThresholdPercent;
    private final int linkSpeedMbps;

    public HostMonitor(Server serverToMonitor, Server destinationServer, int loadThresholdPercent, int linkSpeedMbps) {
        this.serverToMonitor = serverToMonitor;
        this.destinationServer = destinationServer;
        this.loadThresholdPercent = loadThresholdPercent;
        this.linkSpeedMbps = linkSpeedMbps;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            serverToMonitor.printStatus();
            destinationServer.printStatus();

            int currentLoad = serverToMonitor.getCurrentLoadMb();
            int capacity = serverToMonitor.getCapacityMb();
            double loadPercent = ((double) currentLoad / capacity) * 100;

            if (loadPercent > loadThresholdPercent && !serverToMonitor.isMigrationInProgress()) {
                System.out.printf("\nALERT: Server %s is overloaded and not in migration. Triggering migration.\n",
                        serverToMonitor.getName());

                VirtualMachine vmToMigrate = serverToMonitor.getVms().stream()
                        .filter(vm -> vm.getState() == VirtualMachine.State.RUNNING)
                        .findFirst()
                        .orElse(null);

                if (vmToMigrate != null) {
                    MigrationManager.migrate(vmToMigrate, serverToMonitor, destinationServer, this.linkSpeedMbps);
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) { break; }
        }
    }
}