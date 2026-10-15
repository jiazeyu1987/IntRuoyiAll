import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
public final class PrivateJUnitRunner {
    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request();
        for (String name : args) {
            int hash=name.indexOf('#');
            if(hash<0) request.selectors(selectClass(name));
            else request.selectors(selectMethod(name.substring(0,hash),name.substring(hash+1)));
        }
        var listener = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request.build());
        var summary = listener.getSummary();
        summary.printTo(new java.io.PrintWriter(System.out));
        summary.printFailuresTo(new java.io.PrintWriter(System.out));
        if (summary.getTestsFailedCount() > 0 || summary.getTestsSucceededCount() == 0) System.exit(1);
    }
}
