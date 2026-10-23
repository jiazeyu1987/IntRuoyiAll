import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import java.io.PrintWriter;
public final class CategoryJUnitRunner {
    public static void main(String[] args) {
        var request=LauncherDiscoveryRequestBuilder.request().selectors(selectClass("cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalSourceModeTest"),selectClass("cn.iocoder.yudao.module.dcc.service.category.DccCategoryApprovalSourceProjectionTest")).build();
        var listener=new SummaryGeneratingListener();LauncherFactory.create().execute(request,listener);
        var writer=new PrintWriter(System.out,true);listener.getSummary().printTo(writer);listener.getSummary().printFailuresTo(writer);
        if(listener.getSummary().getTestsFoundCount()==0||listener.getSummary().getTestsFailedCount()>0||listener.getSummary().getTestsSkippedCount()>0)System.exit(1);
    }
}
