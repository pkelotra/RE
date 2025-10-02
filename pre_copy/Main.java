public class Main {
    public static void main(String[] args) {
        // --- Default Parameters ---
        int vmSize = 2048;          // in MB
        int dirtyRate = 15000;      // in pages per second
        int linkSpeed = 1000;       // in Mbps

        // --- Simple Command-Line Argument Parsing ---
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--vm-size":
                    if (i + 1 < args.length) vmSize = Integer.parseInt(args[++i]);
                    break;
                case "--dirty-rate":
                    if (i + 1 < args.length) dirtyRate = Integer.parseInt(args[++i]);
                    break;
                case "--link-speed":
                    if (i + 1 < args.length) linkSpeed = Integer.parseInt(args[++i]);
                    break;
                default:
                    System.err.println("Unknown option: " + args[i]);
                    return;
            }
        }

        System.out.println("--- Starting Simulation with Parameters ---");
        System.out.printf("VM to be Migrated -> Size: %d MB | Page Dirty Rate: %d pps\n", vmSize, dirtyRate);
        System.out.printf("Migration Network -> Transfer Rate: %d Mbps\n", linkSpeed);
        System.out.println("-----------------------------------------\n");

        // --- Setup Environment ---
        Server sourceHost = new Server("Host-A", 8192);
        Server destinationHost = new Server("Host-B", 8192);

        // --- Create VMs ---
        // FIX: Add the target VM (VM-1) FIRST so it gets selected for migration.
        VirtualMachine targetVm = new VirtualMachine(1, vmSize, dirtyRate);
        sourceHost.addNewVm(targetVm);

        // Add the background VM second.
        VirtualMachine backgroundVm = new VirtualMachine(2, 4096, 5000); // Using a fixed low dirty rate for this one
        sourceHost.addNewVm(backgroundVm);

        // --- Start Monitoring ---
        HostMonitor monitor = new HostMonitor(sourceHost, destinationHost, 80, linkSpeed);
        monitor.start();

        // After a delay, add a final VM to trigger the overload condition
        try {
            Thread.sleep(10000);
            System.out.println("\nACTION: Adding a new VM to Host-A to trigger overload...\n");
            VirtualMachine triggerVm = new VirtualMachine(3, 2048, 8000);
            sourceHost.addNewVm(triggerVm);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}