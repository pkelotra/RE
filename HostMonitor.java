public class HostMonitor extends Thread {
    private final Server serverToMonitor;
    private final Server destinationServer;
    private final int loadThresholdPercent;

    public HostMonitor(Server serverToMonitor, Server destinationServer, int loadThresholdPercent) {
        this.serverToMonitor = serverToMonitor;
        this.destinationServer = destinationServer;
        this.loadThresholdPercent = loadThresholdPercent;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            serverToMonitor.printStatus();
            destinationServer.printStatus();

            int currentLoad = serverToMonitor.getCurrentLoadMb();
            int capacity = serverToMonitor.getCapacityMb();
            double loadPercent = ((double) currentLoad / capacity) * 100;

            if (loadPercent > loadThresholdPercent) {
                System.out.printf("\nALERT: Server %s load is at %.1f%% (Threshold: %d%%). Triggering migration.\n",
                        serverToMonitor.getName(), loadPercent, loadThresholdPercent);

                // Find a VM to migrate that is NOT already migrating
                VirtualMachine vmToMigrate = serverToMonitor.getVms().stream()
                        .filter(vm -> vm.getState() == VirtualMachine.State.RUNNING)
                        .findFirst()
                        .orElse(null);

                if (vmToMigrate != null) {
                    MigrationManager.migrate(vmToMigrate, serverToMonitor, destinationServer);
                    // Wait after triggering to prevent re-triggering immediately
                    try {
                        Thread.sleep(20000); 
                    } catch (InterruptedException e) { break; }
                } else {
                    System.out.println("ALERT: Host is overloaded, but no available VMs to migrate.");
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) { break; }
        }
    }
}