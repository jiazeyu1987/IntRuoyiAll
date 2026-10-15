import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.io.PrintWriter;

public final class G04TestLauncher {
    public static void main(String[] classes) {
        var request = LauncherDiscoveryRequestBuilder.request();
        for (var name : classes) request.selectors(DiscoverySelectors.selectClass(name));
        var listener = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request.build());
        listener.getSummary().printTo(new PrintWriter(System.out, true));
        listener.getSummary().printFailuresTo(new PrintWriter(System.out, true));
        if (listener.getSummary().getTestsFoundCount() == 0 || listener.getSummary().getTotalFailureCount() != 0)
            System.exit(1);
    }
}
