import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Server {
    private final String name;
    private final int capacityMb;
    private final List<VirtualMachine> vms = new CopyOnWriteArrayList<>();

    public Server(String name, int capacityMb) {
        this.name = name;
        this.capacityMb = capacityMb;
    }

    public synchronized boolean addNewVm(VirtualMachine vm) {
        if (getCurrentLoadMb() + vm.getMemorySizeMb() <= capacityMb) {
            vms.add(vm);
            vm.start(); // Start the thread for a brand new VM
            System.out.printf("INFO: %s started on Server %s.\n", vm, name);
            return true;
        }
        return false;
    }

    // Adds a VM but does not start its workload thread (for the dormant VM)
    public synchronized void addDormantVm(VirtualMachine vm) {
        vms.add(vm);
    }

    public synchronized void removeVm(VirtualMachine vm) {
        vms.remove(vm);
        vm.stop(); // Ensure the old VM's thread is stopped
    }

    public synchronized int getCurrentLoadMb() {
        return vms.stream().mapToInt(VirtualMachine::getMemorySizeMb).sum();
    }
    
    public int getCapacityMb() { return capacityMb; }
    public String getName() { return name; }
    public List<VirtualMachine> getVms() { return vms; }

    public void printStatus() {
        String vmList = vms.stream().map(VirtualMachine::toString).collect(Collectors.joining(", "));
        System.out.printf("STATUS: Server %s | Load: %d/%d MB | VMs: [%s]\n",
                name, getCurrentLoadMb(), capacityMb, vmList);
    }
}