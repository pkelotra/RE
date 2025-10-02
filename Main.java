public class Main {
    public static void main(String[] args) {
        Server sourceHost = new Server("Host-A", 8192); // 8 GB RAM
        Server destinationHost = new Server("Host-B", 8192); // 8 GB RAM

        VirtualMachine vm1 = new VirtualMachine(1, 2048, 15000);
        sourceHost.addNewVm(vm1);
        
        VirtualMachine vm2 = new VirtualMachine(2, 4096, 45000);
        sourceHost.addNewVm(vm2);
        
        VirtualMachine vm3 = new VirtualMachine(3, 1024, 5000);
        sourceHost.addNewVm(vm3);

        HostMonitor monitor = new HostMonitor(sourceHost, destinationHost, 80);
        monitor.start();

        try {
            Thread.sleep(10000);
            System.out.println("\nACTION: Adding a new VM to Host-A, expecting to trigger overload...\n");
            VirtualMachine vm4 = new VirtualMachine(4, 1024, 8000);
            sourceHost.addNewVm(vm4);
        } catch (InterruptedException e) {
            e.printStackTrace(
    }
}