import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.nio.file.*;
import java.util.*;

/** Redefine explicitly named, already loaded classes in a local development JVM. */
public class HotSwap {
    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length % 2 != 1)
            throw new IllegalArgumentException("Usage: HotSwap.java port className classFile [className classFile ...]");
        AttachingConnector connector = Bootstrap.virtualMachineManager().attachingConnectors().stream()
            .filter(c -> c.name().equals("com.sun.jdi.SocketAttach")).findFirst().orElseThrow();
        var options = connector.defaultArguments();
        options.get("hostname").setValue("127.0.0.1");
        options.get("port").setValue(args[0]);
        options.get("timeout").setValue("10000");
        VirtualMachine vm = connector.attach(options);
        try {
            if (!vm.canRedefineClasses()) throw new IllegalStateException("JVM does not support class redefinition");
            Map<ReferenceType, byte[]> changes = new LinkedHashMap<>();
            for (int i = 1; i < args.length; i += 2) {
                List<ReferenceType> matches = vm.classesByName(args[i]);
                if (matches.size() != 1) throw new IllegalStateException(args[i] + ": expected one loaded class, found " + matches.size());
                changes.put(matches.getFirst(), Files.readAllBytes(Path.of(args[i + 1])));
            }
            vm.redefineClasses(changes);
            changes.keySet().forEach(type -> System.out.println("REDEFINED " + type.name()));
        } finally {
            vm.dispose();
        }
    }
}
