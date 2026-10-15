import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import java.io.PrintWriter;

public class G09TestLauncher {
    public static void main(String[] classes) {
        var request = LauncherDiscoveryRequestBuilder.request();
        for (String name : classes) request.selectors(DiscoverySelectors.selectClass(name));
        var listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request.build(), listener);
        var writer = new PrintWriter(System.out, true);
        listener.getSummary().printTo(writer);
        listener.getSummary().printFailuresTo(writer);
        if (listener.getSummary().getTestsFoundCount() == 0 || listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
