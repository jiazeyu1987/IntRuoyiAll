import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.io.PrintWriter;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Private isolated development runner; it is not a Maven/reactor or runtime acceptance gate. */
public class G07DevelopmentTestRunner {
    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(args[0]))
                .filters((org.junit.platform.launcher.PostDiscoveryFilter) descriptor -> {
                    var source = descriptor.getSource().orElse(null);
                    return FilterResult.includedIf(!(source instanceof MethodSource method)
                            || args.length == 1 || method.getMethodName().startsWith(args[1]));
                }).build();
        var summary = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request, summary);
        summary.getSummary().printTo(new PrintWriter(System.out));
        summary.getSummary().printFailuresTo(new PrintWriter(System.out));
        if (summary.getSummary().getTestsFoundCount() == 0) throw new IllegalStateException("NO_G07_TESTS_EXECUTED");
        if (summary.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
