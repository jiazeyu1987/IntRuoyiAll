import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import java.io.PrintWriter;

/** Test-only launcher: compiles in the G03 task directory, never in shared target. */
public final class PrivateJUnitRunner {
    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass("cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrixResolverTest"))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false").build();
        var results = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request, results);
        var writer = new PrintWriter(System.out, true);
        results.getSummary().printTo(writer);
        results.getSummary().printFailuresTo(writer);
        if (results.getSummary().getTestsFoundCount() == 0 || results.getSummary().getTestsFailedCount() > 0
                || results.getSummary().getTestsSkippedCount() > 0) System.exit(1);
    }
}
