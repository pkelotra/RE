import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Server {
    private final String name;
    private final int capacityMb;
    private final List<VirtualMachine> vms = new CopyOnWriteArrayList<>();
    private final AtomicBoolean isMigrationInProgress = new AtomicBoolean(false);

    public Server(String name, int capacityMb) {
        this.name = name;
        this.capacityMb = capacityMb;
    }

    public boolean isMigrationInProgress() {
        return isMigrationInProgress.get();
    }

    public void setMigrationInProgress(boolean status) {
        this.isMigrationInProgress.set(status);
        if (status) {
            System.out.printf("INFO: Server %s is now locked for migration.\n", name);
        } else {
            System.out.printf("INFO: Server %s is now unlocked for migration.\n", name);
        }
    }

    public synchronized boolean addNewVm(VirtualMachine vm) {
        if (getCurrentLoadMb() + vm.getMemorySizeMb() <= capacityMb) {
            vms.add(vm);
            vm.start();
            System.out.printf("INFO: %s started on Server %s.\n", vm, name);
            return true;
        }
        return false;
    }

    public synchronized void addDormantVm(VirtualMachine vm) {
        vms.add(vm);
    }

    public synchronized void removeVm(VirtualMachine vm) {
        vms.remove(vm);
        vm.stop();
    }

    public synchronized int getCurrentLoadMb() {
        return vms.stream().mapToInt(VirtualMachine::getMemorySizeMb).sum();
    }
    
    public int getCapacityMb() { return capacityMb; }
    public String getName() { return name; }
    public List<VirtualMachine> getVms() { return vms; }

    public void printStatus() {
        String vmList = vms.stream().map(VirtualMachine::toString).collect(Collectors.joining(", "));
        // FIX: Uncommented this line to provide status updates
        //System.out.printf("STATUS: Server %s | Load: %d/%d MB | In Migration: %s | VMs: [%s]\n",
          //      name, getCurrentLoadMb(), capacityMb, isMigrationInProgress(), vmList);
    }
}